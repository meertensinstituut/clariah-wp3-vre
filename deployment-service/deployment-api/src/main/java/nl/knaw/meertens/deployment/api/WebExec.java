package nl.knaw.meertens.deployment.api;

import net.sf.saxon.s9api.SaxonApiException;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.FoliaEditor;
import nl.knaw.meertens.deployment.lib.Queue;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.commons.configuration.ConfigurationException;
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

/**
 * exposed at "exec" path
 */
@Path("/exec")
public class WebExec {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

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
   *
   * @return json as String
   * @throws java.io.IOException
   *
   * @throws java.net.MalformedURLException
   *
   * @throws org.apache.commons.configuration.ConfigurationException
   *
   * @throws org.json.simple.parser.ParseException
   *
   * @throws org.jdom2.JDOMException
   *
   * @throws SaxonApiException
   *
   * @throws com.mashape.unirest.http.exceptions.UnirestException
   *
   */
  @GET
  @Path("/test")
  @Produces(MediaType.APPLICATION_JSON)
  public Response test() throws Exception {
    String projectName = "wd12345";
    String serviceId = "FOLIAEDITOR";
    DeploymentLib dplib = new DeploymentLib();
    Service service = dplib.getServiceByName(serviceId);

    JSONObject json = new JSONObject();
    FoliaEditor fe = new FoliaEditor();
    fe.init(projectName, service);

    // fe.uploadFile(projectName, "example.xml", "eng", "template", "author");
    fe.runProject(projectName);
    // clam.init(projectName, service);
    // json = clam.downloadProject(projectName);
    json.put("test", "test");
    return Response.ok().build();
    // return json.toString();
  }

  /**
   * @param pid
   * Project id also known as project name, key and working directory
   * @param service
   * Service record in service registry
   * @return res as Response
   * @throws IOException
   * IO Exception
   * @throws JDOMException
   * Invalid DOM
   * @throws MalformedURLException
   *
   *     TODO: check directory? 404 if no directory else 200
   */
  @GET
  @Path("/{service}/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response poll(
    @PathParam("id") String pid,
    @PathParam("service") String service
  ) throws RecipePluginException {

    // Create instance of the queue
    Queue queue = new Queue();
    // JSONObject status to return
    JSONObject status = new JSONObject();
    // Get plugin from the queue
    RecipePlugin plugin = queue.getPlugin(pid);

    Response res;
    if (plugin != null) {
      status = plugin.getStatus();

      Boolean finished = (Boolean) status.get("finished");

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
   * @param wd
   * Working directory also knowns as key, project id and project name
   * @param service
   * Service record in service registry
   *
   * @throws ConfigurationException
   *
   * @throws ClassNotFoundException
   *
   * @throws InstantiationException
   *
   * @throws IllegalAccessException
   *
   * @throws InterruptedException
   *
   * @throws ExecutionException
   *
   * @throws IOException
   *
   * @throws MalformedURLException
   *
   * @throws ParseException
   *
   * @throws org.jdom2.JDOMException
   *
   */
  @PUT
  @Path("/{service}/{wd}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response exec(
    @PathParam("wd") String wd,
    @PathParam("service") String service
  )
      throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException,
      IOException, MalformedURLException, RecipePluginException {
    Response res;
    JSONObject json = new JSONObject();
    DeploymentLib dplib = new DeploymentLib();
    File configFile = dplib.getConfigFile();
    dplib.parseConfig(configFile);

    if (dplib.serviceExists(service)) {

      logger.info("Service is valid!");

      logger.info("Getting service from service registry!");
      Service serviceObj = dplib.getServiceByName(service);
      logger.info("Got service!");

      logger.info("Getting recipe!");
      RecipePlugin plugin;
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
      logger.info(String.format("### dbConfig in xml as string: %s ###", serviceObj.getServiceSymantics()));
      String dbConfig = serviceObj.getServiceSymantics();
      boolean userConfigIsValid = this.checkUserConfig(
        new DeploymentLib().parseSymantics(dbConfig),
        new DeploymentLib().parseUserConfig(wd)
      );
      if (!userConfigIsValid) {
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
        json = queue.push(wd, plugin);

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

  /**
   * TODO: it SHOULD check
   */
  private boolean checkUserConfig(JSONObject dbSymantics, JSONObject userSymantics) {
    logger.info(String.format("### userConfig: %s ###", userSymantics.toJSONString()));
    logger.info(String.format("### dbConfig: %s ###", dbSymantics.toJSONString()));
    return true;
  }
}
