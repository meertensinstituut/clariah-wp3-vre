package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ObjectsRegistryServiceImpl implements ObjectsRegistryService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String objectTable = "/_table/object";
  private final String objectsDbUrl;
  private final String objectsDbKey;
  private final ObjectMapper mapper;

  public ObjectsRegistryServiceImpl(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
    if (!hasAllObjectsDbDetails(objectsDbKey, objectsDbUrl)) {
      throw new IllegalArgumentException("Not all arguments are provided.");
    }
    this.objectsDbUrl = objectsDbUrl;
    this.objectsDbKey = objectsDbKey;
    this.mapper = mapper;
  }

  @Override
  public ObjectsRecordDto getObjectById(Long id) {
    HttpResponse<String> response;
    var url = objectsDbUrl + objectTable + "/" + id;
    try {
      response = Unirest
        .get(url)
        .header("Content-Type", "application/json")
        .header("X-DreamFactory-Api-Key", objectsDbKey)
        .asString();
      if (isBlank(response.getBody())) {
        return new ObjectsRecordDto();
      } else {
        return mapper.readValue(response.getBody(), ObjectsRecordDto.class);
      }
    } catch (IOException | UnirestException e) {
      throw new RuntimeException(format("Could not retrieve object record [%d] from registry", id), e);
    }
  }

  private boolean hasAllObjectsDbDetails(String objectsDbKey, String objectsDbUrl) {
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
