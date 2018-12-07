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
  // TODO: use a pojo to define the fields that WebExec needs to correctly handle the execute response
  JSONObject execute() throws RecipePluginException;

  /**
   * Get status of deployment
   */
  // TODO: use a pojo to define the fields that WebExec needs to correctly handle the status response
  JSONObject getStatus() throws RecipePluginException;

}
