package nl.knaw.meertens.clariah.vre.switchboard.tag;

public class TagService {

    private final TagRegistry tagRegistry;

    public TagService(TagRegistry tagRegistry) {
        this.tagRegistry = tagRegistry;
    }

    public Long createTag(TagDto tag) {
        return tagRegistry.create(tag);
    }
}
