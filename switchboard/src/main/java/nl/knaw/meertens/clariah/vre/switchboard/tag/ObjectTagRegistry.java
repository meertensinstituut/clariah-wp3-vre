package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import nl.knaw.meertens.clariah.vre.switchboard.registry.AbstractDreamfactoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ObjectTagRegistry extends AbstractDreamfactoryRegistry {

    private final String table = "/_table/object_tag";
    private final String funcTagObject = "/_func/insert_object_tag";
    private Logger logger = LoggerFactory.getLogger(ObjectTagRegistry.class);

    public ObjectTagRegistry(String objectsDbUrl, String objectsDbKey) {
        super(objectsDbUrl, objectsDbKey);
    }

    public Long createObjectTag(CreateObjectTagDto objectTag) {
        String json = null;
        try {
            try {
                json = postFunc(objectTag.params, funcTagObject);
            } catch (SQLException e) {
                String msg = "Could not create object tag.";
                if(e.getSQLState().equals("23503")) {
                    msg += " Tag or object does not exist.";
                } else if(e.getSQLState().equals("23505")) {
                    msg += " Object tag already exists.";
                }
                throw new RuntimeException(msg, e);
            }
            return JsonPath.parse(json).read("$.id", Long.class);
        } catch (PathNotFoundException e) {
            logger.error("Could not parse response: " + json);
            throw new RuntimeException("Could not link object to tag", e);
        }
    }


    public Long deleteObjectTag(ObjectTagDto objectTag) {
        String json;
        try {
            Map<String, String> filters = new HashMap<>();
            filters.put("object", "" + objectTag.object);
            filters.put("tag", "" + objectTag.tag);
            json = delete(filters, table);
        } catch (SQLException e) {
            String msg = String.format(
                    "Could not delete link between object [%d] and tag [%d].",
                    objectTag.object, objectTag.tag
            );
            throw new RuntimeException(msg, e);
        }
        try {
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (PathNotFoundException e) {
            throw new RuntimeException(String.format(
                    "Link between tag [%d] and object [%d] could not be found",
                    objectTag.tag, objectTag.object
            ));
        }
    }
}
