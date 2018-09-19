package nl.knaw.meertens.clariah.vre.integration;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Abstract class containing environment variables
 * and test util methods.
 */
public abstract class AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(UploadingNewFileTest.class);
    final static int maxPollPeriod = 20;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            logger.info(String.format("Starting test [%s]", description.getMethodName()));
        }
    };

    static String getRandomGroupName() {
        return "vre_integration_group" + UUID.randomUUID();
    }

}
