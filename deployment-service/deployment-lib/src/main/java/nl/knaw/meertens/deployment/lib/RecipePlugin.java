package nl.knaw.meertens.deployment.lib;

import org.json.simple.JSONObject;

public interface RecipePlugin {

  /**
   * Set working directory and service
   */
  void init(String workDir, Service service) throws RecipePluginException;

  /**
   * Execute recipe and start deployment
   */
  DeploymentStatus execute() throws RecipePluginException;

  /**
   * Get status of deployment
   */
  DeploymentStatus getStatus() throws RecipePluginException;

}
