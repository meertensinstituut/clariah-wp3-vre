package nl.knaw.meertens.deployment.lib;

public interface RecipePlugin {

  /**
   * Set working directory and service
   */
  void init(String workDir, Service service) throws RecipePluginException;

  /**
   * Execute recipe and start deployment
   */
  DeploymentResponse execute() throws RecipePluginException;

  /**
   * Get status of deployment
   */
  DeploymentResponse getStatus() throws RecipePluginException;

}
