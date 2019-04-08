package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import nl.knaw.meertens.clariah.vre.integration.util.FileUtils;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.integration.util.Poller;
import org.assertj.core.api.AssertionsForClassTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Initialize vre components
 */
public class Initializer {

  private Logger logger = LoggerFactory.getLogger(Initializer.class);

  private static final int MAX_WAITING_PERIOD = 300; // seconds

  String onIntegration = "It is not easy to describe lucidly " +
    "in short notes to a poem the various approaches to a fortified castle.";

  public void init() {
    logger.info("Initialize VRE components");

    awaitNextcloud();
    awaitRecognizer();
    awaitSwitchboard();
    awaitLamachine();

    logger.info("Finished initialisation");
  }

  /**
   * Upload a file, wait for registry to have processed it
   */
  private void awaitNextcloud() {
    try {
      String testFilename = FileUtils.uploadTestFile(onIntegration);
      Poller.awaitAndGet(() -> FileUtils.fileHasContent(testFilename, onIntegration));
    } catch (UnirestException e) {
      logger.error("Could not upload a test file", e);
    }
    logger.info("Wait 15 seconds for registry to process...");
    sleepSeconds(15);
  }

  /**
   * Start the kafka consumer
   */
  private void awaitRecognizer() {
    KafkaConsumerService initConsumer = new KafkaConsumerService(
      Config.KAFKA_ENDPOINT, Config.RECOGNIZER_TOPIC_NAME, "vre_integration_group" + UUID.randomUUID()
    );
    initConsumer.subscribe();
    initConsumer.pollOnce();
  }

  /**
   * Check switchboard's /health returns 200
   */
  private void awaitSwitchboard() {
    GetRequest getHealthRequest = Unirest.get(Config.SWITCHBOARD_ENDPOINT + "/health");
    HttpResponse<String> response;
    int status = 0;
    int waited = 0;
    do {
      sleepSeconds(1);
      waited++;
      try {
        response = getHealthRequest.asString();
        status = response.getStatus();
      } catch (UnirestException ignored) {
      }
      logger.info((MAX_WAITING_PERIOD - waited) + " Switchboard not up yet...");
    } while (status != 200 && waited < MAX_WAITING_PERIOD);
    AssertionsForClassTypes.assertThat(waited).isLessThan(MAX_WAITING_PERIOD);
    logger.info("Switchboard is up");
  }

  private void sleepSeconds(int timeout) {
    try {
      TimeUnit.SECONDS.sleep(timeout);
    } catch (InterruptedException e) {
      logger.error("Could not sleepSeconds");
    }
  }


  /**
   * Check switchboard's /health returns 200
   */
  private void awaitLamachine() {
    GetRequest getHealthRequest = Unirest.get(Config.LAMACHINE_ENDPOINT);
    HttpResponse<String> response;
    int status = 0;
    int waited = 0;
    do {
      sleepSeconds(1);
      waited++;
      try {
        response = getHealthRequest.asString();
        status = response.getStatus();
      } catch (UnirestException ignored) {
      }
      logger.info(String.format("[%d] Lamachine [%s] not up yet...; status is [%d]", MAX_WAITING_PERIOD - waited, Config.LAMACHINE_ENDPOINT,status));
    } while (status != 200 && waited < MAX_WAITING_PERIOD);
    AssertionsForClassTypes.assertThat(waited).isLessThan(MAX_WAITING_PERIOD);
    logger.info("Lamachine is up");
  }

}
