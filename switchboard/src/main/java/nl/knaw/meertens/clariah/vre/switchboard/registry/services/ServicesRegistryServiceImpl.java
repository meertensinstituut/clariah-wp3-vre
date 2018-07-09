package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
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
    public ServiceRecordDto getService(Long id) {
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
            ServiceRecordDto serviceRecord = mapper.readValue(body, ServiceRecordDto.class);
            return serviceRecord;
        } catch (UnirestException | IOException | IllegalStateException e) {
            return handleException(e, "Could not get service for id [%s] and response [%s]", id, body);
        }
    }

    @Override
    public List<ServiceRecordDto> getServices(String mimetype) {
        try {
            String serviceWithMimetypeView = "_table/service_with_mimetype";
            String url = String.format(
                    "%s/%s?filter=mimetype%%20%%3D%%20%s",
                    serviceDbUrl,
                    serviceWithMimetypeView,
                    mimetype
            );
            HttpResponse<String> response = requestServiceDb(url);
            JsonNode resource = mapper.readTree(response.getBody()).at("/resource");
            ObjectReader reader = mapper.readerFor(new TypeReference<List<ServiceRecordDto>>() {});
            List<ServiceRecordDto> serviceRecordDtos = reader.readValue(resource);
            logger.info(String.format(
                    "Mimetype [%s] yielded services [%s]",
                    mimetype,
                    serviceRecordDtos
                            .stream()
                            .map(o -> o.id.toString())
                            .collect(Collectors.joining(", "))
            ));
            return serviceRecordDtos;
        } catch (UnirestException | IOException | IllegalStateException e) {
            return handleException(e, "Could not determine services for mimetype [%s]", mimetype);
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
