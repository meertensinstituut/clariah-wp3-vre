package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

class TagRegistry extends AbstractDreamfactoryRegistry {

    private final ObjectMapper mapper;
    private final String table = "/_table/tag";

    private final Configuration conf = Configuration
            .builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();
    private final ParseContext jsonPath = JsonPath.using(conf);


    TagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        super(objectsDbUrl, objectsDbKey);
        this.mapper = mapper;
    }

    /**
     * Get id by name, owner and type of tag
     * @return Long id
     */

    Long get(CreateTagDto tag) {
        var params = new ArrayList<NameValueDto>();
        params.add(new NameValueDto("name", tag.name));
        params.add(new NameValueDto("owner", tag.owner));
        params.add(new NameValueDto("type", tag.type));
        var json = get(table, params);
        return jsonPath.parse(json).read("$.resource[0].id", Long.class);
    }

    Long create(CreateTagDto tag) throws SQLException {
        try {
            String json;
            json = postResource(mapper.writeValueAsString(tag), table);
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tag", e);
        }
    }

}
