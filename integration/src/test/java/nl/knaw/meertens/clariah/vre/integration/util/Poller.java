package nl.knaw.meertens.clariah.vre.integration.util;

import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

public class Poller {

    private static Logger logger = LoggerFactory.getLogger(Poller.class);

    /**
     * @param check      Procedure which check for condition when polling can be stopped
     * @param pollPeriod period to poll in seconds
     */
    public static void pollUntil(Procedure check, int pollPeriod) {
        int count = 0;
        AssertionError checkError;
        while (true) {
            checkError = null;
            try {
                check.execute();
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
    }

    /**
     * @param check      Procedure which check for condition when polling can be stopped
     * @param pollPeriod period to poll in seconds
     */
    public static <T> T pollUntil(Supplier<T> check, int pollPeriod) {
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
