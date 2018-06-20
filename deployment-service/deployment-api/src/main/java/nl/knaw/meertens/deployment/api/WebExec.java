package nl.knaw.meertens.deployment.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
import java.io.IOException;
//import java.net.HttpURLConnection;

import java.net.MalformedURLException;
import java.nio.file.Paths;
//import java.net.URL;
//import java.nio.file.Paths;

import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.sf.saxon.s9api.SaxonApiException;

//import net.sf.saxon.s9api.SaxonApiException;

import nl.knaw.meertens.deployment.lib.Clam;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.Queue;
import nl.knaw.meertens.deployment.lib.Service;

import org.apache.commons.configuration.ConfigurationException;

import nl.knaw.meertens.deployment.lib.RecipePlugin;
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * exposed at "exec" path
 */
@Path("/exec")
public class WebExec {
    /**
     * Displays the end points list
     * 
     * @return json as String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllCommands() {
        JSONArray json = new JSONArray();
        
        json.add("commands list: \n");
        json.add("1. List all commands \n");
        json.add("http://localhost/deployment-service/a/exec/ \n");
        json.add("2. Execute a service (run a task) \n");
        json.add("http://localhost/deployment-service/a/exec/<service>/<pid>/params \n");
        json.add("3. Poll a task \n");
        json.add("http://localhost/deployment-service/a/exec/<service>/<pid> \n");
        return json.toString();
    }

    /**
     * test
     * @return json as String
     * @throws java.io.IOException
     * @throws java.net.MalformedURLException
     * @throws org.apache.commons.configuration.ConfigurationException
     * @throws org.json.simple.parser.ParseException
     * @throws org.jdom2.JDOMException
     */
    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response test() throws IOException, MalformedURLException, ConfigurationException, ParseException, JDOMException {
        String projectName = "wd12345";
        String serviceId = "UCTO";
//        DeploymentLib dplib = new DeploymentLib();
//        Service service = dplib.getServiceByName(serviceId);

        JSONObject json = new JSONObject();
//        Clam clam = new Clam();
//        clam.init(projectName, service);
//        json = clam.downloadProject(projectName);
        json.put("test", "test");
        return Response.ok().build();
//        return json.toString();
    }
    
    /**
     * 
     * @param pid
     * @param service
     * @return
     * @throws IOException
     * @throws JDOMException
     * @throws MalformedURLException 
     * 
     * Poll project
     * 
     * TODO: check directory? 404 if no directory else 200
     * 
     */
    @GET
    @Path("/{service}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response poll(
            @PathParam("id") String pid, 
            @PathParam("service") String service
    ) throws IOException, JDOMException, MalformedURLException {
                
        // Create instance of the queue
        Queue queue = new Queue();
        // JSONObject status to return
        JSONObject status = new JSONObject();
        // Get plugin from the queue
        RecipePlugin plugin = queue.getPlugin(pid);
        
        Response res;
        if (plugin!=null) {
            status = plugin.getStatus(pid);
            
            Boolean finished = (Boolean)status.get("finished");

            if (finished) {
                res = Response.ok(status.toString(), MediaType.APPLICATION_JSON).build();
            } else {
                res = Response.status(202).entity(status.toString()).type(MediaType.APPLICATION_JSON).build();
            }
            return res;

        } else {
            status.put("status", 404);
            status.put("message", "Task not found");
            status.put("finished", false);
            res = Response.status(404).entity(status.toString()).type(MediaType.APPLICATION_JSON).build();
        }
        status.put("id", pid);
        
        return res;
        
    }
    
    /**
     * 
     * @param wd
     * @param service
     * @param params
     * @return
     * @throws ConfigurationException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     * @throws MalformedURLException
     * @throws ParseException 
     * 
     * 
     * Run project
     * @throws org.jdom2.JDOMException
     */
    @PUT
    @Path("/{service}/{wd}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exec(
            @PathParam("wd") String wd, 
            @PathParam("service") String service
            ) throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException, ExecutionException, IOException, MalformedURLException, ParseException, JDOMException, SaxonApiException {
        Response res;
        JSONObject json = new JSONObject();
        DeploymentLib dplib = new DeploymentLib();
        File configFile = dplib.getConfigFile();
        dplib.parseConfig(configFile);
        
        // init logger to file
        Logger logger = Logger.getLogger("DeploymentLogger");  
        FileHandler fh;  
        try {  
            // This block configure the logger with handler and formatter  
            fh = new FileHandler(Paths.get(dplib.getWd(), wd, "deployment.log").toString(), true);  
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {  
            e.printStackTrace();  
        }  
    
        if (dplib.serviceExists(service)) {
            
            logger.info("Service is valid!");

            logger.info("Getting service from service registry!");
            Service serviceObj = dplib.getServiceByName(service);
            logger.info("Got service!");

            RecipePlugin plugin;
            logger.info("Getting recipe!");
            String className = serviceObj.getRecipe();
            logger.info("Got recipe!");
            
            logger.info("Loading plugin!");
            Class<?> loadedClass = Class.forName(className);
            Class<? extends RecipePlugin> pluginClass = loadedClass.asSubclass(RecipePlugin.class);
            plugin = pluginClass.newInstance();
            plugin.init(wd, serviceObj);
            logger.info("plugin loaded!");
            
            // Check user config against service record
            // This happens before plugin is pushed to the queue
            logger.info("Checking user config against service record!");
            System.out.println(serviceObj.getServiceSymantics());
            if (!this.checkUserConfig(plugin.parseSymantics(service), plugin.parseUserConfig(service))) {
                // config is not fine, throw exception
                JSONObject status = new JSONObject(); 
                status.put("status", 500);
                status.put("message", "user config error according to registry!");
                status.put("finished", false);
                res = Response.status(500).entity(status.toString()).type(MediaType.APPLICATION_JSON).build();
                logger.info("Invalid user config file checked against registry!");
                return res;
            } else {
                // config is fine, push to queue
                logger.info("Valid user config found, invoking plugin!");
                Queue queue = new Queue();
                
                logger.info("Plugin invoked!");
                json = queue.push(wd, plugin, logger);
                
                res = Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
                
                return res;
            }
            
            
        } else {
            logger.info("Invalid service!");
            json.put("status", "invalid service");
            res = Response.status(500, "invalid service").build();
            return res;
        }

    }
    
    @DELETE
    @Path("/{service}/{wd}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
        @PathParam("wd") String wd, 
        @PathParam("service") String service
    ) {
        Queue queue = new Queue();
        queue.removeTask(wd);
        Response res = Response.ok("deleted", MediaType.APPLICATION_JSON).build();
        
        return res;
    }
    
//    public Thread getThreadById(long threadId) {
//        Thread currentThread = Thread.currentThread();
//        ThreadGroup threadGroup = getRootThreadGroup(currentThread);
//        int allActiveThreads = threadGroup.activeCount();
//        Thread[] allThreads = new Thread[allActiveThreads];
//        threadGroup.enumerate(allThreads);
//
//        for (int i = 0; i < allThreads.length; i++) {
//            Thread thread = allThreads[i];
//            long id = thread.getId();
//            if (id == threadId) {
//                return thread;
//            }
//        }
//        return null;
//    }
//
//    private static ThreadGroup getRootThreadGroup(Thread thread) {
//        ThreadGroup rootGroup = thread.getThreadGroup();
//        while (true) {
//            ThreadGroup parentGroup = rootGroup.getParent();
//            if (parentGroup == null) {
//                break;
//            }
//            rootGroup = parentGroup;
//        }
//        return rootGroup;
//    }

    private boolean checkUserConfig(JSONObject dbSymantics, JSONObject userSymantics) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return true;
    }
}
