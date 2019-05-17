package nl.knaw.meertens.clariah.vre.recognizer.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_KEY;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectRepository extends AbstractDreamfactoryRepository {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private String objectTable;

  public ObjectRepository(
    String objectsDbUrl,
    String objectsDbKey,
    String objectTable,
    ObjectMapper mapper
  ) {
    super(objectsDbUrl, objectsDbKey, mapper);
    this.objectTable = objectTable;
  }

  /**
   * Update oldPath with newPath of object
   *
   * @return objectId
   */
  public Long updatePath(String oldPath, String newPath) {
    var id = getObjectIdByPath(oldPath);
    var patchObject = getObjectUrl(id);
    var bodyWithNewPath = format("{ \"filepath\" : \"%s\" }", newPath);

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
    var id = getObjectIdByPath(path);
    var deletedTrue = "{ \"deleted\" : true }";
    var patchResponse = patch(id, deletedTrue);

    if (!isSuccess(patchResponse)) {
      throw new RuntimeException((format(
        "Could not soft delete object [%s] [%s]; response: [%s][%s]",
        id, path, patchResponse.getStatus(), patchResponse.getBody()
      )));
    }
    return id;
  }

  /**
   * Patch object
   */
  private HttpResponse<String> patch(Long id, String patch) {
    var urlToPatch = getObjectUrl(id);

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

  /**
   * Find path of object that has not been deleted
   * Filter in dreamfactory by:
   * (filepath='{path}') AND (deleted='0')
   */
  public Long getObjectIdByPath(String path) {
    var filter = "(filepath='" + path + "') AND (deleted='0')";
    var getObjectByPath = objectsDbUrl +
      objectTable +
      "?limit=1&order=id%20DESC&filter=" +
      encodeUriComponent(filter);

    try {
      logger.info(format(
        "Request id by path [%s] using url [%s]",
        path, getObjectByPath
      ));
      var getIdResponse = Unirest
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
   * Update or create record
   */
  public Long persistObject(ObjectsRecordDto dto, Long id) {
    if (!hasAllObjectsDbDetails()) {
      return null;
    }
    var recordJson = "";
    try {
      recordJson = mapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(format("Could not create record json for [%s]", dto.filepath), e);
    }

    HttpResponse<String> response;
    try {
      var body = createPostOrPut(recordJson, id);
      response = body.asString();
    } catch (UnirestException e) {
      throw new RuntimeException(format(
        "Could not add [%s]",
        recordJson
      ), e);
    }

    String body = response.getBody();
    if (isSuccess(response)) {
      logger.info("Persisted record in objects registry");
      logger.info("persisted object response:" + body);
      return getIdFromPersistedObject(body);
    } else {
      throw new RuntimeException(format(
        "Could not add [%s]: [%s]",
        recordJson, body
      ));
    }
  }

  private Long getIdFromPersistedObject(String body) {
    var id = jsonPath
      .parse(body)
      .read("$.id", String.class);

    if (id == null) {
      id = jsonPath
        .parse(body)
        .read("$.resource[0].id", String.class);
    }

    return Long.valueOf(id);
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

  private String getObjectUrl(Long id) {
    return objectsDbUrl +
      objectTable +
      "/" +
      id;
  }

  private boolean isSuccess(HttpResponse<String> response) {
    return response.getStatus() / 100 == 2;
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
