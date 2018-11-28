package nl.knaw.meertens.deployment.lib;

import org.json.simple.JSONObject;

public interface RecipePlugin {

  /**
   * @param workDir Project name also known as key, project id and working directory
   * @param service Service record in the service registry
   *
   * @throws RecipePluginException exception
   */
  void init(String workDir, Service service) throws RecipePluginException;

  /**
   * @return JSONObject result
   * @throws RecipePluginException exception
   */
  JSONObject execute() throws RecipePluginException;

  /**
   * @return JSONObject result
   * @throws RecipePluginException exception
   */
  JSONObject getStatus() throws RecipePluginException;

}
