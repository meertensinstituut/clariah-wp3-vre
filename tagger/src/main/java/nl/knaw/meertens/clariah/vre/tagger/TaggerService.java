package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.TaggerKafkaDto;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerServiceImpl;
import nl.knaw.meertens.clariah.vre.tagger.kafka.RecognizerKafkaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.tagger.Config.ACTIONS_TO_TAG;
import static nl.knaw.meertens.clariah.vre.tagger.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.tagger.Config.RECOGNIZER_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.Config.RECOGNIZER_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.Config.TAGGER_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.Config.TEST_USER;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.CREATE;

class TaggerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(
            KAFKA_SERVER,
            RECOGNIZER_TOPIC_NAME,
            RECOGNIZER_GROUP_NAME
    );
    private KafkaProducerService kafkaProducer = new KafkaProducerServiceImpl(
            TAGGER_TOPIC_NAME,
            KAFKA_SERVER,
            objectMapper
    );
    private final TagRegistry tagRegistry = new TagRegistry(
            OBJECTS_DB_URL,
            OBJECTS_DB_KEY,
            objectMapper
    );
    private final ObjectTagRegistry objectTagRegistry = new ObjectTagRegistry(
            OBJECTS_DB_URL,
            OBJECTS_DB_KEY
    );

    void consumeRecognizer() {
        kafkaConsumerService.consumeWith((String json) -> {
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
        });
    }

    private void tagObjects(Long objectId) {
        var tag = new TagDto();
        tag.type = "creation-time-ymdhm";
        tag.name = "2018-10-30 16:15";
        tag.owner = "system";
        var tagId = tagRegistry.get(tag);
        if (isNull(tagId)) {
            try {
                tagId = tagRegistry.create(tag);
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Could not create tag %s:%s:%s", tag.owner, tag.type, tag.name), e);
            }
        }
        objectTagRegistry.createObjectTag(new ObjectTagDto(TEST_USER, tagId, objectId));
        var kafkaMsg = new TaggerKafkaDto();

        kafkaMsg.msg = "Created new object tag";
        kafkaMsg.tag = tagId;
        kafkaMsg.object = objectId;
        kafkaMsg.owner = tag.owner;

        kafkaProducer.send(kafkaMsg);
    }
}
