package nl.knaw.meertens.deployment.lib;

import org.json.simple.JSONObject;

public interface RecipePlugin {

  /**
   * Configure workDir and service params of new deployment
   */
  void init(String workDir, Service service) throws RecipePluginException;

  /**
   * Execute recipe and start deployment
   * TODO: replace JSONObject with pojo
   */
  JSONObject execute() throws RecipePluginException;

  /**
   * Get status of deployment
   * TODO: replace JSONObject with pojo
   */
  JSONObject getStatus() throws RecipePluginException;

}
