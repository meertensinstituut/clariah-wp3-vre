package nl.knaw.meertens.clariah.vre.recognizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private static final RecognizerService recognizerService = new RecognizerService();

  public static void main(String[] args) {
    logger.info("Start consuming nextcloud topic...");
    recognizerService.consumeOwncloud();
  }

}
