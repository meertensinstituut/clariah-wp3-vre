package nl.knaw.meertens.deployment.lib;

import org.junit.After;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractDeploymentTest {

  private static Logger logger = LoggerFactory.getLogger(AbstractDeploymentTest.class);

  private static boolean isSetUp;

  private static ClientAndServer mockServer;

  protected static Integer mockPort = 1080;

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

  public static ClientAndServer getMockServer() {
    return mockServer;
  }
}
