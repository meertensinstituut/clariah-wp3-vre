package nl.knaw.meertens.deployment.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/health")
public class HealthController extends AbstractController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @GET
  @Produces(APPLICATION_JSON)
  public Response getHealth() {
    logger.info("Health request; respond with 200 - OK");
    return Response
      .status(202)
      .entity(new Object() {
        public String status = "OK";
      })
      .type(APPLICATION_JSON)
      .build();
  }

}
