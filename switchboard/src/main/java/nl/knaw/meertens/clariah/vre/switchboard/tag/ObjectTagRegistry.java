package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.registry.AbstractDreamfactoryRegistry;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

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


    public Long deleteObjectTag(ObjectTagDto objectTag) {
        String json;
        try {
            Map<String, String> filters = new HashMap<>();
            filters.put("object", "" + objectTag.object);
            filters.put("tag", "" + objectTag.tag);
            json = delete(filters);
        } catch (SQLException e) {
            String msg = String.format(
                    "Could not delete link between object [%d] and tag [%d].",
                    objectTag.object, objectTag.tag
            );
            throw new RuntimeException(msg, e);
        }
        return JsonPath.parse(json).read("$.resource[0].id", Long.class);
    }
}
