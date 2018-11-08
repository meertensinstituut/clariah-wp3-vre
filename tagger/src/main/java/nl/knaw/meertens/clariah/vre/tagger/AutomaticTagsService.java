package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.tagger.Config.SYSTEM_TAG_OWNER;

class AutomaticTagsService {

    private final ObjectRegistry objectRegistry;
    private String owner = SYSTEM_TAG_OWNER;

    AutomaticTagsService(ObjectMapper objectMapper, String objectsDbUrl, String objectsDbKey) {
        objectRegistry =  new ObjectRegistry(
                objectsDbUrl,
                objectsDbKey,
                objectMapper
        );
    }

    ArrayList<CreateTagDto> createTags(Long objectId) {
        var object = objectRegistry.getObjectById(objectId);
        var result = new ArrayList<CreateTagDto>();
        var timeCreated = LocalDateTime.parse(
                object.timeCreated.substring(0, 16),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        result.add(createTagCreationTimeYmdhm(timeCreated));
        result.add(createTagCreationTimeYmd(timeCreated));
        result.add(createTagCreationTimeYm(timeCreated));
        result.add(createTagCreationTimeY(timeCreated));
        result.add(createPath(object.filepath));
        result.addAll(createDirs(object.filepath));
        return result;
    }

    private List<CreateTagDto> createDirs(String filepath) {
        List<CreateTagDto> result = new ArrayList<>();
        var path = Paths.get(filepath);
        var iterator = path.iterator();
        iterator.forEachRemaining(p -> {
            // one underlying dirs get this tag:
            if(iterator.hasNext()) {
                result.add(createDir(p));
            }
        });
        return result;
    }

    private CreateTagDto createDir(Path p) {
        var tag = new CreateTagDto();
        tag.type = "dir";
        tag.name = p.toString();
        tag.owner = owner;
        return tag;
    }

    private CreateTagDto createPath(String filePath) {
        var tag = new CreateTagDto();
        tag.type = "path";
        tag.name = filePath.substring(0, filePath.lastIndexOf('/'));
        tag.owner = owner;
        return tag;
    }

    private CreateTagDto createTagCreationTimeYmdhm(LocalDateTime timeCreated) {
        var tag = new CreateTagDto();
        tag.type = "creation-time-ymdhm";
        tag.name = timeCreated.format(ofPattern("yyyy-MM-dd HH:mm"));
        tag.owner = owner;
        return tag;
    }

    private CreateTagDto createTagCreationTimeYmd(LocalDateTime timeCreated) {
        var tag = new CreateTagDto();
        tag.type = "creation-time-ymd";
        tag.name = timeCreated.format(ofPattern("yyyy-MM-dd"));
        tag.owner = owner;
        return tag;
    }

    private CreateTagDto createTagCreationTimeYm(LocalDateTime timeCreated) {
        var tag = new CreateTagDto();
        tag.type = "creation-time-ym";
        tag.name = timeCreated.format(ofPattern("yyyy-MM"));
        tag.owner = owner;
        return tag;
    }

    private CreateTagDto createTagCreationTimeY(LocalDateTime timeCreated) {
        var tag = new CreateTagDto();
        tag.type = "creation-time-y";
        tag.name = timeCreated.format(ofPattern("yyyy"));
        tag.owner = owner;
        return tag;
    }

}
