package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusResponseDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static java.time.LocalDateTime.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class PollServiceImpl implements PollService {

    private static final double INCREASE_INTERVAL_FACTOR = 1.1;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RequestRepository requestRepositoryService;
    private ObjectMapper mapper;
    private final String hostName;
    private volatile boolean polling = false;
    private Thread pollThread;

    public PollServiceImpl(
            RequestRepository requestRepository,
            ObjectMapper mapper,
            String hostName
    ) {
        this.requestRepositoryService = requestRepository;
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
            if (inNeedOfPolling(report)) {
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

    private boolean inNeedOfPolling(DeploymentStatusReport report) {
        if(report.getStatus() == DeploymentStatus.FINISHED) {
            return false;
        }
        return true;
//        LocalDateTime nextPollMoment = report.getPolled().plusSeconds(report.getInterval());
//        return now().isAfter(nextPollMoment);
    }

    private void runConsumer(DeploymentStatusReport report) {
        try {
            requestRepositoryService
                    .getConsumer(report.getWorkDir())
                    .accept(report);
        } catch (Exception e) {
            logger.error(String.format("consumer of deployment [%s] threw exception", report.getWorkDir()), e);
        }
    }

    private DeploymentStatusReport getDeploymentStatus(DeploymentStatusReport report) {
        URI uri = createDeploymentStatusUri(report);
        DeploymentStatusResponseDto response = requestDeploymentStatusReport(uri);
        report.setStatus(DeploymentStatus.getPollStatus(response.httpStatus));
        report.setMsg(response.message);
        report.setPolled(now());
        report.setInterval(calculateNewInterval(report.getInterval()));
        return report;
    }

    private int calculateNewInterval(int previousInterval) {
        return (int) Math.ceil(previousInterval * INCREASE_INTERVAL_FACTOR);
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

    private URI createDeploymentStatusUri(DeploymentStatusReport report) {
        return URI.create(String.format(
                "%s/%s/%s/%s",
                hostName,
                "deployment-service/a/exec",
                report.getService(),
                report.getWorkDir()
        ));
    }
}
