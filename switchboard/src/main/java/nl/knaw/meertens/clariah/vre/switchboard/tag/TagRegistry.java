package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.registry.AbstractDreamfactoryRegistry;

import java.io.IOException;
import java.sql.SQLException;

import static java.lang.String.format;

public class TagRegistry extends AbstractDreamfactoryRegistry {

    private final ObjectMapper mapper;
    private final String table = "/_table/tag";

    public TagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        super(objectsDbUrl, objectsDbKey);
        this.mapper = mapper;
    }

    public Long create(TagDto tag) {
        try {
            String json;
            try {
                json = postResource(mapper.writeValueAsString(tag), table);
            } catch (SQLException e) {
                String msg = "Could not create tag.";
                if(e.getSQLState().equals("23505")) {
                    msg += " Tag already exists.";
                }
                throw new RuntimeException(msg, e);

            }
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tag", e);
        }
    }

}
