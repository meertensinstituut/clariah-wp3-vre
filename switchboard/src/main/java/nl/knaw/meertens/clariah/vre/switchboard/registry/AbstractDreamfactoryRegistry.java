package nl.knaw.meertens.clariah.vre.switchboard.registry;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static java.lang.String.format;

public class AbstractDreamfactoryRegistry {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String objectsDbUrl;
    private final String objectsDbKey;
    private final String table;


    public AbstractDreamfactoryRegistry(String objectsDbUrl, String objectsDbKey, String table) {

        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
        this.table = table;
    }

    protected String post(String recordJson) throws SQLException {
        HttpResponse<String> response;
        try {
            RequestBodyEntity body = createPost(table, recordJson);
            response = body.asString();
        } catch (UnirestException e) {
            throw new RuntimeException(format(
                    "Could not post [%s]",
                    recordJson
            ), e);
        }
        if (isSuccess(response)) {
            logger.info(String.format("Posted record to [%s]", table));
            return response.getBody();
        } else {
            String reason = JsonPath.parse(response.getBody()).read("$.error.context.resource[0].message", String.class);
            String sqlState = JsonPath.parse(response.getBody()).read("$.error.context.resource[0].code", String.class);
            throw new SQLException(reason, sqlState);
        }
    }

    private boolean isSuccess(HttpResponse<String> response) {
        return response.getStatus() / 100 == 2;
    }

    /**
     * Create POST request
     */
    private RequestBodyEntity createPost(String tagTable, String recordJson) {
        HttpRequestWithBody request;
        request = Unirest.post(objectsDbUrl + tagTable);
        // wrap new entry in resource array:
        recordJson = format("{\"resource\" : [%s]}", recordJson);
        return request
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", objectsDbKey)
                .body(recordJson);
    }


}
