package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.IdentificationType.Identity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_TABLE;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectsRepositoryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String objectsDbUrl;
    private final String objectsDbKey;
    private final String objectTable;
    private final ParseContext jsonPath;

    ObjectsRepositoryService(String objectsDbUrl, String objectsDbKey, String objectTable) {
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
        this.objectTable = objectTable;

        Configuration conf = Configuration
                .builder()
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();
        jsonPath = JsonPath.using(conf);
    }

    /**
     * Create new object in object registry
     *
     * @return objectId
     */
    public Long create(Report report) {
        String recordJson = createJson(report);
        String persistResult = persistRecord(recordJson, null);
        return jsonPath.parse(persistResult).read("$.resource[0].id", Integer.class).longValue();
    }

    /**
     * Update object by path
     *
     * @return objectId
     */
    public Long update(Report report) {
        Long id = getObjectIdByPath(report.getPath());
        String recordJson = createJson(report);
        String persistResult = persistRecord(recordJson, id);
        return Long.valueOf(jsonPath.parse(persistResult).read("$.id", String.class));
    }

    /**
     * Update oldPath with newPath of object
     *
     * @return objectId
     */
    public Long updatePath(String oldPath, String newPath) {
        Long id = getObjectIdByPath(oldPath);

        String patchObject = getObjectUrl(id);

        String bodyWithNewPath = String.format("{ \"filepath\" : \"%s\" }", newPath);
        HttpResponse<String> patchResponse;
        try {
            patchResponse = Unirest
                    .patch(patchObject)
                    .header("Content-Type", "application/json")
                    .header("X-DreamFactory-Api-Key", OBJECTS_DB_KEY)
                    .body(bodyWithNewPath)
                    .asString();
        } catch (UnirestException e) {
            throw new IllegalArgumentException(String.format(
                    "Could not update path [%s]", newPath
            ), e);
        }

        if (!isSuccess(patchResponse)) {
            logger.error(String.format(
                    "Could not patch object [%s] with new path [%s]; response: [%s]",
                    id, newPath, patchResponse.getBody()
            ));
        }
        return id;
    }

    /**
     * Delete object by path
     *
     * @return objectId
     */
    public Long delete(String path) {
        Long id = getObjectIdByPath(path);
        String deleteObject = getObjectUrl(id);
        HttpResponse<String> response;
        try {
            response = Unirest
                    .delete(deleteObject)
                    .header("Content-Type", "application/json")
                    .header("X-DreamFactory-Api-Key", OBJECTS_DB_KEY)
                    .asString();
        } catch (UnirestException e) {
            throw new IllegalArgumentException(String.format(
                    "Could not delete object [%s]", path
            ), e);
        }
        if (!isSuccess(response)) {
            logger.error(String.format(
                    "Could not delete object [%s]; response: [%s]",
                    id, response.getBody()
            ));
        }
        return id;
    }

    private String getObjectUrl(Long id) {
        return new StringBuilder()
                .append(OBJECTS_DB_URL)
                .append(OBJECT_TABLE)
                .append("/")
                .append(id)
                .toString();
    }

    private Long getObjectIdByPath(String path) {
        String getObjectByPath;
        getObjectByPath = new StringBuilder()
                .append(OBJECTS_DB_URL)
                .append(OBJECT_TABLE)
                .append("?limit=1&order=id%20DESC&filter=filepath=")
                .append(path)
                .toString();
        try {
            logger.info(String.format(
                    "Request id by path [%s] using url [%s]",
                    path, getObjectByPath
            ));
            HttpResponse<String> getIdResponse = Unirest
                    .get(getObjectByPath)
                    .header("Content-Type", "application/json")
                    .header("X-DreamFactory-Api-Key", OBJECTS_DB_KEY)
                    .asString();
            if (getIdResponse.getStatus() != 200) {
                throw new RuntimeException(String.format(
                        "Retrieving id of path [%s]. Registry responded: [%s]",
                        path, getIdResponse.getBody()
                ));
            }

            Long id = Long.valueOf(jsonPath.parse(getIdResponse.getBody()).read("$.resource[0].id"));
            logger.info(String.format("Path [%s] returned object id [%s]", path, id));
            return id;
        } catch (UnirestException e) {
            throw new RuntimeException(String.format(
                    "Could not get object by path [%s]", path
            ), e);
        }
    }

    public String createJson(Report report) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(WRITE_DATES_AS_TIMESTAMPS);

        ObjectsRecordDTO record = fillObjectsRecordDto(report);
        try {
            return mapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format(
                    "Could not create json from report with path [%s]",
                    report.getPath()
            ), e);
        }
    }

    /**
     * Persist record. Create new entry when id == null
     */
    private String persistRecord(String recordJson, Long id) {
        if (!hasAllObjectsDbDetails()) {
            return null;
        }
        HttpResponse<String> response;
        try {
            RequestBodyEntity body = createPostOrPutRequestBasedOnId(recordJson, id);
            response = body.asString();
        } catch (UnirestException e) {
            throw new RuntimeException(String.format(
                    "Could not add new record [%s]",
                    recordJson
            ), e);
        }

        logger.info(String.format(
                "Objects registry responded with: %d - %s",
                response.getStatus(), response.getStatusText()
        ));

        if (isSuccess(response)) {
            logger.info("Persisted record in objects registry");
            return response.getBody();
        } else {
            throw new RuntimeException(String.format(
                    "Failed to add object [%s]; response was: [%s]",
                    recordJson, response.getBody()
            ));
        }
    }

    private RequestBodyEntity createPostOrPutRequestBasedOnId(String recordJson, Long id) {
        HttpRequestWithBody request;
        if (id == null) {
            request = Unirest.post(objectsDbUrl + objectTable);
            // wrap new entry in resource array:
            recordJson = String.format("{\"resource\" : [%s]}", recordJson);
        } else {
            request = Unirest.put(objectsDbUrl + objectTable + "/" + id);
        }
        logger.info(String.format(
                "Create for [%d] uri [%s] method [%s] body [%s]",
                id, request.getUrl(), request.getHttpMethod(), abbreviate(recordJson, 1000))
        );
        return request
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", objectsDbKey)
                .body(recordJson);
    }

    private boolean isSuccess(HttpResponse<String> response) {
        return response.getStatus() / 100 == 2;
    }

    private ObjectsRecordDTO fillObjectsRecordDto(Report report) {
        ObjectsRecordDTO msg = new ObjectsRecordDTO();
        msg.filepath = report.getPath();
        Identity identity = report.getFits().getIdentification().getIdentity().get(0);
        msg.fits = report.getXml();
        msg.format = identity.getFormat();
        msg.mimetype = identity.getMimetype();
        msg.timechanged = LocalDateTime.now();
        msg.timecreated = LocalDateTime.now();
        msg.user_id = report.getUser();
        msg.type = "object";
        return msg;
    }

    private boolean hasAllObjectsDbDetails() {
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
