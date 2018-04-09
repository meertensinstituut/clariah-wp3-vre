package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.handleException;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.getDeployStatus;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.getPollStatus;

public class DeploymentServiceImpl implements DeploymentService {

    private final ObjectMapper mapper;
    private final String hostName;

    private final RequestRepoServiceStub deployRequestService = RequestRepoServiceStub.getInstance();

    public DeploymentServiceImpl(ObjectMapper mapper, String hostName) {
        this.mapper = mapper;
        this.hostName = hostName;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public DeploymentStatusReport start(
            DeploymentRequest request,
            ExceptionalConsumer<DeploymentStatusReport> finishRequest
    ) {
        deployRequestService.saveDeploymentRequest(request, finishRequest);

        deployRequestService.getConsumer(request.getWorkDir());

        URI uri = URI.create(String.format(
                "%s/%s/%s/%s/",
                hostName,
                "deployment-service/a/exec",
                request.getService(),
                request.getWorkDir()
        ));
        return sendDeployRequest(uri);
    }

    @Override
    public DeploymentStatusReport pollStatus(String workDir) {
        logger.info(String.format("Polling deployment [%s]", workDir));
        URI uri = URI.create(String.format(
                "%s/%s/%s/",
                hostName,
                "deployment-service/a/exec/task",
                workDir
        ));
        DeploymentStatusResponseDto response = getStatusRequest(uri);
        DeploymentStatusReport report = mapStatusReport(response);
        DeploymentStatus status = getPollStatus(response.httpStatus);
        report.setStatus(status);
        report.setWorkDir(workDir);
        if (status == FINISHED) {
            logger.info(String.format("Unstage [%s]", workDir));
            unstageDeployment(report);
        }
        return report;
    }

    private DeploymentStatusReport unstageDeployment(DeploymentStatusReport report) {
        deployRequestService
                .getRequest(report.getWorkDir())
                .getStatusReport();
        ExceptionalConsumer<DeploymentStatusReport> consumerMethod = deployRequestService
                .getConsumer(report.getWorkDir());

        consumerMethod.accept(report);

        logger.info(String.format("Deployment [%s] has been unstaged by consumer ", report));
        return report;
    }

    @Override
    public boolean stop(String workDir) {
        // TODO: stop deployment
        throw new UnsupportedOperationException("Deployments cannot be stopped");
    }

    private DeploymentStatusReport sendDeployRequest(URI uri) {
        try {
            HttpResponse<String> response = Unirest.get(uri.toString())
                    .asString();
            logger.info(String.format(
                    "Started deployment of [%s]",
                    uri.toString())
            );
            DeploymentStatusReport report = new DeploymentStatusReport();
            report.setMsg(response.getBody());
            report.setStatus(getDeployStatus(response.getStatus()));
            report.setUri(uri);
            return report;
        } catch (UnirestException e) {
            handleException(e, "Could not start deployment of [%s]", uri.toString());
            return null;
        }
    }

    private DeploymentStatusResponseDto getStatusRequest(URI uri) {
        try {
            HttpResponse<String> httpResponse = Unirest
                    .get(uri.toString())
                    .asString();
            logger.info(String.format("Polled deployment [%s] and received http status [%s] with body [%s]", uri.toString(), httpResponse.getStatus(), httpResponse.getBody()));
            DeploymentStatusResponseDto response =
            StringUtils.isBlank(httpResponse.getBody())
                    ? new DeploymentStatusResponseDto()
                    : mapper.readValue(httpResponse.getBody(), DeploymentStatusResponseDto.class);

            response.httpStatus = httpResponse.getStatus();
            return response;
        } catch (UnirestException | IOException e) {
            handleException(e, "Status request failed [%s]", uri.toString());
            return null;
        }
    }

    private DeploymentStatusReport mapStatusReport(DeploymentStatusResponseDto response) {
        DeploymentStatusReport result = new DeploymentStatusReport();
        result.setStatus(getPollStatus(response.httpStatus));
        result.setMsg(response.message);
        return result;
    }

}
