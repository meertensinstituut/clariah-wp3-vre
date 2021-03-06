package nl.knaw.meertens.deployment.lib;

import java.util.Stack;

public abstract class RecipePluginImpl implements RecipePlugin {

  private Stack<HandlerPlugin> handlers;

  @Override
  public void init(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws RecipePluginException {
    this.handlers = handlers;
  }

  @Override
  public abstract DeploymentResponse execute() throws RecipePluginException;

  @Override
  public abstract DeploymentResponse getStatus() throws RecipePluginException;

  @Override
  public void cleanup() {
    DeploymentLib.invokeHandlerCleanup(handlers);
  }
}
