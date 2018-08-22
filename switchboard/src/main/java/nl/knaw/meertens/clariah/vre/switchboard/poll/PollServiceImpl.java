package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusResponseDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.FinishDeploymentConsumer;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static java.time.LocalDateTime.now;
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

                DeploymentStatusReport newReport = getDeploymentStatus(report);
                runConsumer(newReport);
                requestRepositoryService.saveStatusReport(newReport);

                logger.info(String.format(
                        "Polled deployment [%s]; received status [%s]",
                        newReport.getWorkDir(), newReport.getStatus()
                ));
            }
        }
    }

    private boolean inNeedOfPolling(DeploymentStatusReport report) {
        return report.getStatus() != DeploymentStatus.FINISHED;
    }

    private void runConsumer(DeploymentStatusReport report) {
        try {
            FinishDeploymentConsumer<DeploymentStatusReport> finishDeploymentConsumer =
                    requestRepositoryService.getConsumer(report.getWorkDir());
            finishDeploymentConsumer.accept(report);
        } catch (Exception e) {
            logger.error(String.format("Consumer of deployment [%s] threw exception", report.getWorkDir()), e);
        }
    }

    private DeploymentStatusReport getDeploymentStatus(DeploymentStatusReport report) {
        DeploymentStatusReport result = new DeploymentStatusReport(report);

        URI uri = createDeploymentStatusUri(report);
        DeploymentStatusResponseDto response = requestDeploymentStatusReport(uri);
        result.setStatus(DeploymentStatus.getDeployStatus(response.httpStatus));
        result.setMsg(response.message);
        result.setPolled(now());
        result.setInterval(calculateNewInterval(report.getInterval()));
        return result;
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
