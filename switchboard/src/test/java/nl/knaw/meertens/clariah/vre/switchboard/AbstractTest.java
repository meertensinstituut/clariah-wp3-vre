package nl.knaw.meertens.clariah.vre.switchboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public abstract class AbstractTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            logger.info(String.format("Starting test [%s]", description.getMethodName()));
        }
    };

}
