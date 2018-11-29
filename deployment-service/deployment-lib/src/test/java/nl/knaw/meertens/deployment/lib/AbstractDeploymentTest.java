package nl.knaw.meertens.deployment.lib;

import org.junit.After;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractDeploymentTest {

  private static Logger logger = LoggerFactory.getLogger(AbstractDeploymentTest.class);

  private static boolean isSetUp;
  static ClientAndServer mockServer;
  static Integer mockPort = 1080;
  static final String mockHostName = "http://localhost:" + mockPort;

  @BeforeClass
  public static void setUpClass() {
    if (isSetUp) {
      return;
    }
    logger.info("create mock server");
    mockServer = ClientAndServer.startClientAndServer(mockPort);
    isSetUp = true;
  }

  @After
  public void tearDown() {
    logger.info("reset abstract controller test setup");
    mockServer.reset();
  }
}
