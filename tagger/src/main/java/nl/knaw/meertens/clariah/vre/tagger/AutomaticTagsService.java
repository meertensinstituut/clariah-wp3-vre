package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.tagger.Config.SYSTEM_TAG_OWNER;

class AutomaticTagsService {

    private final ObjectRegistry objectRegistry;
    private String owner = SYSTEM_TAG_OWNER;

    AutomaticTagsService(ObjectMapper objectMapper) {
        objectRegistry =  new ObjectRegistry(
                OBJECTS_DB_URL,
                OBJECTS_DB_KEY,
                objectMapper
        );
    }

    ArrayList<CreateTagDto> createTags(Long objectId) {
        var object = objectRegistry.getObjectById(objectId);
        var result = new ArrayList<CreateTagDto>();
        result.add(createTagCreationTimeYmdhm(object.timeCreated));
        result.add(createTagCreationTimeYmd(object.timeCreated));
        result.add(createTagCreationTimeYm(object.timeCreated));
        result.add(createTagCreationTimeY(object.timeCreated));
        result.add(createPath(object.filepath));
        result.addAll(createDirs(object.filepath));
        return result;
    }

    private List<CreateTagDto> createDirs(String filepath) {
        List<CreateTagDto> result = new ArrayList<>();
        var path = Paths.get(filepath);
        path.iterator().forEachRemaining(p -> {
            result.add(createDir(p));
        });
        return result;
    }

    private CreateTagDto createDir(Path p) {
        var tag = new CreateTagDto();
        tag.type = "path";
        tag.name = p.toString();
        tag.owner = owner;
        return tag;
    }

    private CreateTagDto createPath(String filePath) {
        var tag = new CreateTagDto();
        tag.type = "path";
        tag.name = filePath;
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
