package nl.knaw.meertens.deployment.lib;

public interface HandlerPlugin {
  void init();

  String handle(String serviceName, String serviceLocation);
}
