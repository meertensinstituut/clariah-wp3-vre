package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.registry.AbstractDreamfactoryRegistry;

import java.io.IOException;

public class ObjectTagRegistry extends AbstractDreamfactoryRegistry {

    private final ObjectMapper mapper;

    public ObjectTagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        super(objectsDbUrl, objectsDbKey, "/_table/object_tag");
        this.mapper = mapper;
    }

    public Long createObjectTag(ObjectTagDto objectTag) {
        try {
            String json = post(mapper.writeValueAsString(objectTag));
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Could link object to tag", e);
        }
    }


}
