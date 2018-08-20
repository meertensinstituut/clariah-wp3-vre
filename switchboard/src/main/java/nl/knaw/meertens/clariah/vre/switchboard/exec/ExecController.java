package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleControllerException;

@Path("/exec")
public class ExecController extends AbstractController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    ExecService execService;

    @Inject
    ObjectMapper mapper;

    @GET
    @Produces(APPLICATION_JSON)
    public Response getHelp() {
        SwitchboardMsg msg = new SwitchboardMsg("See readme for info on how to use exec api");
        return createResponse(msg);
    }

    @POST
    @Path("/{service}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response postDeploymentRequest(
            @PathParam("service") String service,
            String body
    ) {
        logger.info(String.format("Received request of service [%s] with body [%s]", service, body));
        DeploymentRequest request = execService.deploy(service, body);
        SwitchboardMsg switchboardMsg = new SwitchboardMsg(String.format(
                "Deployment of service [%s] has been requested.", request.getService()
        ));
        switchboardMsg.workDir = request.getWorkDir();
        switchboardMsg.status = request.getStatusReport().getStatus();
        return createResponse(switchboardMsg, switchboardMsg.status.getHttpStatus());
    }

    @GET
    @Path("/task/{workDir}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getDeploymentStatus(@PathParam("workDir") String workDir) {
        logger.info(String.format("Status request of [%s]", workDir));
        DeploymentStatusReport report = execService.getStatus(workDir);
        return createResponse(report, report.getStatus().getHttpStatus());
    }
}