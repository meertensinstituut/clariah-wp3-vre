package nl.knaw.meertens.clariah.vre.switchboard.registry;

import com.google.common.base.Joiner;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import nl.knaw.meertens.clariah.vre.switchboard.tag.NameValueDto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AbstractDreamfactoryRegistry {

  private final String objectsDbUrl;
  private final String objectsDbKey;


  public AbstractDreamfactoryRegistry(String objectsDbUrl, String objectsDbKey) {

    this.objectsDbUrl = objectsDbUrl;
    this.objectsDbKey = objectsDbKey;
  }

  protected String postResource(
    String json,
    String endpoint
  ) throws SQLException {
    return post(json, endpoint, true);
  }

  protected String postProcedure(
    List<NameValueDto> params,
    String endpoint
  ) throws SQLException {
    var urlParams = Joiner.on("&").join(params.stream().map(p ->
      encodeUriComponent(p.name) +
        "=" +
        encodeUriComponent(p.value.toString())
    ).collect(toList()));
    var body = "";
    var isResource = false;
    return post(body, endpoint + "?" + urlParams, isResource);
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

  protected String delete(Map<String, String> filters, String endpoint) throws SQLException {
    var urlFilters = convert(filters);
    var request = createDeleteRequest(endpoint, urlFilters);
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
      throw new SQLException();
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
   * Convert filters to a url filter query param
   * that Dreamfactory understands
   */
  private String convert(Map<String, String> filters) {
    var filterParts = filters
      .entrySet()
      .stream()
      .map(entry -> "(" + entry.getKey() + " = " + entry.getValue() + ")")
      .collect(toList());
    var allFilters = Joiner
      .on(" AND ")
      .join(filterParts);
    return encodeUriComponent(allFilters);
  }

  /**
   * Source: technicaladvices.com/2012/02/20/java-encoding-similiar-to-javascript-encodeuricomponent/
   */
  private String encodeUriComponent(String filter) throws RuntimeException {
    try {
      return URLEncoder.encode(filter, "UTF-8")
                       .replaceAll("\\%28", "(")
                       .replaceAll("\\%29", ")")
                       .replaceAll("\\+", "%20")
                       .replaceAll("\\%27", "'")
                       .replaceAll("\\%21", "!")
                       .replaceAll("\\%7E", "~");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(format(
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
  private HttpRequest createPostRequest(String table, String json, boolean isResource) {
    HttpRequestWithBody request;
    request = Unirest.post(objectsDbUrl + table);
    // wrap new entry in resource array:
    if (isResource) {
      json = format("{\"resource\" : [%s]}", json);
    }
    return addHeaders(request)
      .body(json)
      .getHttpRequest();
  }

  /**
   * Create DELETE by filters request
   */
  private HttpRequest createDeleteRequest(String table, String filters) {
    var request = Unirest
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
