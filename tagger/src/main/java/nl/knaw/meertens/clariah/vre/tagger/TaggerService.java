package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.RecognizerKafkaDto;
import nl.knaw.meertens.clariah.vre.tagger.kafka.TaggerKafkaDto;
import nl.knaw.meertens.clariah.vre.tagger.object_tag.ObjectTagDto;
import nl.knaw.meertens.clariah.vre.tagger.object_tag.ObjectTagRegistry;
import nl.knaw.meertens.clariah.vre.tagger.object_tag.UdateObjectTagDto;
import nl.knaw.meertens.clariah.vre.tagger.tag.AutomaticTagsService;
import nl.knaw.meertens.clariah.vre.tagger.tag.CreateTagDto;
import nl.knaw.meertens.clariah.vre.tagger.tag.TagDto;
import nl.knaw.meertens.clariah.vre.tagger.tag.TagRegistry;
import nl.knaw.meertens.clariah.vre.tagger.tag.UpdateTagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.tagger.Config.ACTIONS_TO_TAG;
import static nl.knaw.meertens.clariah.vre.tagger.Config.TEST_USER;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.CREATE;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.UPDATE;

class TaggerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ObjectMapper objectMapper;
    private final KafkaConsumerService kafkaConsumerService;
    private final KafkaProducerService kafkaProducerService;
    private final TagRegistry tagRegistry;
    private final ObjectTagRegistry objectTagRegistry;
    private final AutomaticTagsService automaticTagsService;

    TaggerService(
            ObjectMapper objectMapper,
            KafkaConsumerService kafkaConsumerService,
            KafkaProducerService kafkaProducerService,
            TagRegistry tagRegistry,
            ObjectTagRegistry objectTagRegistry,
            AutomaticTagsService automaticTagsService
    ) {
        this.objectMapper = objectMapper;
        this.kafkaConsumerService = kafkaConsumerService;
        this.kafkaProducerService = kafkaProducerService;
        this.tagRegistry = tagRegistry;
        this.objectTagRegistry = objectTagRegistry;
        this.automaticTagsService = automaticTagsService;
    }

    void consumeRecognizer() {
        kafkaConsumerService.consumeWith(this::consumeKafkaMsg);
    }

    void consumeKafkaMsg(String json) {
        try {
            var msg = objectMapper.readValue(json, RecognizerKafkaDto.class);
            var action = FileAction.from(msg.action);
            if (!ACTIONS_TO_TAG.contains(msg.action)) {
                logger.info(format(
                        "Ignored message about file [%s] with action [%s]",
                        msg.path, msg.action
                ));
                return;
            }
            if (action.equals(CREATE)) {
                tagNewObject(msg.objectId);
            } else if (action.equals(UPDATE)) {
                tagUpdatedObject(msg.objectId);
            }
        } catch (Exception e) {
            logger.error(String.format("Could not process kafka message [%s]", json), e);
        }
    }

    private void tagNewObject(Long objectId) {
        var tags = automaticTagsService.createNewTags(objectId);
        processTags(objectId, tags);
    }

    private void tagUpdatedObject(Long objectId) {
        var tags = automaticTagsService.createUpdateTags(objectId);
        processTags(objectId, tags);
    }

    private void processTags(Long objectId, List<TagDto> tags) {
        tags.forEach(tag -> {
            if(tag instanceof CreateTagDto) {
                var tagId = createTag(tag);
                createObjectTag(objectId, tagId);
                createKafkaMsg(objectId, tagId, tag, "Created new object tag");
            }
            if(tag instanceof UpdateTagDto) {
                var tagId = createTag(tag);
                updateObjectTag(objectId, tagId, tag.type, tag.owner);
                createKafkaMsg(objectId, tagId, tag, "Updated object tag");
            }
        });
    }

    /**
     * Create tag if it not already exists
     * @return Long tagId of existing or new tag
     */
    private Long createTag(TagDto tag) {
        var tagId = tagRegistry.get(tag);
        if (isNull(tagId)) {
            try {
                tagId = tagRegistry.create(tag);
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Could not create tag %s:%s:%s", tag.owner, tag.type, tag.name), e);
            }
        }
        return tagId;
    }

    private void createObjectTag(Long objectId, Long tagId) {
        objectTagRegistry.createObjectTag(
                new ObjectTagDto(
                        TEST_USER,
                        tagId,
                        objectId
                )
        );
    }

    private void updateObjectTag(Long objectId, Long newTagId, String type, String owner) {
        objectTagRegistry.updateObjectTag(new UdateObjectTagDto(objectId, newTagId, type, owner));
    }

    private void createKafkaMsg(Long objectId, Long tagId, TagDto tag, String msg) {
        var kafkaMsg = new TaggerKafkaDto();
        kafkaMsg.msg = msg;
        kafkaMsg.tag = tagId;
        kafkaMsg.object = objectId;
        kafkaMsg.owner = tag.owner;
        kafkaProducerService.send(kafkaMsg);
    }
}
