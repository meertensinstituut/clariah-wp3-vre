package nl.knaw.meertens.clariah.vre.integration.util;

import nl.knaw.meertens.clariah.vre.integration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

public class Poller {

  private static Logger logger = LoggerFactory.getLogger(Poller.class);

  public static void awaitCheck(Procedure check) {
    awaitAndGet(() -> {
      check.execute();
      return null;
    });
  }

  public static void awaitCheckFor(Procedure check, Duration maxPolled) {
    awaitAndGetFor(() -> {
      check.execute();
      return null;
    }, maxPolled);
  }

  public static <T> T awaitAndGet(Supplier<T> check) {
    return awaitAndGetFor(check, Config.MAX_POLLING_PERIOD);
  }

  public static <T> T awaitAndGetFor(Supplier<T> check, Duration maxPolled) {
    T result = null;
    int polled = 0;
    AssertionError checkError;
    while (true) {
      checkError = null;
      try {
        result = check.get();
      } catch (AssertionError e) {
        checkError = e;
      }

      if (isNull(checkError)) {
        logger.info(String.format("Finished polling at %d seconds", polled));
        break;
      } else {
        logger.info(String.format("Polling for %d seconds", polled));
      }

      if (polled > maxPolled.toSeconds()) {
        throw new AssertionError(String.format("Timed out at [%d seconds] with assertion error", polled), checkError);
      }
      waitASecond();
      polled++;
    }
    return result;
  }

  private static void waitASecond() {
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      logger.error("Polling was interrupted", e);
    }
  }

}
