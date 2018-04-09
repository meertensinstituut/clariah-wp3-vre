package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusResponseDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.handleException;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class PollService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RequestRegistryService deployRequestService;
    private ObjectMapper mapper;
    private final String hostName;

    public PollService(ObjectMapper mapper, String hostName) {
        this.mapper = mapper;
        this.hostName = hostName;
        deployRequestService = RequestRegistryService.getInstance();
    }

    public DeploymentStatusReport getDeploymentStatus(String workDir) {
        URI uri = createDeploymentStatusUri(workDir);
        DeploymentStatusResponseDto dto = requestDeploymentStatusReport(uri);
        DeploymentStatusReport report = deployRequestService.getStatusReport(workDir);
        report.setStatus(DeploymentStatus.getPollStatus(dto.httpStatus));
        report.setMsg(dto.message);
        return report;
    }

    private DeploymentStatusResponseDto requestDeploymentStatusReport(URI uri) {
        DeploymentStatusResponseDto response = null;
        try {
            HttpResponse<String> httpResponse = Unirest
                    .get(uri.toString())
                    .asString();

            response = isBlank(httpResponse.getBody())
                    ? new DeploymentStatusResponseDto()
                    : mapper.readValue(httpResponse.getBody(), DeploymentStatusResponseDto.class);
            response.httpStatus = httpResponse.getStatus();
            logger.info(String.format(
                    "Polled deployment [%s] and received http status [%s] with body [%s]",
                    uri.toString(), httpResponse.getStatus(), httpResponse.getBody()
            ));
        } catch (UnirestException | IOException e) {
            handleException(e, "Status request failed [%s]", uri.toString());
        }
        return response;
    }

    private URI createDeploymentStatusUri(String workDir) {
        return URI.create(String.format(
                "%s/%s/%s/",
                hostName,
                "deployment-service/a/exec/task",
                workDir
        ));
    }

}
