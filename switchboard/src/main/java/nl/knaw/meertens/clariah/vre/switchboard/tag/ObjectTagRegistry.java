package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.registry.AbstractDreamfactoryRegistry;

import java.io.IOException;
import java.sql.SQLException;

public class ObjectTagRegistry extends AbstractDreamfactoryRegistry {

    private final ObjectMapper mapper;

    public ObjectTagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        super(objectsDbUrl, objectsDbKey, "/_table/object_tag");
        this.mapper = mapper;
    }

    public Long createObjectTag(ObjectTagDto objectTag) {
        try {
            String json = null;
            try {
                json = post(mapper.writeValueAsString(objectTag));
            } catch (SQLException e) {
                String msg = "Could not create object tag.";
                if(e.getSQLState().equals("23503")) {
                    msg += " Tag or object does not exist.";
                } else if(e.getSQLState().equals("23505")) {
                    msg += " Object tag already exists.";
                }
                throw new RuntimeException(msg, e);
            }
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Could link object to tag", e);
        }
    }


}
