package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.RecognizerKafkaDto;
import nl.knaw.meertens.clariah.vre.tagger.kafka.TaggerKafkaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.tagger.Config.ACTIONS_TO_TAG;
import static nl.knaw.meertens.clariah.vre.tagger.Config.TEST_USER;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.CREATE;

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
                tagObjects(msg.objectId);
            }
        } catch (Exception e) {
            logger.error(String.format("Could not process kafka message [%s]", json), e);
        }
    }

    private void tagObjects(Long objectId) {
        var tags = automaticTagsService.createTags(objectId);
        tags.forEach(tag -> {
            var tagId = createTag(tag);
            createObjectTag(objectId, tagId);
            createKafkaMsg(objectId, tagId, tag);
        });
    }

    /**
     * Create tag if it not already exists
     * @return Long tagId
     */
    private Long createTag(CreateTagDto tag) {
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

    private void createKafkaMsg(Long objectId, Long tagId, CreateTagDto tag) {
        var kafkaMsg = new TaggerKafkaDto();
        kafkaMsg.msg = "Created new object tag";
        kafkaMsg.tag = tagId;
        kafkaMsg.object = objectId;
        kafkaMsg.owner = tag.owner;
        kafkaProducerService.send(kafkaMsg);
    }
}
