package nl.knaw.meertens.deployment.lib;

import org.json.simple.JSONObject;

public class Demo implements RecipePlugin {

  @Override
  public void init(String workDir, Service service) throws RecipePluginException {

  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    return null;
  }

  @Override
  public JSONObject getStatus() throws RecipePluginException {
    return null;
  }
}
