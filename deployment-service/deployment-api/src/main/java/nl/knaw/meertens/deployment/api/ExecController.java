package nl.knaw.meertens.deployment.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.EditorPluginImpl;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
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

    DeploymentResponse result;
    RecipePlugin plugin = Queue.getPlugin(workDir);

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
      plugin.cleanup();
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
      Service service = DeploymentLib.getServiceByName(serviceName);
      if (isNull(service)) {
        String msg = "invalid service";
        return handleException(msg);
      }

      String loc = DeploymentLib.getServiceLocationFromJson(DeploymentLib.parseSemantics(service
          .getServiceSemantics()));
      logger.info(String.format("Has loc [%s]", loc));

      ObjectNode json = null;

      if (loc.equals("") || loc.toLowerCase().equals("none")) {
        logger.info("loc is empty, no loc handlers needed");
      } else {
        try {
          logger.info("loc is valid URL, no loc handlers needed");
          URL url = new URL(loc);
        } catch (Exception e) {
          json = DeploymentLib.invokeHandler(workDir, service, loc);
        }
      }

      if (isNull(json)) {
        json = DeploymentLib.invokeService(workDir, service, null, null);
      }

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
    logger.info(String.format("Saving file from editor and downloading for service [%s]", service));

    RecipePlugin plugin = Queue.getPlugin(workDir);
    if (plugin instanceof EditorPluginImpl) {
      deleteResult = ((EditorPluginImpl) plugin).saveFileFromEditor();
    }

    Queue.removeTask(workDir);
    if (deleteResult) {
      return Response
          .ok("deleted", APPLICATION_JSON)
          .build();
    } else {
      return Response.status(500).build();

    }

  }

}
