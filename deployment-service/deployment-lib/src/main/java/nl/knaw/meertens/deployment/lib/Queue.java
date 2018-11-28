/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.knaw.meertens.deployment.lib;

import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * @author vic
 */
public class Queue {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  protected static LinkedHashMap<String, RecipePlugin> executed = new LinkedHashMap<>() {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
      String queueLength = null;
      try {
        logger.info("creating queue");
        DeploymentLib dplib = new DeploymentLib();
        queueLength = SystemConf.queueLength;
        logger.info("created queue");
      } catch (ConfigurationException ex) {
        logger.error("failure creating queue", ex);
      }
      return size() > (queueLength != null ? Integer.parseInt(queueLength) : 100);
    }
  };

  public JSONObject push(String key, RecipePlugin plugin) {
    JSONObject json = new JSONObject();
    json.put("id", key);

    if (!(key == null || plugin == null || executed == null) && !(executed.containsKey(key))) {
      executed.put(key, plugin);
      new Thread(() -> {
        try {
          plugin.execute();
        } catch (RecipePluginException e) {
          logger.error("Could not execute plugin " + plugin);
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
      json.put("message", "Put to queue failed. Key or plugin or queue is null");

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
