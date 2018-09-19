package nl.knaw.meertens.clariah.vre.integration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.integration.Config.MAX_POLLING_PERIOD;

public class Poller {

    private static Logger logger = LoggerFactory.getLogger(Poller.class);

    public static void pollAndAssert(Procedure check) {
        pollAndAssert(() -> {check.execute(); return null;});
    }

    public static <T> T pollAndAssert(Supplier<T> check) {
        return pollAndAssertUntil(check, MAX_POLLING_PERIOD);
    }

    private static <T> T pollAndAssertUntil(Supplier<T> check, int pollPeriod) {
        T result = null;
        int count = 0;
        AssertionError checkError;
        while (true) {
            checkError = null;
            try {
                result = check.get();
            } catch (AssertionError e) {
                checkError = e;
            }
            if (isNull(checkError)) {
                logger.info(String.format("Polled %ds", count));
                break;
            }
            if (pollPeriod < count) {
                throw new AssertionError("Poller timed out", checkError);
            }
            count++;
            waitASecond();
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
