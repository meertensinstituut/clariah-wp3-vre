package nl.knaw.meertens.clariah.vre.switchboard.registry;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

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
        HttpRequest request = createPost(table, recordJson);
        HttpResponse<String> response = fireRequest(request);
        return handleResponse(response);
    }

    protected String delete(Map<String,String> filters) throws SQLException {
        String urlFilters = convert(filters);
        HttpRequest request = createDelete(this.table, urlFilters);
        HttpResponse<String> response = fireRequest(request);
        return handleResponse(response);
    }

    /**
     * Fire request and handle UnirestException
     */
    private HttpResponse<String> fireRequest(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = request.asString();
        } catch (UnirestException e) {
            throw new RuntimeException(format(
                    "Could not fire request [%s]",
                    request.getUrl()
            ), e);
        }
        return response;
    }

    /**
     * @return response body when status is in success range
     * @throws SQLException with sqlState and reason otherwise
     */
    private String handleResponse(HttpResponse<String> response) throws SQLException {
        if (isSuccess(response)) {
            logger.info(String.format("Posted record to [%s]", table));
            return response.getBody();
        } else {
            String reason = JsonPath.parse(response.getBody()).read("$.error.context.resource[0].message", String.class);
            String sqlState = JsonPath.parse(response.getBody()).read("$.error.context.resource[0].code", String.class);
            throw new SQLException(reason, sqlState);
        }
    }

    private String convert(Map<String, String> filters) {
        List<String> filterParts = filters
                .entrySet()
                .stream()
                .map(entry -> "(" + entry.getKey() + " = "+ entry.getValue() + ")")
                .collect(toList());
        String allFilters = Joiner
                .on(" AND ")
                .join(filterParts);
        return encodeUriComponent(allFilters);
    }

    /**
     * Source: technicaladvices.com/2012/02/20/java-encoding-similiar-to-javascript-encodeuricomponent/
     */
    private String encodeUriComponent(String filter) {
        try {
            return URLEncoder.encode(filter, "UTF-8")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(String.format(
                    "Could not create url from filter %s", filter
            ));
        }
    }

    private boolean isSuccess(HttpResponse<String> response) {
        return response.getStatus() / 100 == 2;
    }

    /**
     * Create POST request
     */
    private HttpRequest createPost(String table, String recordJson) {
        HttpRequestWithBody request;
        request = Unirest.post(objectsDbUrl + table);
        // wrap new entry in resource array:
        recordJson = format("{\"resource\" : [%s]}", recordJson);
        return addHeaders(request)
                .body(recordJson)
                .getHttpRequest();
    }

    /**
     * Create DELETE by filters request
     */
    private HttpRequest createDelete(String table, String filters) {
        HttpRequestWithBody request = Unirest
                .delete(objectsDbUrl + table + "?filter=" + filters);
        return addHeaders(request)
                .getHttpRequest();
    }

    private HttpRequestWithBody addHeaders(HttpRequestWithBody request) {
        return request
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", objectsDbKey);
    }


}
