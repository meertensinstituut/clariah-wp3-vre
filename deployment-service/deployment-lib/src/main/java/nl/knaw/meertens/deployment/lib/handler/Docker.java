package nl.knaw.meertens.deployment.lib.handler;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Docker implements HandlerPlugin {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init() {
    logger.info("initialized!");
  }

  @Override
  public Boolean handle(String serviceName, String serviceLocation) {
    // docker handler
    Pattern p = Pattern("([^/]+)/([^/]+)/(([^/]+)/(.*)").compile();
    Matcher m = p.matcher(loc);
    String dockerRepo = m.group(1); // vre-repository
    String dockerImg = m.group(2); // lamachine
    String dockerTag = m.group(3); // tag-1.0
    String remainder = m.group(4); // nl.knaw.meertens.deployment.lib.handler.http://{docker-container-ip}/frog

    // do all the docker magic
    // which results in the following variables to be known
    // - docker-container-id
    // - docker-container-ip = 192.3.4.5

    if (remainder.trim().length() > 0) {
      // replace variables
      String loc = remainder.replaceAll("{docker-container-ip}",
          docker - container - ip); // nl.knaw.meertens.deployment.lib.handler.http://192.3.4.5/frog
      loc = loc.replaceAll("{docker-container-id}", docker - container - id);
      // invoke next handler
      DeploymentLib.invokeHandler(serviceName, loc);

      // do all the docker shutdown magic
    }
    throw new DeploymentException("docker handler can't be used on its own!");
  }
}

