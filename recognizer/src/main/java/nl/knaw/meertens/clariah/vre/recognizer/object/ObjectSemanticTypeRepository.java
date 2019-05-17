package nl.knaw.meertens.clariah.vre.recognizer.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ObjectSemanticTypeRepository extends AbstractDreamfactoryRepository {

  private final String objectSemanticTypeTable;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public ObjectSemanticTypeRepository(
    String objectsDbUrl,
    String objectsDbKey,
    String objectSemanticTypeTable,
    ObjectMapper mapper
  ) {
    super(objectsDbUrl, objectsDbKey, mapper);
    this.objectSemanticTypeTable = objectSemanticTypeTable;
  }

  /**
   * Post symantic types
   *
   * <p>Does not check if semantic types already exist for object
   */
  public void createSemanticTypes(long objectRecordId, List<String> semanticTypes) {
    if (semanticTypes.isEmpty()) {
      logger.info(format("No semantic types for [%s], skip response", objectRecordId));
      return;
    }
    HttpResponse<String> response;
    try {

      class ObjectSemanticType {
        @JsonProperty("object_id")
        public Long objectId;

        @JsonProperty("semantic_type")
        public String semanticType;

        private ObjectSemanticType(Long objectId, String semanticType) {
          this.objectId = objectId;
          this.semanticType = semanticType;
        }
      }

      var objectSemanticTypes = semanticTypes
        .stream()
        .map((st) -> new ObjectSemanticType(objectRecordId, st))
        .collect(Collectors.toList());

      var requestBody = format("{\"resource\" : %s}", mapper.writeValueAsString(objectSemanticTypes));
      response = Unirest
        .post(objectsDbUrl + objectSemanticTypeTable)
        .header("Content-Type", "application/json")
        .header("X-DreamFactory-Api-Key", objectsDbKey)
        .body(requestBody)
        .asString();

      if (response.getStatus() != 200) {
        throw new IllegalStateException(format(
          "Expected status 200 but was [%d][%s]",
          response.getStatus(),
          response.getBody()
        ));
      }
    } catch (IllegalStateException | JsonProcessingException | UnirestException e) {
      logger.error(format("Could not persist semantic types of [%d]", objectRecordId), e);
    }
  }

  /**
   * Delete symantic types
   */
  public void deleteSemanticTypes(long objectRecordId) {
    HttpResponse<String> request = null;
    try {
      request = Unirest
        .delete(objectsDbUrl + objectSemanticTypeTable +
          "?filter=" + encodeUriComponent("(object_id=" + objectRecordId + ")")
        ).header("Content-Type", "application/json")
        .header("X-DreamFactory-Api-Key", objectsDbKey)
        .asString();
      if (request.getStatus() != 200) {
        throw new IllegalStateException(format("Expected status 200 but was [%d]", request.getStatus()));
      }
    } catch (IllegalStateException | UnirestException e) {
      logger.error(format("Could not delete semantic types of [%d]", objectRecordId), e);
    }
  }


}
