package nl.knaw.meertens.clariah.vre.switchboard.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleControllerException;

@Path("/health")
public class HealthController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GET
    @Produces(APPLICATION_JSON)
    public Response getDeploymentStatus() {
        try {
            logger.info("Health request; respond with 200 OK");
            return Response
                    .status(200)
                    .entity("{\"status\": \"OK\"}")
                    .build();
        } catch (Exception e) {
            return handleControllerException(e);
        }

    }

}
