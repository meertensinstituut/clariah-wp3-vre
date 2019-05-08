package nl.knaw.meertens.deployment.lib;

public class HandlerPluginException extends Exception {
  public HandlerPluginException(String message) {
    super(message);
  }

  public HandlerPluginException(String message, Exception exception) {
    super(message, exception);
  }

}
