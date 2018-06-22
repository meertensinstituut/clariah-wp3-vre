package nl.knaw.meertens.clariah.vre.switchboard.param;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleControllerException;

@Path("/param")
public class ParamController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    ParamService paramService;

    @Inject
    ObjectMapper mapper;

    @GET
    @Path("/{service}")
    @Produces(APPLICATION_JSON)
    public Response getCmdiFor(@PathParam("service") long service) {
        try {
            logger.info(String.format("Received request for cmdi of service [%s]", service));
            CmdiDto cmdi = paramService.getCmdi(service);
            return Response
                    .status(200)
                    .entity(mapper.writeValueAsString(cmdi))
                    .build();
        } catch (Exception e) {
            return handleControllerException(e);
        }

    }


}