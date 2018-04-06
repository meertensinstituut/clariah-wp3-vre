package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.example.TestConsumer;
import nl.knaw.meertens.clariah.vre.example.TestProducer;
import nl.knaw.meertens.clariah.vre.recognizer.dto.OwncloudKafkaDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    private static final String kafkaServer = "kafka:" + System.getenv("KAFKA_PORT");
    private static final String owncloudTopic = System.getenv("OWNCLOUD_TOPIC_NAME");
    private static final String owncloudGroup = System.getenv("OWNCLOUD_GROUP_NAME");
    private static final String recognizerTopic = System.getenv("RECOGNIZER_TOPIC_NAME");
    private static final String fitsFilesRoot = System.getenv("FITS_FILES_ROOT");

    private static final String fitsUrl = "http://fits:8080/fits/";
    private static final String objectsDbUrl = "http://dreamfactory/api/v2/objects";
    private static final String objectsDbKey = System.getenv("APP_KEY_OBJECTS");
    private static final String objectsDbToken = System.getenv("OBJECTS_TOKEN");

    private static final FitsService fitsService = new FitsService(fitsUrl);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final KafkaConsumerService owncloudConsumerService = new KafkaConsumerService(kafkaServer, owncloudTopic, owncloudGroup);

    private static final KafkaProducerService kafkaProducer = new KafkaProducerService(new RecognizerKafkaProducer(kafkaServer), recognizerTopic);
    private static final ObjectsRepositoryService objectsRepository = new ObjectsRepositoryService(objectsDbUrl, objectsDbKey, objectsDbToken);

    private static final List<String> ACTIONS_TO_RECOGNIZE = newArrayList("create", "read");

    public static void main(String[] args) {

        switch (args[0]) {
            case "consume":
                startConsuming();
                break;
            case "test-produce":
                startProducing();
                break;
            case "test-consume-owncloud":
                startTestConsumingOwncloud();
                break;
            case "test-consume-recognizer":
                startTestConsumingRecognizer();
                break;
            default:
                logger.info("Run application with argument 'consume', 'test-consume-owncloud', 'test-consume-recognizer', or 'test-produce'.");
                break;
        }
    }

    private static void startProducing() {
        logger.info("Produce test dto...");
        TestProducer.produce(kafkaServer, owncloudTopic);
    }

    private static void startConsuming() {
        consumeOwncloud();
    }

    private static void consumeOwncloud() {
        logger.info("Start consuming owncloud...");
        owncloudConsumerService.consumeWith((String json) -> {
            try {
                OwncloudKafkaDTO msg = objectMapper.readValue(json, OwncloudKafkaDTO.class);
                if(!ACTIONS_TO_RECOGNIZE.contains(msg.action)) {
                    logger.info(format("Ignored owncloud kafka message with action '%s'.", msg.action));
                    return;
                }
                if (StringUtils.isBlank(msg.userPath)) {
                    throw new RuntimeException("Field userPath is empty of: " + msg.toString());
                }
                String fitsPath = new File(fitsFilesRoot + "/" + msg.userPath).getCanonicalPath();
                Report report = fitsService.checkFile(fitsPath);
                report.setUser(msg.user);
                report.setPath(msg.userPath);
                report.setObjectId(objectsRepository.perist(report));
                kafkaProducer.produceToRecognizerTopic(report);
            } catch (Exception e) {
                logger.info("What we've got here is failure to recognize:");
                e.printStackTrace();
            }
        });
    }

    private static void startTestConsumingRecognizer() {
        logger.info("Start test: consuming recognizer...");
        TestConsumer.consume(kafkaServer, recognizerTopic);
    }

    private static void startTestConsumingOwncloud() {
        logger.info("Start test: consuming owncloud...");
        TestConsumer.consume(kafkaServer, owncloudTopic);
    }

}
