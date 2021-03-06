package nl.knaw.meertens.clariah.vre.tagger.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import nl.knaw.meertens.clariah.vre.tagger.AbstractDreamfactoryRegistry;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class TagRegistry extends AbstractDreamfactoryRegistry {

  private final ObjectMapper mapper;
  private final String table = "/_table/tag/";

  private final Configuration jsonPathConf = Configuration
    .builder()
    .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
    .options(Option.SUPPRESS_EXCEPTIONS)
    .build();
  private final ParseContext jsonPath = JsonPath.using(jsonPathConf);

  public TagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
    super(objectsDbUrl, objectsDbKey);
    this.mapper = mapper;
  }

  /**
   * Get id by name, owner and type of tag
   *
   * @return Long id
   */

  public Long get(TagDto tag) {
    var params = new HashMap<String, Object>();
    params.put("name", tag.name);
    params.put("owner", tag.owner);
    params.put("type", tag.type);
    var json = get(table, params);
    return jsonPath.parse(json).read("$.resource[0].id", Long.class);
  }

  public Long create(TagDto tag) throws SQLException {
    try {
      var json = postResource(mapper.writeValueAsString(tag), table);
      return JsonPath.parse(json).read("$.resource[0].id", Long.class);
    } catch (IOException e) {
      throw new RuntimeException("Could not create tag", e);
    }
  }

}
