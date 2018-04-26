package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.example.TestConsumer;
import nl.knaw.meertens.clariah.vre.example.TestProducer;
import nl.knaw.meertens.clariah.vre.fits.FitsResult;
import nl.knaw.meertens.clariah.vre.fits.FitsService;
import nl.knaw.meertens.clariah.vre.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.kafka.OwncloudKafkaDTO;
import nl.knaw.meertens.clariah.vre.kafka.RecognizerKafkaProducer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.ACTIONS_TO_PERSIST;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_FILES_ROOT;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_TABLE;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OWNCLOUD_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OWNCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.RECOGNIZER_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.CREATE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.DELETE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.RENAME;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.UPDATE;

public class App {

    public static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private static final FitsService fitsService = new FitsService(FITS_URL, FITS_FILES_ROOT);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final KafkaConsumerService owncloudConsumerService = new KafkaConsumerService(KAFKA_SERVER, OWNCLOUD_TOPIC_NAME, OWNCLOUD_GROUP_NAME);
    private static final KafkaProducerService kafkaProducer = new KafkaProducerService(new RecognizerKafkaProducer(KAFKA_SERVER), RECOGNIZER_TOPIC_NAME);
    private static final ObjectsRepositoryService objectsRepository = new ObjectsRepositoryService(OBJECTS_DB_URL, OBJECTS_DB_KEY, OBJECT_TABLE);

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
        TestProducer.produce(KAFKA_SERVER, OWNCLOUD_TOPIC_NAME);
    }

    private static void startConsuming() {
        logger.info("Start consuming owncloud...");
        consumeOwncloud();
    }

    private static void consumeOwncloud() {
        owncloudConsumerService.consumeWith((String json) -> {
            try {
                OwncloudKafkaDTO msg = objectMapper.readValue(json, OwncloudKafkaDTO.class);
                FileAction action = FileAction.from(msg.action);
                if (!ACTIONS_TO_PERSIST.contains(msg.action)) {
                    logger.info(format("Ignored message about file [%s] with action [%s]", msg.path, msg.action));
                    return;
                }
                if (StringUtils.isBlank(msg.path)) {
                    throw new IllegalArgumentException(String.format("No field path in owncloud msg [%s]", json));
                }
                Report report = new Report();
                report.setAction(msg.action);
                report.setUser(msg.user);
                report.setPath(Paths.get(msg.path).normalize().toString());
                report.setOldPath(isNull(msg.oldPath) ? null : Paths.get(msg.oldPath).normalize().toString());
                if (action.equals(CREATE)) {
                    checkFileType(msg, report);
                    report.setObjectId(objectsRepository.create(report));
                } else if (action.equals(UPDATE)) { // update
                    checkFileType(msg, report);
                    report.setObjectId(objectsRepository.update(report));
                } else if (action.equals(RENAME)) {
                    report.setObjectId(objectsRepository.updatePath(report.getOldPath(), report.getPath()));
                } else if (action.equals(DELETE)) {
                    report.setObjectId(objectsRepository.delete(report.getPath()));
                }
                kafkaProducer.produceToRecognizerTopic(report);
            } catch (Exception e) {
                logger.error("What we've got here is failure to recognize:", e);
            }
        });
    }

    private static void checkFileType(OwncloudKafkaDTO msg, Report report) throws IOException, JAXBException {
        FitsResult fitsResult = fitsService.checkFile(msg.path);
        report.setXml(fitsResult.getXml());
        report.setFits(fitsResult.getFits());
    }

    private static void startTestConsumingRecognizer() {
        logger.info("Start test: consuming recognizer...");
        TestConsumer.consume(KAFKA_SERVER, RECOGNIZER_TOPIC_NAME);
    }

    private static void startTestConsumingOwncloud() {
        logger.info("Start test: consuming owncloud...");
        TestConsumer.consume(KAFKA_SERVER, OWNCLOUD_TOPIC_NAME);
    }

}
