package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;

public class ServicesRegistryServiceImpl implements ServicesRegistryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String servicesWithMimetypeView = "_table/service_with_mimetype";
    private final String serviceDbUrl;
    private final String serviceDbKey;
    private final ObjectMapper mapper;

    public ServicesRegistryServiceImpl(String serviceDbUrl, String serviceDbKey, ObjectMapper mapper) {
        if(serviceDbUrl == null) throw new IllegalArgumentException("Url of service db cannot be null");
        this.serviceDbUrl = serviceDbUrl;
        if(serviceDbKey == null) throw new IllegalArgumentException("Key of service db cannot be null");
        this.serviceDbKey = serviceDbKey;
        this.mapper = mapper;
    }

    @Override
    public List<ServiceRecordDTO> getServicesByMimetype(String mimetype) {
        HttpResponse<String> response;
        String url = String.format("%s/%s?filter=mimetype%%20%%3D%%20%s", serviceDbUrl, servicesWithMimetypeView, mimetype);
        try {
            response = Unirest
                    .get(url)
                    .header("Content-Type", "application/json")
                    .header("X-DreamFactory-Api-Key", serviceDbKey)
                    .asString();
            if(response.getStatus() != 200) {
                throw new IllegalStateException(String.format(
                        "Services registry did not return 200 but [%d]",
                        response.getStatus()
                ));
            }
            JsonNode resource = mapper.readTree(response.getBody()).at("/resource");
            ObjectReader reader = mapper.readerFor(new TypeReference<List<ServiceRecordDTO>>() {});
            return reader.readValue(resource);
        } catch (UnirestException | IOException | IllegalStateException e) {
            return handleException(e, "Could not determine services for mimetype [%s]", mimetype);
        }
    }
}
