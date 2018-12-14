package nl.knaw.meertens.deployment.api;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.Queue;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.NOT_FOUND;

// TODO: extract all logic to services
@Path("/exec")
public class ExecController extends AbstractController {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * @param workDir Project id also known as project name, key and working directory
   * @param service Service record in service registry
   * @return res as Response
   */
  @GET
  @Path("/{service}/{workDir}")
  @Produces(APPLICATION_JSON)
  public Response poll(
    @PathParam("service") String service,
    @PathParam("workDir") String workDir
  ) {

    Queue queue = new Queue();
    DeploymentResponse result;
    RecipePlugin plugin = queue.getPlugin(workDir);

    if (isNull(plugin)) {
      return Response
        .status(NOT_FOUND.getStatus())
        .entity(NOT_FOUND.toDeploymentResponse().getBody().toString())
        .type(APPLICATION_JSON)
        .build();
    }

    try {
      result = plugin.getStatus();
    } catch (RecipePluginException ex) {
      String msg = format("Failed to get status of [%s]", workDir);
      return handleException(msg, ex);
    }

    boolean finished = result.getStatus().isFinished();

    if (finished) {
      return Response
        .ok(result.getBody().toString(), APPLICATION_JSON)
        .build();
    } else {
      return Response
        .status(202)
        .entity(result.getBody().toString())
        .type(APPLICATION_JSON)
        .build();
    }

  }

  /**
   * @param workDir     Working directory also knowns as key, project id and project name
   * @param serviceName Service record in service registry
   */
  @PUT
  @Path("/{service}/{workDir}")
  @Produces(APPLICATION_JSON)
  public Response exec(
    @PathParam("workDir") String workDir,
    @PathParam("service") String serviceName
  ) {
    try {
      logger.info("Get service");
      Service service = new DeploymentLib().getServiceByName(serviceName);
      if (isNull(service)) {
        String msg = "invalid service";
        return handleException(msg);
      }

      logger.info("Get recipe");
      RecipePlugin plugin;
      String className = service.getRecipe();

      logger.info("Loading plugin");
      Class<?> loadedClass = Class.forName(className);
      Class<? extends RecipePlugin> pluginClass = loadedClass.asSubclass(RecipePlugin.class);
      // TODO: replace with method that isn't deprecated:
      plugin = pluginClass.newInstance();
      plugin.init(workDir, service);

      logger.info("Check user config against service record");
      String dbConfig = service.getServiceSemantics();
      boolean userConfigIsValid = this.checkUserConfig(
        DeploymentLib.parseSemantics(dbConfig),
        DeploymentLib.parseUserConfig(workDir)
      );

      if (!userConfigIsValid) {
        String msg = "user config is invalid";
        JSONObject status = new JSONObject();
        status.put("finished", false);
        return handleException(msg, status);
      }

      Queue queue = new Queue();
      logger.info("Plugin invoked");
      JSONObject json = queue.push(workDir, plugin);
      return Response
        .ok(json.toJSONString(), APPLICATION_JSON)
        .build();

    } catch (IOException |
        ConfigurationException |
        InstantiationException |
        ClassNotFoundException |
        IllegalAccessException |
        RecipePluginException ex
    ) {
      return handleException(format("Could not deploy [%s][%s]", serviceName, workDir), ex);
    }
  }

  @DELETE
  @Path("/{service}/{workDir}")
  @Produces(APPLICATION_JSON)
  public Response delete(
    @PathParam("workDir") String workDir,
    @PathParam("service") String service
  ) {
    // TODO: Queues are created at three different places atm, shouldn't there be just one queue?
    Queue queue = new Queue();
    queue.removeTask(workDir);
    return Response
      .ok("deleted", APPLICATION_JSON)
      .build();
  }

  private boolean checkUserConfig(JSONObject dbSymantics, JSONObject userSymantics) {
    // TODO: check if user config if valid instead of returning true
    logger.info(format("userConfig: %s", userSymantics.toJSONString()));
    logger.info(format("dbConfig: %s", dbSymantics.toJSONString()));
    return true;
  }

}
