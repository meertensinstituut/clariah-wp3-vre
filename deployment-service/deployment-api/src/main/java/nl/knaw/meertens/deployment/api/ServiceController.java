package nl.knaw.meertens.deployment.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static java.lang.String.format;

// TODO: extract all logic to services
@Path("/service")
public class ServiceController extends AbstractController {

  private static JsonNodeFactory factory = new JsonNodeFactory(false);

  @GET
  @Path("/{service}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getService(@PathParam("service") String serviceName) {
    DeploymentLib dplib = new DeploymentLib();
    try {
      Service service = dplib.getServiceByName(serviceName);
      ObjectNode json = factory.objectNode();
      json.put("serviceName", service.getName());
      json.put("serviceId", service.getServiceId());
      json.put("serviceSymantics", service.getServiceSemantics());
      json.put("serviceTechInfo", service.getServiceTechInfo());
      return Response
        .status(200)
        .entity(json.toString())
        .build();
    } catch (IOException ex) {
      String msg = format("Failed to get service by name [%s]", serviceName);
      return handleException(msg, ex);
    }
  }

}
