package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.example.TestConsumer;
import nl.knaw.meertens.clariah.vre.example.TestProducer;
import nl.knaw.meertens.clariah.vre.recognizer.dto.OwncloudKafkaDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

public class App {

    public static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private static final FitsService fitsService = new FitsService(Config.FITS_URL, Config.FITS_FILES_ROOT);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final KafkaConsumerService owncloudConsumerService = new KafkaConsumerService(Config.KAFKA_SERVER, Config.OWNCLOUD_TOPIC_NAME, Config.OWNCLOUD_GROUP_NAME);

    private static final KafkaProducerService kafkaProducer = new KafkaProducerService(new RecognizerKafkaProducer(Config.KAFKA_SERVER), Config.RECOGNIZER_TOPIC_NAME);
    private static final ObjectsRepositoryService objectsRepository = new ObjectsRepositoryService(Config.OBJECTS_DB_URL, Config.OBJECTS_DB_KEY);


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
        TestProducer.produce(Config.KAFKA_SERVER, Config.OWNCLOUD_TOPIC_NAME);
    }

    private static void startConsuming() {
        logger.info("Start consuming owncloud...");
        consumeOwncloud();
    }

    private static void consumeOwncloud() {
        owncloudConsumerService.consumeWith((String json) -> {
            try {
                OwncloudKafkaDTO msg = objectMapper.readValue(json, OwncloudKafkaDTO.class);
                if(!Config.ACTIONS_TO_RECOGNIZE.contains(msg.action)) {
                    logger.info(format("Ignored message about file [%s] with action [%s]", msg.path, msg.action));
                    return;
                }
                if (StringUtils.isBlank(msg.path)) {
                    throw new IllegalArgumentException(String.format("Field path is empty of owncloud kafka msg [%s]", msg.toString()));
                }
                Report report = fitsService.checkFile(msg.path);
                report.setUser(msg.user);
                report.setPath(msg.path);
                report.setObjectId(objectsRepository.perist(report));
                kafkaProducer.produceToRecognizerTopic(report);
            } catch (Exception e) {
                logger.error("What we've got here is failure to recognize:", e);
            }
        });
    }

    private static void startTestConsumingRecognizer() {
        logger.info("Start test: consuming recognizer...");
        TestConsumer.consume(Config.KAFKA_SERVER, Config.RECOGNIZER_TOPIC_NAME);
    }

    private static void startTestConsumingOwncloud() {
        logger.info("Start test: consuming owncloud...");
        TestConsumer.consume(Config.KAFKA_SERVER, Config.OWNCLOUD_TOPIC_NAME);
    }

}
