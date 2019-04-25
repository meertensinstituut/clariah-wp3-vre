package nl.knaw.meertens.deployment.lib.handler;

import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http implements HandlerPlugin {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init() {
    logger.info("initialized!");
  }

  @Override
  public String handle(String serviceName, String serviceLocation) {
    String httpLoc = "http:" + serviceLocation;
    return httpLoc;
    // DeploymentLib.invokeService(serviceName, httpLoc);
  }
}

