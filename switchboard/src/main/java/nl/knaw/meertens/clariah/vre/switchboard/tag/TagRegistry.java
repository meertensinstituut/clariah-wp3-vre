package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.registry.AbstractDreamfactoryRegistry;

import java.io.IOException;

import static java.lang.String.format;

public class TagRegistry extends AbstractDreamfactoryRegistry {

    private final ObjectMapper mapper;

    public TagRegistry(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
        super(objectsDbUrl, objectsDbKey, "/_table/tag");
        this.mapper = mapper;
    }

    public Long create(TagDto tag) {
        try {
            String json = post(mapper.writeValueAsString(tag));
            return JsonPath.parse(json).read("$.resource[0].id", Long.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tag", e);
        }
    }

}
