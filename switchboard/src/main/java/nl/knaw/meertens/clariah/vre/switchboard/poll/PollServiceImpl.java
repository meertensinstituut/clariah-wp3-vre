package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusResponseDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class PollServiceImpl implements PollService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RequestRepositoryService requestRepositoryService;
    private ObjectMapper mapper;
    private final String hostName;
    private volatile boolean polling = false;
    private Thread pollThread;

    public PollServiceImpl(
            RequestRepositoryService requestRepositoryService,
            ObjectMapper mapper,
            String hostName
    ) {
        this.requestRepositoryService = requestRepositoryService;
        this.mapper = mapper;
        this.hostName = hostName;
        startPolling();
    }

    @Override
    public void startPolling() {
        if (polling) {
            logger.error("Already polling");
            return;
        }
        startPollingInSeparateThread();
        polling = true;
    }

    @Override
    public void stopPolling() {
        logger.info("Stop polling");
        pollThread.interrupt();
        polling = false;
    }

    private void startPollingInSeparateThread() {
        pollThread = new Thread(() -> {
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(1);
                    logger.info("Start polling...");
                    poll();
                }
            } catch (InterruptedException e) {
                logger.warn("Polling was interrupted");
            }
        });
        pollThread.start();
    }

    private void poll() {
        for (DeploymentStatusReport report : requestRepositoryService.getAllStatusReports()) {
            if (report.getStatus() != DeploymentStatus.FINISHED) {
                String workDir = report.getWorkDir();
                logger.info(String.format("Polling [%s]", workDir));

                DeploymentStatusReport updatedReport = getDeploymentStatus(report);
                requestRepositoryService.saveStatusReport(updatedReport);
                runConsumer(updatedReport);

                logger.info(String.format(
                        "Polled deployment [%s]; received status [%s]",
                        updatedReport.getWorkDir(), updatedReport.getStatus()
                ));
            }
        }
    }


    private void runConsumer(DeploymentStatusReport report) {
        requestRepositoryService
                .getConsumer(report.getWorkDir())
                .accept(report);
    }

    private DeploymentStatusReport getDeploymentStatus(DeploymentStatusReport report) {
        URI uri = createDeploymentStatusUri(report.getWorkDir());
        DeploymentStatusResponseDto response = requestDeploymentStatusReport(uri);
        report.setStatus(DeploymentStatus.getPollStatus(response.httpStatus));
        report.setMsg(response.message);
        return report;
    }

    private DeploymentStatusResponseDto requestDeploymentStatusReport(URI uri) {
        DeploymentStatusResponseDto response = new DeploymentStatusResponseDto();
        try {
            HttpResponse<String> httpResponse = Unirest
                    .get(uri.toString())
                    .asString();
            response = isBlank(httpResponse.getBody())
                    ? new DeploymentStatusResponseDto()
                    : mapper.readValue(httpResponse.getBody(), DeploymentStatusResponseDto.class);
            response.httpStatus = httpResponse.getStatus();
        } catch (UnirestException | IOException e) {
            logger.error(String.format("Deployment status request failed for [%s]", uri.toString()), e);
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
