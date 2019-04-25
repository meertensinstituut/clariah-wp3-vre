package nl.knaw.meertens.deployment.lib;

import java.lang.reflect.InvocationTargetException;

public interface HandlerPlugin {
  void init();

  String handle(String serviceName, String serviceLocation)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
      IllegalAccessException, HandlerPluginException;
}
