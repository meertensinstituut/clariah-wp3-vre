package nl.knaw.meertens.deployment.lib;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

public interface HandlerPlugin {
  void init();

  ObjectNode handle(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
      IllegalAccessException, HandlerPluginException, RecipePluginException;

  void cleanup();
}
