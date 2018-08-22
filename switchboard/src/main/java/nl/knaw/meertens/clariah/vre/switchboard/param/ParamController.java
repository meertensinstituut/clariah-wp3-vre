package nl.knaw.meertens.clariah.vre.switchboard.param;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/services")
public class ParamController extends AbstractController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    ParamService paramService;

    @Inject
    ObjectMapper mapper;

    @GET
    @Path("/{service}/params")
    @Produces(APPLICATION_JSON)
    public Response getParamsFor(@PathParam("service") Long service) {
            Cmdi cmdi = paramService.getParams(service);
            return createResponse(cmdi);
    }


}
