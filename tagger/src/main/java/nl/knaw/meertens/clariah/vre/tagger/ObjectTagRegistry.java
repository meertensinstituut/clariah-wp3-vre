package nl.knaw.meertens.clariah.vre.tagger;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

class ObjectTagRegistry extends AbstractDreamfactoryRegistry {

    private final String insertObjectTagProcedure = "/_proc/insert_object_tag";
    private Logger logger = LoggerFactory.getLogger(ObjectTagRegistry.class);

    ObjectTagRegistry(String objectsDbUrl, String objectsDbKey) {
        super(objectsDbUrl, objectsDbKey);
    }

    Long createObjectTag(ObjectTagDto objectTag) {
        String json;
        try {
            json = postProcedure(objectTag.params, insertObjectTagProcedure);
        } catch (SQLException e) {
            var msg = String.format("Could not create object tag with tag [%s], object [%s], owner [%s]", objectTag.params.get("_tag"), objectTag.params.get("_object"), objectTag.params.get("_owner"));
            if ("23503".equals(e.getSQLState())) {
                msg += " Tag or object does not exist.";
            } else if ("23505".equals(e.getSQLState())) {
                msg += " Object tag already exists.";
            }
            throw new RuntimeException(msg, e);
        }
        try {
            return JsonPath.parse(json).read("$.id", Long.class);
        } catch (PathNotFoundException e) {
            logger.error("Could not parse response: " + json);
            throw new RuntimeException("Could not link object to tag", e);
        }
    }

}
