package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Initialize vre:
 *  - add one file to owncloud
 *  - initialize kafka queues
 *  - wait for registry to have processed first file
 *  - wait for switchboard to have started
 */
public class Initializer extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(UploadingNewFileTest.class);

    private static final int WAITING_PERIOD = 120; // seconds

    @Test
    public void init() throws Exception {
        logger.info("Initialize VRE Integration");
        uploadTestFile();
        TimeUnit.SECONDS.sleep(2);
        KafkaConsumerService initConsumer = new KafkaConsumerService(
                KAFKA_ENDPOINT, RECOGNIZER_TOPIC_NAME, getRandomGroupName()
        );
        initConsumer.subscribe();
        initConsumer.pollOnce();

        logger.info("Wait 15 seconds for registry to process...");
        TimeUnit.SECONDS.sleep(15);

        logger.info("Poll " + WAITING_PERIOD + "s for services to start...");
        waitUntilSwitchboardIsUp();

        logger.info("Finished initialisation of VRE Integration");
    }

    private void waitUntilSwitchboardIsUp() throws UnirestException, InterruptedException {
        GetRequest getHealthRequest = Unirest.get(SWITCHBOARD_ENDPOINT + "/health");
        HttpResponse<String> response;
        int waited = 0;
        do {
            TimeUnit.SECONDS.sleep(1);
            waited++;
            response = getHealthRequest.asString();
            logger.info("Poll switchboard...");
        } while(response.getStatus() != 200 && waited < WAITING_PERIOD);
        assertThat(waited).isLessThan(WAITING_PERIOD);
        logger.info("Switchboard is up");
    }

}
