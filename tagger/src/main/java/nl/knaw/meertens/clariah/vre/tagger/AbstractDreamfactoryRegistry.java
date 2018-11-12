package nl.knaw.meertens.clariah.vre.tagger;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AbstractDreamfactoryRegistry {

    private final String objectsDbUrl;
    private final String objectsDbKey;


    protected AbstractDreamfactoryRegistry(String objectsDbUrl, String objectsDbKey) {

        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
    }

    protected String postResource(
            String json,
            String endpoint
    ) throws SQLException {
        return post(json, endpoint, true);
    }

    public String postProcedure(
            HashMap<String, Object> params,
            String endpoint
    ) throws SQLException {
        var body = "";
        var isResource = false;
        return post(body, endpoint + "?" + createUrlQueryParams(params), isResource);
    }

    private String createUrlQueryParams(Map<String, Object> params) {
        return Joiner.on("&").join(params.entrySet().stream().map(p ->
                    encodeUriComponent(p.getKey())
                    + "="
                    + encodeUriComponent(p.getValue().toString())
            ).collect(toList()));
    }

    private String createDreamfactoryParams(HashMap<String, Object> params) {
        return "("+Joiner.on(")and(").join(params.entrySet().stream().map(p ->
                    encodeUriComponent(p.getKey())
                    + "="
                    + encodeUriComponent(p.getValue().toString())
            ).collect(toList()))+")";
    }

    private String post(
            String json,
            String endpoint,
            boolean isResource
    ) throws SQLException {
        var request = createPostRequest(endpoint, json, isResource);
        var response = fireRequest(request);
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
     * @throws SQLException with sqlState and reason
     */
    private String handleResponse(HttpResponse<String> response) throws SQLException {
        if (isSuccess(response)) {
            return response.getBody();
        } else {
            return handleFailure(response);
        }
    }

    /**
     * Try to find the reason and SQLState
     * in Dreamfactory error response
     *
     * @throws SQLException e
     */
    private String handleFailure(HttpResponse<String> response) throws SQLException {
        if (isBlank(response.getBody())) {
            throw new SQLException(String.format("http status [%d] - no response body", response.getStatus()));
        }

        var parsed = JsonPath.parse(response.getBody());

        if (isNull(parsed.read("$.error"))) {
            throw new SQLException();
        }

        var reason = "";
        var sqlState = "";

        if (!isNull(parsed.read("$.error.context"))) {
            reason = parsed.read("$.error.context.resource[0].message", String.class);
            sqlState = parsed.read("$.error.context.resource[0].code", String.class);
        } else if (!isNull(parsed.read("$.error.message"))) {
            reason = parsed.read("$.error.message");
            sqlState = getSqlState(reason);
        }
        throw new SQLException(reason, sqlState);
    }

    private String getSqlState(String msg) {
        var pattern = ".*(SQLSTATE\\[(.*)\\]).*";
        var matcher = Pattern
                .compile(pattern)
                .matcher(msg);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
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

    protected String get(String endpoint, HashMap<String, Object> params) {
        var request = createGetRequest(endpoint, params);
        var response = fireRequest(request);
        try {
            return handleResponse(response);
        } catch (SQLException e) {
            throw new RuntimeException("Could not process get request", e);
        }
    }

    /**
     * Create GET request
     */
    private HttpRequest createGetRequest(String table, HashMap<String, Object> filterParams) {
        HttpRequest request = Unirest.get(
                objectsDbUrl
                + table
                + "?filter="
                + createDreamfactoryParams(filterParams)
        );
        return addHeaders(request);
    }

    private boolean isSuccess(HttpResponse<String> response) {
        return response.getStatus() / 100 == 2;
    }

    /**
     * Create POST request
     */
    private HttpRequest createPostRequest(String table, String json, boolean isResource) {
        HttpRequestWithBody request;
        request = Unirest.post(objectsDbUrl + table);
        // wrap new entry in resource array:
        if (isResource) {
            json = format("{\"resource\" : [%s]}", json);
        }

        request.body(json);
        return addHeaders(request);
    }

    private HttpRequest addHeaders(HttpRequest request) {
        return request
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", objectsDbKey);
    }

}
