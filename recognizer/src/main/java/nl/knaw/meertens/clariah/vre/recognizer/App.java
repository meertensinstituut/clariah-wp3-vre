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

    public static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final RecognizerService recognizerService = new RecognizerService();

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

    private static void startConsuming() {
        logger.info("Start consuming owncloud topic...");
        recognizerService.consumeOwncloud();
    }

    private static void startProducing() {
        logger.info("Produce test msg...");
        TestProducer.produce(KAFKA_SERVER, OWNCLOUD_TOPIC_NAME);
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
