package nl.knaw.meertens.deployment.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import nl.knaw.meertens.deployment.lib.Queue;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import nl.knaw.meertens.deployment.lib.recipe.FoliaEditor;
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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.NOT_FOUND;

// TODO: extract all logic to services
@Path("/exec")
public class ExecController extends AbstractController {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private static JsonNodeFactory jsonFactory = new JsonNodeFactory(false);

  @GET
  @Path("/test/{dockerRepo}/{dockerImage}/{dockerTag}")
  @Produces(APPLICATION_JSON)
  public Response test(
      @PathParam("dockerRepo") String dockerRepo,
      @PathParam("dockerImage") String dockerImage,
      @PathParam("dockerTag") String dockerTag
  ) throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, InstantiationException, IllegalAccessException, HandlerPluginException {

    String serviceName = "Docker";
    // String loc = "Docker:repo/image/tag/Http://{docker-container-ip}/orgpath";
    // String loc = "Docker:_/busybox/latest/Http://{docker-container-ip}/orgpath";
    String loc = String.format("Docker:%s/%s/%s/Http://{docker-container-ip}/", dockerRepo, dockerImage, dockerTag);
    logger.info(String.format("Given loc is: [%s]", loc));

    String serviceLocation = DeploymentLib.invokeHandler(serviceName, loc); // http://192.3.4.5/frog

    return Response
        .ok(serviceLocation, APPLICATION_JSON)
        .build();
  }

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
          .entity(NOT_FOUND.toResponse().getBody().toString())
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
      logger.info(String.format("Get service [%s]", serviceName));
      Service service = new DeploymentLib().getServiceByName(serviceName);
      if (isNull(service)) {
        String msg = "invalid service";
        return handleException(msg);
      }

      // get semantic info serviceName
      // get the serviceLocation
      String loc = DeploymentLib.getServiceLocationFromJson(DeploymentLib.parseSemantics(service
          .getServiceSemantics())); // "nl.knaw.meertens.deployment.lib.handler.docker:vre-repository/lamachine/tag-1
      // .0/http://{docker-container-ip}/frog";
      logger.info(String.format("Has loc [%s]", loc));

      String serviceLocation;

      if (loc.equals("") || loc.toLowerCase().equals("none")) {
        logger.info("loc is not necessary, set serviceLocation to null");
        serviceLocation = null;
      } else {
        try {
          logger.info("loc is valid URL, set serviceLocation to null");
          URL url = new URL(loc);
          serviceLocation = null;
        } catch (Exception e) {
          serviceLocation = DeploymentLib.invokeHandler(serviceName, loc); // http://192.3.4.5/frog
          logger.info(String.format("loc is not valid, it presumably is cascaded [%s]", serviceLocation));
        }
      }

      logger.info("Get recipe");
      String className = service.getRecipe();

      logger.info("Loading plugin");
      Class<?> loadedClass = Class.forName(className);
      Class<? extends RecipePlugin> pluginClass = loadedClass.asSubclass(RecipePlugin.class);
      RecipePlugin plugin;
      plugin = pluginClass.getDeclaredConstructor().newInstance();
      plugin.init(workDir, service, serviceLocation);

      logger.info("Check user config against service record");
      String dbConfig = service.getServiceSemantics();
      boolean userConfigIsValid = this.checkUserConfig(
          DeploymentLib.parseSemantics(dbConfig),
          DeploymentLib.parseUserConfig(workDir)
      );

      if (!userConfigIsValid) {
        String msg = "user config is invalid";
        ObjectNode status = jsonFactory.objectNode();
        status.put("finished", false);
        return handleException("", new IllegalStateException());
      }

      Queue queue = new Queue();
      logger.info("Plugin invoked");
      ObjectNode json = queue.push(workDir, plugin);
      return Response
          .ok(json.toString(), APPLICATION_JSON)
          .build();

    } catch (Exception ex
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
  ) throws RecipePluginException, IOException {
    Boolean deleteResult = false;
    logger.info(String.format("Saving folia file and downloading for service [%s]", service));
    // TODO: Queues are created at three different places atm, shouldn't there be just one queue?
    Queue queue = new Queue();

    // is service is FOLIAEDITOR save file first before closing
    if (service.equals("FOLIAEDITOR")) {
      logger.info("Service is FOLIAEDITOR");
      RecipePlugin plugin = queue.getPlugin(workDir);

      FoliaEditor editor = (FoliaEditor) plugin;
      deleteResult = editor.saveFoliaFileFromEditor();

    }

    queue.removeTask(workDir);
    if (deleteResult) {
      return Response
          .ok("deleted", APPLICATION_JSON)
          .build();
    } else {
      return Response.status(500).build();

    }

  }


  private boolean checkUserConfig(ObjectNode dbSymantics, ObjectNode userSymantics) {
    // TODO: check if user config if valid instead of returning true
    logger.info(format("userConfig: %s", userSymantics.toString()));
    logger.info(format("dbConfig: %s", dbSymantics.toString()));
    return true;
  }

}
