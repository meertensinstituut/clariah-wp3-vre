package nl.knaw.meertens.clariah.vre.tagger.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.object.ObjectRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.tagger.Config.SYSTEM_TAG_OWNER;

public class AutomaticTagsService {
  private final ObjectRegistry objectRegistry;

  private String owner = SYSTEM_TAG_OWNER;

  public AutomaticTagsService(ObjectMapper objectMapper, String objectsDbUrl, String objectsDbKey) {
    objectRegistry = new ObjectRegistry(
      objectsDbUrl,
      objectsDbKey,
      objectMapper
    );
  }

  public List<TagDto> createNewTags(Long objectId) {
    var object = objectRegistry.getObjectById(objectId);
    var result = new ArrayList<TagDto>();

    var timeCreated = toLocalDateTime(object.timeCreated);
    result.add(createTagCreationTimeYmdhm(timeCreated));
    result.add(createTagCreationTimeYmd(timeCreated));
    result.add(createTagCreationTimeYm(timeCreated));
    result.add(createTagCreationTimeY(timeCreated));

    var timeChanged = toLocalDateTime(object.timeCreated);
    result.add(createTagModificationTimeYmdhm(timeChanged, newTag(CreateTagDto.class)));
    result.add(createTagModificationTimeYmd(timeChanged, newTag(CreateTagDto.class)));
    result.add(createTagModificationTimeYm(timeChanged, newTag(CreateTagDto.class)));
    result.add(createTagModificationTimeY(timeChanged, newTag(CreateTagDto.class)));

    var path = Paths.get(object.filepath);

    // is there a path to tag besides the file and a two-part nextcloud-prefix?
    if (Paths.get(object.filepath).getNameCount() > 3) {
      result.add(createPath(path, newTag(CreateTagDto.class)));
      result.addAll(createDirs(path));
    }

    return result;
  }

  public List<TagDto> createUpdateTags(Long objectId) {
    var object = objectRegistry.getObjectById(objectId);
    var result = new ArrayList<TagDto>();
    var timeChanged = toLocalDateTime(object.timeChanged);

    result.add(createTagModificationTimeYmdhm(timeChanged, newTag(UpdateTagDto.class)));
    result.add(createTagModificationTimeYmd(timeChanged, newTag(UpdateTagDto.class)));
    result.add(createTagModificationTimeYm(timeChanged, newTag(UpdateTagDto.class)));
    result.add(createTagModificationTimeY(timeChanged, newTag(UpdateTagDto.class)));

    var path = Paths.get(object.filepath);

    // is there a path to tag?
    if (path.getNameCount() > 3) {
      result.add(createPath(path, newTag(UpdateTagDto.class)));
      result.addAll(createDirs(path));
    }

    return result;
  }

  private LocalDateTime toLocalDateTime(String timeChanged) {
    return LocalDateTime.parse(
      timeChanged.substring(0, 16),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );
  }

  private List<TagDto> createDirs(Path filePath) {
    List<TagDto> result = new ArrayList<>();
    var iterator = getTagPath(filePath).iterator();
    iterator.forEachRemaining(p -> {
      result.add(createDir(p));
    });
    return result;
  }

  /**
   * Create path stripped from `/{user}/files` and file
   */
  private Path getTagPath(Path filePath) {
    return filePath.subpath(2, filePath.getNameCount() - 1);
  }

  private TagDto createDir(Path path) {
    var tag = newTag(CreateTagDto.class);
    tag.type = "dir";
    tag.name = path.toString();
    return tag;
  }

  private TagDto createPath(Path filePath, TagDto tag) {
    tag.type = "path";
    tag.name = getTagPath(filePath).toString();
    return tag;
  }

  private TagDto createTagCreationTimeYmdhm(LocalDateTime timeCreated) {
    var tag = newTag(CreateTagDto.class);
    tag.type = "creation-time-ymdhm";
    tag.name = timeCreated.format(ofPattern("yyyy-MM-dd HH:mm"));
    return tag;
  }

  private TagDto createTagCreationTimeYmd(LocalDateTime timeCreated) {
    var tag = newTag(CreateTagDto.class);
    tag.type = "creation-time-ymd";
    tag.name = timeCreated.format(ofPattern("yyyy-MM-dd"));
    return tag;
  }

  private TagDto createTagCreationTimeYm(LocalDateTime timeCreated) {
    var tag = newTag(CreateTagDto.class);
    tag.type = "creation-time-ym";
    tag.name = timeCreated.format(ofPattern("yyyy-MM"));
    return tag;
  }

  private TagDto createTagCreationTimeY(LocalDateTime timeCreated) {
    var tag = newTag(CreateTagDto.class);
    tag.type = "creation-time-y";
    tag.name = timeCreated.format(ofPattern("yyyy"));
    return tag;
  }

  private TagDto createTagModificationTimeYmdhm(LocalDateTime timeCreated, TagDto tag) {
    tag.type = "modification-time-ymdhm";
    tag.name = timeCreated.format(ofPattern("yyyy-MM-dd HH:mm"));
    return tag;
  }

  private TagDto createTagModificationTimeYmd(LocalDateTime timeCreated, TagDto tag) {
    tag.type = "modification-time-ymd";
    tag.name = timeCreated.format(ofPattern("yyyy-MM-dd"));
    return tag;
  }

  private TagDto createTagModificationTimeYm(LocalDateTime timeCreated, TagDto tag) {
    tag.type = "modification-time-ym";
    tag.name = timeCreated.format(ofPattern("yyyy-MM"));
    return tag;
  }

  private TagDto createTagModificationTimeY(LocalDateTime timeCreated, TagDto tag) {
    tag.type = "modification-time-y";
    tag.name = timeCreated.format(ofPattern("yyyy"));
    return tag;
  }

  private <T extends TagDto> TagDto newTag(Class<T> type) {
    if (type == CreateTagDto.class) {
      return new CreateTagDto(owner);
    } else {
      return new UpdateTagDto(owner);
    }
  }
}
