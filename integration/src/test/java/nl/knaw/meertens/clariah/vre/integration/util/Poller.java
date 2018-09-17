package nl.knaw.meertens.clariah.vre.integration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

public class Poller {

    private static Logger logger = LoggerFactory.getLogger(Poller.class);

    /**
     * @param check      Procedure which check for condition when polling can be stopped
     * @param pollPeriod period to poll in seconds
     */
    public static void pollUntilSuccess(Procedure check, int pollPeriod) {
        int count = 0;
        AssertionError checkError;
        while (true) {
            checkError = null;
            try {
                check.execute();
                waitSeconds(1);
            } catch (AssertionError e) {
                checkError = e;
            }
            if (isNull(checkError)) {
                logger.info(String.format("Finished polling succesfully after %ds", pollPeriod - count));
                break;
            }
            boolean cannotPollAgain = pollPeriod < count;
            if (cannotPollAgain) {
                throw checkError;
            }
            logger.info("Poll");
            count++;
        }
    }

    private static void waitSeconds(int timeout) {
        try {
            TimeUnit.SECONDS.sleep(timeout);
        } catch (InterruptedException e) {
            logger.error("Polling was interrupted", e);
        }
    }

}
