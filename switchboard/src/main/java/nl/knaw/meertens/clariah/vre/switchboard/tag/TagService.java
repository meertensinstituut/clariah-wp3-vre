package nl.knaw.meertens.clariah.vre.switchboard.tag;

import java.time.LocalDateTime;

public class TagService {

    private final TagRegistry tagRegistry;
    private final ObjectTagRegistry objectTagRegistry;

    public TagService(TagRegistry tagRegistry, ObjectTagRegistry objectTagRegistry) {
        this.tagRegistry = tagRegistry;
        this.objectTagRegistry = objectTagRegistry;
    }

    public Long createTag(TagDto tag) {
        return tagRegistry.create(tag);
    }

    public Long tagObject(Long object, Long tag) {
        ObjectTagDto objectTag = new ObjectTagDto();
        objectTag.object = object;
        objectTag.tag = tag;
        objectTag.timestamp = LocalDateTime.now();
        return objectTagRegistry.createObjectTag(objectTag);
    }
}
