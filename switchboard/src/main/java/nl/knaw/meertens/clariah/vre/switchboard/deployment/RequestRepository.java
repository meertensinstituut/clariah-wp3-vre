package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.consumer.DeploymentConsumer;
import nl.knaw.meertens.clariah.vre.switchboard.exception.NoReportFileException;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_MEMORY_SPAN;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;

public class RequestRepository {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final String deploymentRoot;
  private final String statusFileName;
  private final ObjectMapper mapper;

  private final Map<String, DeploymentStatusReport> reports = new HashMap<>();
  private final Map<String, DeploymentConsumer> consumers = new HashMap<>();
  private final Map<String, LocalDateTime> finished = new HashMap<>();

  public RequestRepository(
    String deploymentRoot,
    String statusFileName,
    ObjectMapper mapper
  ) {
    this.deploymentRoot = deploymentRoot;
    this.statusFileName = statusFileName;
    this.mapper = mapper;
  }

  public DeploymentConsumer getConsumer(String workDir) {
    return consumers.get(workDir);
  }

  DeploymentStatusReport getStatusReport(String workDir) {
    var request = reports.get(workDir);
    if (!isNull(request)) {
      return request;
    }
    logger.info(String.format("Report of [%s] not available in memory: checking work dir", workDir));
    return findReportInWorkDir(workDir);
  }

  void saveDeploymentRequest(
    DeploymentStatusReport report,
    DeploymentConsumer reportConsumer
  ) {
    saveStatusReport(report);
    consumers.put(report.getWorkDir(), reportConsumer);
    logger.info(String.format(
      "Persisted deployment request of workDir [%s]",
      report.getWorkDir()
    ));
  }

  public List<DeploymentStatusReport> getAllStatusReports() {
    return new ArrayList<>(reports.values());
  }

  /**
   * Status reports are saved in a hashmap and
   * in a json file in workDir.
   *
   * <p>A report is removed from the hashmap when it has
   * finished longer than DEPLOYMENT_MEMORY_SPAN-seconds ago.
   *
   * <p>When the status of a deployment is requested,
   * it is added to the hashmap again
   */
  public void saveStatusReport(DeploymentStatusReport report) {
    var workDir = report.getWorkDir();
    reports.put(workDir, report);
    saveToFile(report);
    if (report.getStatus() == FINISHED) {
      handleFinishedRequest(workDir);
    }
  }

  private DeploymentStatusReport findReportInWorkDir(String workDir) {
    var statusFile = getStatusFilePath(workDir).toFile();
    var statusJson = "";
    if (!statusFile.exists()) {
      throw new NoReportFileException(String.format("Status of [%s] could not be found", workDir));
    }
    try {
      statusJson = FileUtils.readFileToString(statusFile, UTF_8);
    } catch (IOException e) {
      throw new RuntimeIOException(String.format("Could not read [%s]", statusFile.toString()), e);
    }
    try {
      return mapper.readValue(statusJson, DeploymentStatusReport.class);
    } catch (IOException e) {
      throw new RuntimeIOException(String.format("Could not parse [%s] to DeploymentStatusReport", statusJson), e);
    }
  }

  private void handleFinishedRequest(String workDir) {
    if (!finished.containsKey(workDir)) {
      finished.put(workDir, now());
    } else if (now().isAfter(finished.get(workDir).plusSeconds(DEPLOYMENT_MEMORY_SPAN))) {
      reports.remove(workDir);
      consumers.remove(workDir);
      finished.remove(workDir);
    }
  }

  private void saveToFile(DeploymentStatusReport report) {
    var file = getStatusFilePath(report.getWorkDir());
    try {
      var json = mapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(report);
      FileUtils.write(file.toFile(), json, UTF_8);
    } catch (IOException e) {
      throw new RuntimeIOException(String.format("Could create status report file [%s]", file.toString()), e);
    }
  }

  private Path getStatusFilePath(String workDir) {
    return Paths.get(
      deploymentRoot,
      workDir,
      statusFileName
    );
  }

  public void clearAll() {
    this.reports.clear();
    this.consumers.clear();
    this.finished.clear();
  }

}
