package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.example.TestConsumer;
import nl.knaw.meertens.clariah.vre.recognizer.example.TestProducer;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsResult;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.OwncloudKafkaDTO;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.RecognizerKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OWNCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.RECOGNIZER_TOPIC_NAME;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

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
