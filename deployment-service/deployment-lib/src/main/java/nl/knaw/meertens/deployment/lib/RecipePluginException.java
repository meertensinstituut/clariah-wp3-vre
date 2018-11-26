package nl.knaw.meertens.deployment.lib;

public class RecipePluginException extends Exception {
  public RecipePluginException(String message) {
    super(message);
  }

  public RecipePluginException(String message, Exception exception) {
    super(message, exception);
  }
}
