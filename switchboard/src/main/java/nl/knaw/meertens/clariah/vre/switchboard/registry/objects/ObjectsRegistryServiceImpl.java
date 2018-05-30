package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectsRegistryServiceImpl implements ObjectsRegistryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String objectTable = "/_table/object";
    private final String objectsDbUrl;
    private final String objectsDbKey;
    private final ObjectMapper mapper;

    public ObjectsRegistryServiceImpl(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        if (!hasAllObjectsDbDetails(objectsDbKey, objectsDbUrl)) {
            throw new IllegalArgumentException("Not all arguments are provided.");
        }
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
        this.mapper = mapper;
    }

    @Override
    public ObjectsRecordDTO getObjectById(Long id) {
        HttpResponse<String> response;
        String url = objectsDbUrl + objectTable + "/" + id;
        try {
            response = Unirest
                    .get(url)
                    .header("Content-Type", "application/json")
                    .header("X-DreamFactory-Api-Key", objectsDbKey)
                    .asString();
            ObjectsRecordDTO result = isBlank(response.getBody())
                    ? new ObjectsRecordDTO()
                    : mapper.readValue(response.getBody(), ObjectsRecordDTO.class);
            logger.info(String.format("Requested [%d] from registry, received object of [%s]", id, result.filepath));
            return result;
        } catch (IOException | UnirestException e) {
            return handleException(e, "Could not retrieve object record [%d] from registry", id.toString());
        }
    }

    private boolean hasAllObjectsDbDetails(String objectsDbKey, String objectsDbUrl) {
        if (isBlank(objectsDbKey)) {
            logger.warn("Key of object registry is not set");
            return false;
        }
        if (isBlank(objectsDbUrl)) {
            logger.warn("Url of object registry is not set");
            return false;
        }
        return true;
    }

}
