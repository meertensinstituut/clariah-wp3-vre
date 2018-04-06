package nl.knaw.meertens.deployment.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import nl.knaw.meertens.deployment.lib.Clam;
import static nl.knaw.meertens.deployment.lib.Clam.readStringFromURL;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.Queue;
import nl.knaw.meertens.deployment.lib.Service;

import org.apache.commons.configuration.ConfigurationException;

import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.mpi.tla.util.Saxon;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * exposed at "exec" path
 */
@Path("/exec")
public class WebExec {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/test/{projectName}")
    public String test(
            @PathParam("projectName") String projectName
    ) throws IOException, MalformedURLException, JDOMException, SaxonApiException, FileNotFoundException, ParseException {
        Clam clam = new Clam();
        JSONObject jsonResult = new JSONObject();
        
        jsonResult = clam.prepareProject(projectName);
        return jsonResult.toString();
    }
    
    
    

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "json".
     *
     * @return String that will be returned as a json response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllCommands() {
        JSONArray json = new JSONArray();
        
        json.add("commands list: \n");
        json.add("1. List all commands \n");
        json.add("http://localhost/deployment-service/a/exec/ \n");
        json.add("2. Execute a service (run a task) \n");
        json.add("http://localhost/deployment-service/a/exec/UCTO/wd1234/params \n");
        json.add("3. Poll a task \n");
        json.add("http://localhost/deployment-service/a/exec/task/<pid> \n");
        return json.toString();
    }
    
    @GET
    @Path("/task/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response poll(
            @PathParam("id") String pid) throws IOException, JDOMException, MalformedURLException {
        
        Queue queue = new Queue();
        JSONObject status = new JSONObject();
        RecipePlugin plugin = queue.getPlugin(pid);
        Response res;
        
        if (plugin!=null) {
            if (!plugin.finished()) {
                status = plugin.getStatus(pid);
                res = Response.status(202).build();
            } else {
                status.put("status", 200);
                status.put("message", "Task finished, clean up the queue.");
                res = Response.ok(status.toString(), MediaType.APPLICATION_JSON).build();
                /* FIX TODO: runProject throws an error but runs the project. */
                plugin.execute(pid);
            }
        } else {
            res = Response.status(404).build();
            status.put("status", 404);
            status.put("message", "Task not found");
        }
        status.put("id", pid);
        
        
        
        new Thread() {
            @Override
            public void run() {
                queue.cleanFinishedTask();
            }
        }.start(); 
        
        return res;
        
    }
            
    @GET
    @Path("/{service}/{wd}/{params: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exec(
            @PathParam("wd") String wd, 
            @PathParam("service") String service, 
            @PathParam("params") String params) throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException, ExecutionException, IOException, MalformedURLException, ParseException {
        Response res;
        JSONObject json = new JSONObject();
        DeploymentLib dplib = new DeploymentLib();
        if (dplib.serviceExists(service)) {
            File configFile = dplib.getConfigFile();
            dplib.parseConfig(configFile);
            String fullPath = dplib.getWd() + "/" + wd;
            Service serviceObj = dplib.getServiceByName(service);

            // ### Load plugin; for demo purpose, fall back to Clam ###
            // the class name should be read from a configuration file or db
//            String className = "nl.knaw.meertens.deployment.lib.Clam";// + serviceObj.getName();
            RecipePlugin plugin;
            String className = serviceObj.getRecipe();
            if ("nl.knaw.meertens.deployment.lib.Test".equals(className)) {
                Class<?> loadedClass = Class.forName(className);
                Class<? extends RecipePlugin> pluginClass = loadedClass.asSubclass(RecipePlugin.class);
                plugin = pluginClass.newInstance();
            } else {
                Class<?> loadedClass = Class.forName("nl.knaw.meertens.deployment.lib.Clam");
                Class<? extends RecipePlugin> pluginClass = loadedClass.asSubclass(RecipePlugin.class);
                plugin = pluginClass.newInstance();
            }
            
            // ### end of plugin file ###
            
            Queue queue = new Queue();
            
            json = queue.push(wd, plugin);
          
        } else {
            json.put("status", "invalid service");
        }
        res = Response.ok(json.toString(),MediaType.APPLICATION_JSON).build();
        return res;
    }
    
    public Thread getThreadById(long threadId) {
        Thread currentThread = Thread.currentThread();
        ThreadGroup threadGroup = getRootThreadGroup(currentThread);
        int allActiveThreads = threadGroup.activeCount();
        Thread[] allThreads = new Thread[allActiveThreads];
        threadGroup.enumerate(allThreads);

        for (int i = 0; i < allThreads.length; i++) {
            Thread thread = allThreads[i];
            long id = thread.getId();
            if (id == threadId) {
                return thread;
            }
        }
        return null;
    }

    private static ThreadGroup getRootThreadGroup(Thread thread) {
        ThreadGroup rootGroup = thread.getThreadGroup();
        while (true) {
            ThreadGroup parentGroup = rootGroup.getParent();
            if (parentGroup == null) {
                break;
            }
            rootGroup = parentGroup;
        }
        return rootGroup;
    }

}
