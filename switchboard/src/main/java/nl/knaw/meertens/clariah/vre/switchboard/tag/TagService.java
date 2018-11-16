package nl.knaw.meertens.clariah.vre.switchboard.tag;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.TEST_USER;

public class TagService {

  private final TagRegistry tagRegistry;
  private final ObjectTagRegistry objectTagRegistry;

  public TagService(TagRegistry tagRegistry, ObjectTagRegistry objectTagRegistry) {
    this.tagRegistry = tagRegistry;
    this.objectTagRegistry = objectTagRegistry;
  }

  public Long createTag(TagDto tag) {
    // TODO: use shibboleth user:
    tag.owner = TEST_USER;
    return tagRegistry.create(tag);
  }

  public Long tagObject(Long object, Long tag) {
    // TODO: use shibboleth user:
    var objectTag = new CreateObjectTagDto(TEST_USER, tag, object);
    return objectTagRegistry.createObjectTag(objectTag);
  }

  public Long untagObject(Long object, Long tag) {
    var objectTag = new ObjectTagDto();
    objectTag.object = object;
    objectTag.tag = tag;
    return objectTagRegistry.deleteObjectTag(objectTag);
  }

  public Long deleteTag(Long tag) {
    return tagRegistry.deleteTag(tag);
  }
}
