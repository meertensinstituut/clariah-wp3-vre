package nl.knaw.meertens.deployment.lib.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

public class Http implements HandlerPlugin {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init() {
    logger.info("initialized!");
  }

  @Override
  public ObjectNode handle(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
      IllegalAccessException, HandlerPluginException, RecipePluginException {
    handlers.push(this);
    String httpLoc = "http:" + serviceLocation;
    logger.info(String.format("Final http url is [%s]", httpLoc));
    return DeploymentLib.invokeService(workDir, service, httpLoc, handlers);
  }

  @Override
  public void cleanup() {
    logger.info(String.format("HTTP handler does not need cleanup"));
  }

}

