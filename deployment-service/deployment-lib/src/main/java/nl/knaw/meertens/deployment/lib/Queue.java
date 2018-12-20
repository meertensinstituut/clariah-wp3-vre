package nl.knaw.meertens.deployment.lib;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class Queue {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private static JsonNodeFactory jsonFactory = new JsonNodeFactory(false);

  protected static LinkedHashMap<String, RecipePlugin> executed = new LinkedHashMap<>() {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
      String queueLength;
      logger.info("creating queue");
      queueLength = SystemConf.QUEUE_LENGTH;
      logger.info("created queue");
      return size() > (queueLength != null ? Integer.parseInt(queueLength) : 100);
    }
  };

  public ObjectNode push(String key, RecipePlugin plugin) {
    ObjectNode json = jsonFactory.objectNode();
    json.put("id", key);

    if (!(key == null || plugin == null || executed == null) && !(executed.containsKey(key))) {
      executed.put(key, plugin);
      new Thread(() -> {
        try {
          plugin.execute();
        } catch (RecipePluginException ex) {
          logger.error(String.format("Could not execute plugin [%s]", plugin), ex);
        }
      }).start();
      json.put("status", 202);
      json.put("message", "running");
      return json;
    } else if (executed.containsKey(key)) {
      json.put("status", 403);
      json.put("message", "Put to queue failed. Task is still running.");
    } else {
      json.put("status", 500);
      String msg = "Put to queue failed. Key or plugin or queue is null";
      json.put("message", msg);
      logger.error(msg);
    }
    return json;
  }

  public RecipePlugin getPlugin(String key) {

    if (executed.containsKey(key)) {
      return executed.get(key);
    } else {
      return null;
    }
  }

  public void removeTask(String key) {
    executed.remove(key);
  }

}
