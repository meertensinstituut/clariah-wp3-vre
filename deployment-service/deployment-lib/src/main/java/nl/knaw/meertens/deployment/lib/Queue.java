/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import java.util.LinkedHashMap;
import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
//import javax.servlet.ServletContextEvent;
//import javax.servlet.ServletContextiLstener;
import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.JSONObject;

/**
 *
 * @author vic
 */
public class Queue {
    
    protected static LinkedHashMap<String, RecipePlugin> executed = new LinkedHashMap<String, RecipePlugin>() 
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                String queueLength = null;
                try {
                    System.out.println("creating queue");
                    DeploymentLib dplib = new DeploymentLib();
                    queueLength = dplib.getQueueLength();
                    System.out.println("created queue");
                } catch (ConfigurationException ex) {
                    System.out.println("failure creating queue");
                    Logger.getLogger(Queue.class.getName()).log(Level.SEVERE, null, ex);
                }
                return size() > (queueLength!=null?Integer.parseInt(queueLength):100);
            }
        };
    
    public JSONObject push(String key, RecipePlugin plugin) {
        JSONObject json = new JSONObject();
        json.put("id", key);
        
        if (!(key == null || plugin == null || executed == null) && !(executed.containsKey(key))) {
            executed.put(key, plugin);
            new Thread() {
                @Override
                public void run() {
                    plugin.execute(key);
                }
            }.start();        
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
    
//    public void cleanFinishedTask() {
//        executed.entrySet().forEach(entry -> {
//            RecipePlugin value = entry.getValue();
//            if (value.finished()) {
//                executed.remove(entry.getKey());
//            }
//        });
//    }
    
    public void removeTask(String key) {
        executed.remove(key);
    }


}
