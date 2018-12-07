package nl.knaw.meertens.deployment.api;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static java.lang.String.format;

// TODO: rename to controller
// TODO: extract all logic to services
@Path("/service")
public class WebServiceClass extends AbstractController {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @GET
  @Path("/{service}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getService(@PathParam("service") String serviceName) {
    DeploymentLib dplib = new DeploymentLib();
    try {
      Service service = dplib.getServiceByName(serviceName);
      JSONObject json = new JSONObject();
      json.put("serviceName", service.getName());
      json.put("serviceId", service.getServiceId());
      json.put("serviceSymantics", service.getServiceSemantics());
      json.put("serviceTechInfo", service.getServiceTechInfo());
      return Response
        .status(200)
        .entity(json.toJSONString())
        .build();
    } catch (IOException | ConfigurationException ex) {
      String msg = format("Failed to get service by name [%s]", serviceName);
      return handleException(msg, ex);
    }
  }

}
