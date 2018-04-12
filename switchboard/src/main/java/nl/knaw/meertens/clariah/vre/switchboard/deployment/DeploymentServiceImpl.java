package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;

import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.handleException;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.getDeployStatus;

public class DeploymentServiceImpl implements DeploymentService {

    private final String hostName;
    private final RequestRepositoryService deployRequestService;

    public DeploymentServiceImpl(
            String hostName,
            RequestRepositoryService deployRequestService,

            PollService pollService) {
        this.hostName = hostName;
        this.deployRequestService = deployRequestService;

        pollService.startPolling();

    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public DeploymentStatusReport deploy(
            DeploymentRequest request,
            ExceptionalConsumer<DeploymentStatusReport> finishRequestConsumer
    ) {
        DeploymentStatusReport report = requestDeployment(request);
        deployRequestService.saveDeploymentRequest(report, finishRequestConsumer);
        return report;
    }

    private DeploymentStatusReport requestDeployment(DeploymentRequest request) {
        DeploymentStatusReport report = new DeploymentStatusReport();
        report.setUri(createDeployServiceUri(request));
        report.setService(request.getService());
        report.setFiles(new ArrayList<>(request.getFiles().values()));
        report.setWorkDir(request.getWorkDir());

        HttpResponse<String> response = sendRequest(report.getUri());
        report.setMsg(response.getBody());
        report.setStatus(getDeployStatus(response.getStatus()));

        return report;
    }

    private URI createDeployServiceUri(DeploymentRequest request) {
        return URI.create(String.format(
                "%s/%s/%s/%s/",
                hostName,
                "deployment-service/a/exec",
                request.getService(),
                request.getWorkDir()
        ));
    }

    @Override
    public DeploymentStatusReport getStatus(String workDir) {
        logger.info(String.format("Polling deployment [%s]", workDir));
        DeploymentStatusReport report = deployRequestService.getStatusReport(workDir);
        if (report.getStatus() == FINISHED) {
            logger.info(String.format("Unstage [%s]", workDir));
            unstageDeployment(workDir);
        }
        return report;
    }

    private void unstageDeployment(String workDir) {
        DeploymentStatusReport report = deployRequestService
                .getStatusReport(workDir);
        ExceptionalConsumer<DeploymentStatusReport> unstageConsumerMethod = deployRequestService
                .getConsumer(workDir);
        unstageConsumerMethod.accept(report);
        logger.info(String.format("Deployment [%s] has been unstaged by consumer ", report));
    }

    @Override
    public boolean stop(String workDir) {
        throw new UnsupportedOperationException("Stopping deployments has not been implemented");
    }

    private HttpResponse<String> sendRequest(URI uri) {
        try {
            HttpResponse<String> response = Unirest.get(uri.toString())
                    .asString();
            logger.info(String.format(
                    "Started deployment of [%s]",
                    uri.toString())
            );
            return response;
        } catch (UnirestException e) {
            handleException(e, "Could not start deployment of [%s]", uri.toString());
            return null;
        }
    }

}
