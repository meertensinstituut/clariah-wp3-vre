package nl.knaw.meertens.clariah.vre.integration.util;

import nl.knaw.meertens.clariah.vre.integration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.integration.Config.MAX_POLLING_PERIOD;

public class Poller {

    private static Logger logger = LoggerFactory.getLogger(Poller.class);

    public static void awaitAndGet(Procedure check) {
        awaitAndGet(() -> {check.execute(); return null;});
    }

    public static <T> T awaitAndGet(Supplier<T> check) {
        return awaitAndGetUntil(check, Config.MAX_POLLING_PERIOD);
    }

    private static <T> T awaitAndGetUntil(Supplier<T> check, int maxPolled) {
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
                logger.info(String.format("Polled %ds", polled));
                break;
            }
            if (polled > maxPolled) {
                throw new AssertionError("Timed out with assertion error", checkError);
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
