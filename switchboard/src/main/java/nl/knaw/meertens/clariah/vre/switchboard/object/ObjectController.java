package nl.knaw.meertens.clariah.vre.switchboard.object;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.List;

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
        logger.info(String.format("Received request of available services for object [%s]", objectId));
        List<ServiceRecord> services = objectService.getServicesOfKindServiceFor(objectId);
        return createResponse(services);
    }

    @GET
    @Path("/{objectId}/view/{viewer}")
    @Produces(APPLICATION_JSON)
    public Response getViewFor(@PathParam("objectId") long objectId, @PathParam("viewer") String viewer) {
        logger.info(String.format("Received request to view object [%s] with viewer [%s]", objectId, viewer));
        List<ServiceRecord> services = objectService.getServicesOfKindServiceFor(objectId);
        return createResponse(services);
    }

}
