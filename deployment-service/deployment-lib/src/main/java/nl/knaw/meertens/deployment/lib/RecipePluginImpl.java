package nl.knaw.meertens.deployment.lib;

import java.util.Stack;

public abstract class RecipePluginImpl implements RecipePlugin {
  @Override
  public abstract void init(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws RecipePluginException;

  @Override
  public abstract DeploymentResponse execute() throws RecipePluginException;

  @Override
  public abstract DeploymentResponse getStatus() throws RecipePluginException;

  @Override
  public void cleanup() {

  }
}
