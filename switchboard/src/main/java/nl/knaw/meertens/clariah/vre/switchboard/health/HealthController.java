package nl.knaw.meertens.clariah.vre.switchboard.health;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleControllerException;

@Path("/health")
public class HealthController extends AbstractController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GET
    @Produces(APPLICATION_JSON)
    public Response getDeploymentStatus() {
        logger.info("Health request; respond with 200 OK");
        String msg = "{\"status\": \"OK\"}";
        return createResponse(msg);
    }

}
