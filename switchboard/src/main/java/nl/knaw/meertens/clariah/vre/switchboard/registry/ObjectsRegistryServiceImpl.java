package nl.knaw.meertens.clariah.vre.switchboard.registry;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectsRegistryServiceImpl implements ObjectsRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(ObjectsRegistryServiceStub.class);
    private final String objectTable = "/_table/object";
    private final String objectsDbUrl;
    private final String objectsDbKey;
    private final String objectsDbToken;

    public ObjectsRegistryServiceImpl(String objectsDbUrl, String objectsDbKey, String objectsDbToken) {
        if(!hasAllObjectsDbDetails(objectsDbKey, objectsDbToken, objectsDbUrl)) {
            throw new IllegalArgumentException("Not all arguments are provided.");
        }
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
        this.objectsDbToken = objectsDbToken;
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
                    .header("X-DreamFactory-Session-Token", objectsDbToken)
                    .asString();
        } catch (UnirestException e) {
            return handleException(e, "Could not retrieve object record [%d] from objects repository", id.toString());
        }
        logger.info(String.format("Requested object with id [%d] from registry: [%s]", id, response.getBody()));
        ObjectsRecordDTO result = new ObjectsRecordDTO();
        result.id = id;
        result.filepath = JsonPath.parse(response.getBody()).read("filepath");
        return result;
    }

    private boolean hasAllObjectsDbDetails(String objectsDbKey, String objectsDbToken, String objectsDbUrl) {
        if(isBlank(objectsDbKey)) {
            logger.warn("Key of object registry is not set");
            return false;
        }
        if(isBlank(objectsDbToken)) {
            logger.warn("Token of object registry is not set");
            return false;
        }
        if(isBlank(objectsDbUrl)) {
            logger.warn("Url of object registry is not set");
            return false;
        }
        return true;
    }

}
