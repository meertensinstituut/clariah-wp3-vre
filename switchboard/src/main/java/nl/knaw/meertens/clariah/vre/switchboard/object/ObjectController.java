package nl.knaw.meertens.clariah.vre.switchboard.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecordDto;
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
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleControllerException;

@Path("/object")
public class ObjectController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    ObjectService objectService;

    @Inject
    ObjectMapper mapper;

    @GET
    @Path("/{objectId}/services")
    @Produces(APPLICATION_JSON)
    public Response getServicesFor(@PathParam("objectId") long objectId) {
        try {
            logger.info(String.format("Received request of available services for object [%s]", objectId));
            List<ServiceRecordDto> services = objectService.getServicesOfKindServiceFor(objectId);
            return Response
                    .status(200)
                    .entity(mapper.writeValueAsString(services))
                    .build();
        } catch (Exception e) {
            return handleControllerException(e);
        }

    }

    @GET
    @Path("/{objectId}/view/{viewer}")
    @Produces(APPLICATION_JSON)
    public Response getViewFor(@PathParam("objectId") long objectId, @PathParam("viewer") String viewer) {
        try {
            logger.info(String.format("Received request to view object [%s] with viewer [%s]", objectId, viewer));
            List<ServiceRecordDto> services = objectService.getServicesOfKindServiceFor(objectId);
            return Response
                    .status(200)
                    .entity(mapper.writeValueAsString(services))
                    .build();
        } catch (Exception e) {
            return handleControllerException(e);
        }

    }

}
