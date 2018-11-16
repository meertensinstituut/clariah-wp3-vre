package nl.knaw.meertens.clariah.vre.tagger.object;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.AbstractDreamfactoryRegistry;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECT_TABLE;

public class ObjectRegistry extends AbstractDreamfactoryRegistry {

  private final String objectTable = OBJECT_TABLE;
  private final ObjectMapper mapper;

  public ObjectRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper objectMapper) {
    super(objectsDbUrl, objectsDbKey);
    this.mapper = objectMapper;
  }

  public ObjectsDto getObjectById(Long id) {
    var params = new HashMap<String, Object>();
    params.put("id", id);
    var result = get(objectTable, params);
    try {
      var resource = mapper.readTree(result).at("/resource");
      var reader = mapper.readerFor(new TypeReference<List<ObjectsDto>>() {
      });
      List<ObjectsDto> objectRecords = reader.readValue(resource);
      if (objectRecords.isEmpty()) {
        throw new IllegalStateException(String.format("No object found with id [%d]", id));
      }
      return objectRecords.get(0);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Could not retrieve object record [%d] from registry", id), e);
    }
  }

}
