package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.RecognizerKafkaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.tagger.Config.ACTIONS_TO_TAG;
import static nl.knaw.meertens.clariah.vre.tagger.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.tagger.Config.RECOGNIZER_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.Config.RECOGNIZER_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.CREATE;

class TaggerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(KAFKA_SERVER, RECOGNIZER_TOPIC_NAME, RECOGNIZER_GROUP_NAME);

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
        // TODO: check if tags exist
        // TODO: if not: create tag and create kafka msg
        // TODO: create object tag and create kafka msg
    }
}
