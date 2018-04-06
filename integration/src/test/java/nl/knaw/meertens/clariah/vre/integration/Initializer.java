package nl.knaw.meertens.clariah.vre.integration;

import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Initialize vre:
 *  - wait for components to start
 *  - add one file to owncloud
 *  - initialize kafka ques
 */
public class Initializer extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(UploadingNewFileTest.class);

    @Test
    public void init() throws Exception {
        logger.info("Initialize VRE Integration");
        uploadTestFile();
        TimeUnit.SECONDS.sleep(2);
        KafkaConsumerService initConsumer = new KafkaConsumerService(
                KAFKA_ENDPOINT, RECOGNIZER_TOPIC_NAME, getRandomGroupName());
        initConsumer.subscribe();
        initConsumer.pollOnce();
        TimeUnit.SECONDS.sleep(2);
        logger.info("Wait 15 seconds for registry to process...");
        TimeUnit.SECONDS.sleep(15);
        logger.info("Finished initialisation of VRE Integration");
    }

}
