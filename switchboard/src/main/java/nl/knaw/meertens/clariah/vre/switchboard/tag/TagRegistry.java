package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.String.format;

public class TagRegistry {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String objectsDbUrl;
    private final String objectsDbKey;
    private final String tagTable = "/_table/tag";
    private final ObjectMapper mapper;

    public TagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
        this.mapper = mapper;
    }

    public Long create(TagDto tag) {
        try {
            String json = persistTagRecord(mapper.writeValueAsString(tag));
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tag", e);
        }
    }

    private String persistTagRecord(String recordJson) {
        HttpResponse<String> response;
        try {
            RequestBodyEntity body = createPost(recordJson);
            response = body.asString();
        } catch (UnirestException e) {
            throw new RuntimeException(format(
                    "Could not add [%s]",
                    recordJson
            ), e);
        }

        if (isSuccess(response)) {
            logger.info("Persisted record in tag registry");
            return response.getBody();
        } else {
            throw new RuntimeException(format(
                    "Could not add [%s]: [%d][%s]",
                    recordJson, response.getStatus(), response.getBody()
            ));
        }
    }

    /**
     * Create POST request
     */
    private RequestBodyEntity createPost(String recordJson) {
        HttpRequestWithBody request;
        request = Unirest.post(objectsDbUrl + tagTable);
        // wrap new entry in resource array:
        recordJson = format("{\"resource\" : [%s]}", recordJson);
        return request
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", objectsDbKey)
                .body(recordJson);
    }

    private boolean isSuccess(HttpResponse<String> response) {
        return response.getStatus() / 100 == 2;
    }

}
