package nl.knaw.meertens.clariah.vre.recognizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

  private final static Logger logger = LoggerFactory.getLogger(App.class.getName());

  private static final RecognizerService recognizerService = new RecognizerService();

  public static void main(String[] args) {

    var message = "logtest5";
    logger.trace(message);
    logger.debug(message);
    logger.info(message);
    logger.warn(message);
    logger.error(message);

    startConsuming();
  }

  private static void startConsuming() {
    logger.info("Start consuming nextcloud topic...");
    recognizerService.consumeOwncloud();
  }

}
