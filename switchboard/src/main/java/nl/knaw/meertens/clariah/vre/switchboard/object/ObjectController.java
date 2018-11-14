package nl.knaw.meertens.clariah.vre.switchboard.object;

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

@Path("/object")
public class ObjectController extends AbstractController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    ObjectService objectService;

    @GET
    @Path("/{objectId}/services")
    @Produces(APPLICATION_JSON)
    public Response getServicesFor(@PathParam("objectId") long objectId) {
        logger.info(String.format("Received request of available 'service' services for object [%s]", objectId));
        var services = objectService.getServiceServicesFor(objectId);
        return createResponse(services);
    }

    @GET
    @Path("/{objectId}/viewers")
    @Produces(APPLICATION_JSON)
    public Response getViewersFor(@PathParam("objectId") long objectId) {
        logger.info(String.format("Received request of available viewers for object [%s]", objectId));
        var services = objectService.getViewersFor(objectId);
        return createResponse(services);
    }

    @GET
    @Path("/{objectId}/editors")
    @Produces(APPLICATION_JSON)
    public Response getEditorsFor(@PathParam("objectId") long objectId) {
        logger.info(String.format("Received request of available editors for object [%s]", objectId));
        var services = objectService.getEditorsFor(objectId);
        return createResponse(services);
    }

}
