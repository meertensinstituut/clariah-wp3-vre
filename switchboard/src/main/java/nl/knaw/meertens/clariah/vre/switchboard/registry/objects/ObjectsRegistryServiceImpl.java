package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectsRegistryServiceImpl implements ObjectsRegistryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String objectTable = "/_table/object";
    private final String objectsDbUrl;
    private final String objectsDbKey;

    public ObjectsRegistryServiceImpl(String objectsDbUrl, String objectsDbKey) {
        if(!hasAllObjectsDbDetails(objectsDbKey, objectsDbUrl)) {
            throw new IllegalArgumentException("Not all arguments are provided.");
        }
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
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
        } catch (UnirestException e) {
            return handleException(e, "Could not retrieve object record [%d] from registry", id.toString());
        }
        ObjectsRecordDTO result = new ObjectsRecordDTO();
        result.id = id;
        result.filepath = JsonPath.parse(response.getBody()).read("filepath");
        logger.info(String.format("Requested [%d] from registry, received object of [%s]", id, result.filepath));
        return result;
    }

    private boolean hasAllObjectsDbDetails(String objectsDbKey, String objectsDbUrl) {
        if(isBlank(objectsDbKey)) {
            logger.warn("Key of object registry is not set");
            return false;
        }
        if(isBlank(objectsDbUrl)) {
            logger.warn("Url of object registry is not set");
            return false;
        }
        return true;
    }

}
