package nl.knaw.meertens.deployment.lib;

import org.json.simple.JSONObject;

public interface RecipePlugin {

  /**
   * @throws RecipePluginException exception
   */
  void init(String wd, Service serviceObj) throws RecipePluginException;

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
