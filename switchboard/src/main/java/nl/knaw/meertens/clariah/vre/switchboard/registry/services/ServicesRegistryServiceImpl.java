package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ServicesRegistryServiceImpl implements ServicesRegistryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String serviceDbUrl;
    private final String serviceDbKey;
    private final ObjectMapper mapper;

    public ServicesRegistryServiceImpl(String serviceDbUrl, String serviceDbKey, ObjectMapper mapper) {
        if (isBlank(serviceDbUrl)) throw new IllegalArgumentException("Url of service db must be set");
        this.serviceDbUrl = serviceDbUrl;
        if (isBlank(serviceDbKey)) throw new IllegalArgumentException("Key of service db must be set");
        this.serviceDbKey = serviceDbKey;
        this.mapper = mapper;
    }

    @Override
    public ServiceRecord getService(Long id) {
        String body = "";
        try {
            String service = "_table/service";
            String url = String.format(
                    "%s/%s/%d",
                    serviceDbUrl,
                    service,
                    id
            );
            HttpResponse<String> response = requestServiceDb(url);
            body = response.getBody();
            return mapper.readValue(body, ServiceRecord.class);
        } catch (UnirestException | IOException | IllegalStateException e) {
            throw new RuntimeException(String.format("Could not get service for id [%s]; response: [%s]", id, body), e);
        }
    }

    /**
     * Find service by name
     *
     * @return ServiceRecord service
     * @throws IllegalStateException when no service is found
     */
    @Override
    public ServiceRecord getServiceByName(String name) {
        var service = "_table/service";
        var url = String.format(
                "%s/%s/?filter=name%%20like%%20%s",
                serviceDbUrl,
                service,
                name
        );
        var services = this.getServices(url);
        if (services.isEmpty()) {
            throw new IllegalStateException(String.format("No services found for service [%s]", name));
        }
        return services.get(0);
    }

    @Override
    public List<ServiceRecord> getServicesByMimetype(String mimetype) {
        var serviceWithMimetypeView = "_table/service_with_mimetype";
        var url = String.format(
                "%s/%s?filter=mimetype%%20%%3D%%20%s",
                serviceDbUrl,
                serviceWithMimetypeView,
                mimetype
        );
        return getServices(url);
    }

    @Override
    public List<ServiceRecord> getServicesByMimetypeAndKind(String mimetype, ServiceKind kind) {
        var serviceWithMimetypeView = "_table/service_with_mimetype";
        var url = String.format(
                "%s/%s?filter=(mimetype%%20%%3D%%20%s)%%20and%%20(kind%%20like%%20%s)",
                serviceDbUrl,
                serviceWithMimetypeView,
                mimetype,
                kind.getKind()
        );
        return getServices(url);
    }

    private List<ServiceRecord> getServices(String url) {
        try {
            var response = requestServiceDb(url);
            var resource = mapper.readTree(response.getBody()).at("/resource");
            var reader = mapper.readerFor(new TypeReference<List<ServiceRecord>>() {
            });
            List<ServiceRecord> serviceRecords = reader.readValue(resource);
            logger.info(String.format(
                    "Url [%s] yielded services [%s]",
                    url,
                    serviceRecords
                            .stream()
                            .map(o -> o.getId().toString())
                            .collect(Collectors.joining(", "))
            ));
            return serviceRecords;
        } catch (UnirestException | IOException e) {
            throw new RuntimeException(String.format("Could not determine services for url [%s]", url), e);
        }
    }

    private HttpResponse<String> requestServiceDb(String url) throws UnirestException {
        HttpResponse<String> response;
        response = Unirest
                .get(url)
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", serviceDbKey)
                .asString();
        if (response.getStatus() != 200) {
            throw new IllegalStateException(String.format(
                    "Services registry response status was not 200 but [%d]",
                    response.getStatus()
            ));
        }
        return response;
    }
}
