package nl.knaw.meertens.clariah.vre.recognizer.object;

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
import nl.knaw.meertens.clariah.vre.recognizer.Report;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_KEY;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectsRepositoryService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String objectsDbUrl;
  private final String objectsDbKey;
  private final String objectTable;
  private final ParseContext jsonPath;
  private final ObjectMapper mapper;

  public ObjectsRepositoryService(String objectsDbUrl, String objectsDbKey, String objectTable) {
    this.objectsDbUrl = objectsDbUrl;
    this.objectsDbKey = objectsDbKey;
    this.objectTable = objectTable;

    Configuration conf = Configuration
      .builder()
      .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
      .options(Option.SUPPRESS_EXCEPTIONS)
      .build();
    jsonPath = JsonPath.using(conf);

    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(WRITE_DATES_AS_TIMESTAMPS);
  }

  /**
   * Create new object in object registry
   *
   * @return objectId
   */
  public Long create(Report report) {
    String recordJson = createObjectRecordJson(report);
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
    String recordJson = createObjectRecordJson(report);
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

    String bodyWithNewPath = format("{ \"filepath\" : \"%s\" }", newPath);
    HttpResponse<String> patchResponse;
    try {
      patchResponse = Unirest
        .patch(patchObject)
        .header("Content-Type", "application/json")
        .header("X-DreamFactory-Api-Key", OBJECTS_DB_KEY)
        .body(bodyWithNewPath)
        .asString();
    } catch (UnirestException e) {
      throw new IllegalArgumentException(format(
        "Could not update path [%s]", newPath
      ), e);
    }

    if (!isSuccess(patchResponse)) {
      logger.error(format(
        "Could not patch object [%s] with new path [%s]; response: [%s]",
        id, newPath, patchResponse.getBody()
      ));
    }
    return id;
  }

  /**
   * Soft delete object with {path}
   * by setting object.deleted to true
   *
   * @return objectId
   */
  public Long softDelete(String path) {
    Long id = getObjectIdByPath(path);
    String deletedTrue = "{ \"deleted\" : true }";
    HttpResponse<String> patchResponse = patch(id, deletedTrue);

    if (!isSuccess(patchResponse)) {
      throw new RuntimeException((format(
        "Could not soft delete object [%s] [%s]; response: [%s][%s]",
        id, path, patchResponse.getStatus(), patchResponse.getBody()
      )));
    }
    return id;
  }

  private HttpResponse<String> patch(Long id, String patch) {
    String urlToPatch = getObjectUrl(id);

    HttpResponse<String> patchResponse;
    try {
      patchResponse = Unirest
        .patch(urlToPatch)
        .header("Content-Type", "application/json")
        .header("X-DreamFactory-Api-Key", objectsDbKey)
        .body(patch)
        .asString();
    } catch (UnirestException e) {
      throw new IllegalArgumentException(format(
        "Could not soft delete object [%d]", id
      ), e);
    }
    return patchResponse;
  }

  private String getObjectUrl(Long id) {
    return new StringBuilder()
      .append(objectsDbUrl)
      .append(objectTable)
      .append("/")
      .append(id)
      .toString();
  }

  /**
   * Find path of object that has not been deleted
   * Filter in dreamfactory by:
   * (filepath='{path}') AND (deleted='0')
   */
  private Long getObjectIdByPath(String path) {
    String getObjectByPath;

    String filter = "(filepath='" + path + "') AND (deleted='0')";
    getObjectByPath = objectsDbUrl +
      objectTable +
      "?limit=1&order=id%20DESC&filter=" +
      encodeUriComponent(filter);

    try {
      logger.info(format(
        "Request id by path [%s] using url [%s]",
        path, getObjectByPath
      ));
      HttpResponse<String> getIdResponse = Unirest
        .get(getObjectByPath)
        .header("Content-Type", "application/json")
        .header("X-DreamFactory-Api-Key", OBJECTS_DB_KEY)
        .asString();
      if (getIdResponse.getStatus() != 200) {
        throw new RuntimeException(format(
          "Retrieving id of path [%s]. Registry responded: [%s]",
          path, getIdResponse.getBody()
        ));
      }

      Long id = Long.valueOf(jsonPath.parse(getIdResponse.getBody()).read("$.resource[0].id"));
      logger.info(format("Path [%s] returned object id [%s]", path, id));
      return id;
    } catch (UnirestException e) {
      throw new RuntimeException(format(
        "Could not get object by path [%s]", path
      ), e);
    }
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

  public String createObjectRecordJson(Report report) {
    ObjectsRecordDto record = fillObjectsRecordDto(report);
    try {
      return mapper.writeValueAsString(record);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(format("Could not create json from [%s]", report.getPath()), e);
    }
  }

  /**
   * Update or create record
   */
  private String persistRecord(String recordJson, Long id) {
    if (!hasAllObjectsDbDetails()) {
      return null;
    }
    HttpResponse<String> response;
    try {
      RequestBodyEntity body = createPostOrPut(recordJson, id);
      response = body.asString();
    } catch (UnirestException e) {
      throw new RuntimeException(format(
        "Could not add [%s]",
        recordJson
      ), e);
    }

    if (isSuccess(response)) {
      logger.info("Persisted record in objects registry");
      return response.getBody();
    } else {
      throw new RuntimeException(format(
        "Could not add [%s]: [%s]",
        recordJson, response.getBody()
      ));
    }
  }

  /**
   * Create POST request when id is null, PUT otherwise
   */
  private RequestBodyEntity createPostOrPut(String recordJson, Long id) {
    HttpRequestWithBody request;
    if (id == null) {
      request = Unirest.post(objectsDbUrl + objectTable);
      // wrap new entry in resource array:
      recordJson = format("{\"resource\" : [%s]}", recordJson);
    } else {
      request = Unirest.put(objectsDbUrl + objectTable + "/" + id);
    }
    logger.info(format(
      "Create request for object with id [%d]: [%s] [%s] [%s]",
      id, request.getHttpMethod(), request.getUrl(), abbreviate(recordJson, 1000))
    );
    return request
      .header("Content-Type", "application/json")
      .header("X-DreamFactory-Api-Key", objectsDbKey)
      .body(recordJson);
  }

  private boolean isSuccess(HttpResponse<String> response) {
    return response.getStatus() / 100 == 2;
  }

  private ObjectsRecordDto fillObjectsRecordDto(Report report) {
    ObjectsRecordDto msg = new ObjectsRecordDto();
    msg.filepath = report.getPath();
    msg.fits = report.getXml();

    msg.mimetype = FitsService.getMimeType(report.getFits());
    msg.format = FitsService.getIdentity(report.getFits()).getFormat();

    msg.timeChanged = LocalDateTime.now();
    msg.timeCreated = LocalDateTime.now();
    msg.userId = report.getUser();
    msg.type = "object";
    msg.deleted = false;
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
