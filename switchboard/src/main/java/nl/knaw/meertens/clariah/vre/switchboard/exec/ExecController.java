package nl.knaw.meertens.clariah.vre.switchboard.exec;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/exec")
public class ExecController extends AbstractController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  ExecService execService;

  @POST
  @Path("/{service}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response postDeploymentRequest(
    @PathParam("service") String service,
    String body
  ) {
    logger.info(format("Received request of service [%s] with body [%s]", service, body));
    var request = execService.deploy(service, body);
    var msg = new SwitchboardMsg(format(
      "Deployment of service [%s] has been requested.", request.getService()
    ));
    msg.workDir = request.getWorkDir();
    msg.status = request.getStatusReport().getStatus();
    return createResponse(msg, msg.status.getHttpStatus());
  }

  @GET
  @Path("/task/{workDir}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response getDeploymentStatus(@PathParam("workDir") String workDir) {
    logger.info(format("Status request of [%s]", workDir));
    var report = execService.getStatus(workDir);
    return createResponse(report, report.status.getHttpStatus());
  }

  @DELETE
  @Path("/task/{workDir}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response deleteDeploymentRequest(
    @PathParam("workDir") String workDir
  ) {
    logger.info(format("Received request to delete service [%s]", workDir));
    execService.delete(workDir);
    return createResponse(workDir, 200);
  }
}
