package nl.knaw.meertens.clariah.vre.switchboard.param;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/services")
public class ParamController extends AbstractController {

  @Inject
  ParamService paramService;

  @GET
  @Path("/{service}/params")
  @Produces(APPLICATION_JSON)
  public Response getParamsFor(@PathParam("service") Long service) {
    var cmdi = paramService.getParams(service);
    return createResponse(cmdi);
  }


}
