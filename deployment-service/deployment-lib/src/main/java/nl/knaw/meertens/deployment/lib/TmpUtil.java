package nl.knaw.meertens.deployment.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.JSONObject;

import java.io.IOException;

public class TmpUtil {
  private static ObjectMapper jsonMapper = new ObjectMapper();

  public static ObjectNode readTree(JSONObject serviceSemantics) throws RecipePluginException {
    try {
      return (ObjectNode) jsonMapper.readTree(serviceSemantics.toJSONString());
    } catch (IOException e) {
      throw  new RecipePluginException("could not read tree", e);
    }
  }

  public static JSONObject readTree(ObjectNode serviceSemantics) throws RecipePluginException {
    return jsonMapper.convertValue(serviceSemantics, JSONObject.class);
  }


}
