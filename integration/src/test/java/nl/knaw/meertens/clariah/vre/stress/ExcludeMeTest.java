package nl.knaw.meertens.clariah.vre.stress;

import nl.knaw.meertens.clariah.vre.integration.AbstractIntegrationTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ExcludeMeTest extends AbstractIntegrationTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void excludeMe() {
    assertThat(false).isTrue();
  }

}
