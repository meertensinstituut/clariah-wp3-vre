package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusResponseDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.getDeployStatus;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class PollServiceImpl implements PollService {

  private static final double INCREASE_INTERVAL_FACTOR = 1.1;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final RequestRepository requestRepositoryService;
  private final String hostName;
  private ObjectMapper mapper;
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

  /**
   * Check deployment status:
   * - poll deployment service
   * - run deployment consumer function
   * - save deployment status
   */
  private void poll() {
    for (var report : requestRepositoryService.getAllStatusReports()) {
      if (shouldPoll(report)) {
        String workDir = report.getWorkDir();
        logger.info(String.format("Polling [%s]", workDir));
        report.setPolled(now());

        report = getDeploymentStatus(report);
        runConsumer(report);
        requestRepositoryService.saveStatusReport(report);

        logger.info(String.format(
          "Polled deployment [%s]; received status [%s]",
          report.getWorkDir(), report.getStatus()
        ));
      }
    }
  }

  private boolean shouldPoll(DeploymentStatusReport report) {
    var lastPoll = report.getPolled();
    if (isNull(lastPoll)) {
      return true;
    }

    var nextPoll = lastPoll
      .plusSeconds(report.getInterval());

    return report.getStatus() != FINISHED &&
      now().isAfter(nextPoll);
  }

  private void runConsumer(DeploymentStatusReport report) {
    var deploymentConsumer =
      requestRepositoryService.getConsumer(report.getWorkDir());
    try {
      deploymentConsumer.accept(report);
    } catch (Exception e) {
      logger.error(String.format("Consumer of deployment [%s] threw exception", report.getWorkDir()), e);
    }
  }

  private DeploymentStatusReport getDeploymentStatus(DeploymentStatusReport report) {
    var result = new DeploymentStatusReport(report);

    var uri = createDeploymentStatusUri(report);
    var response = requestDeploymentStatusReport(uri);
    result.setStatus(getDeployStatus(response.httpStatus));
    result.setMsg(response.message);
    result.setPolled(now());
    result.setInterval(calculateNewInterval(report.getInterval()));
    return result;
  }

  private int calculateNewInterval(int previousInterval) {
    return (int) Math.ceil(previousInterval * INCREASE_INTERVAL_FACTOR);
  }

  private DeploymentStatusResponseDto requestDeploymentStatusReport(URI uri) {
    try {
      DeploymentStatusResponseDto response;
      var httpResponse = Unirest
        .get(uri.toString())
        .asString();
      if (isBlank(httpResponse.getBody())) {
        response = new DeploymentStatusResponseDto();
      } else {
        response = mapper.readValue(
          httpResponse.getBody(),
          DeploymentStatusResponseDto.class
        );
      }
      response.httpStatus = httpResponse.getStatus();
      return response;
    } catch (UnirestException | IOException e) {
      throw new RuntimeIOException(String.format("Deployment status request failed for [%s]", uri.toString()), e);
    }
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
