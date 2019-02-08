package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.SystemConfig;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil;
import nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.STATUS_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.createTestFileWithRegistryObject;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PollingServiceImplTest extends AbstractControllerTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void testDeploymentStatusReportFile() throws Exception {
    var object = createTestFileWithRegistryObject(resultSentence);
    var uniqueTestFile = object.filepath;

    var startTest = now();
    var expectedService = "UCTO";
    var deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
    var deployResponse = deploy(expectedService, deploymentRequestDto);
    var json = deployResponse.readEntity(String.class);

    String workDir = JsonPath.parse(json).read("$.workDir");
    MockServerUtil.startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");

    var request = target(String.format("exec/task/%s", workDir)).request();
    DeployUtil.waitUntil(request, RUNNING);

    testReportFileFields(startTest, expectedService, workDir, uniqueTestFile, RUNNING);
  }

  @Test
  public void testPollingInterval() throws Exception {
    var object = createTestFileWithRegistryObject(resultSentence);
    var deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
    var deployResponse = deploy("UCTO", deploymentRequestDto);
    var json = deployResponse.readEntity(String.class);
    String workDir = JsonPath.parse(json).read("$.workDir");

    var report = waitUntillDeploymentIsPolled(workDir);

    var firstTimePolled = report.getPolled();
    assertThat(firstTimePolled.isBefore(now())).isTrue();
    assertThat(report.getInterval()).isLessThanOrEqualTo(3);
    report = getReportWhenPollInterval(workDir, 5);

    assertThat(report.getInterval()).isGreaterThanOrEqualTo(4);
    assertThat(report.getPolled().isAfter(firstTimePolled)).isTrue();
  }

  private DeploymentStatusReport waitUntillDeploymentIsPolled(String workDir) throws IOException, InterruptedException {
    var maxWaitPeriod = 20;
    var waited = 0;
    DeploymentStatusReport report;
    while (waited < maxWaitPeriod) {
      report = getReport(workDir);
      if (isNull(report.getPolled())) {
        logger.info(String.format("Deployment [%s] not polled yet", workDir));
      } else {
        return report;
      }
      waited++;
      TimeUnit.SECONDS.sleep(1);
    }
    throw new IllegalStateException(
      String.format("Deployment [%s] was not polled within [%s] seconds", workDir, maxWaitPeriod));
  }

  private DeploymentStatusReport getReportWhenPollInterval(String workDir, int pollInterval)
    throws IOException, InterruptedException {
    var maxWaitPeriod = 20;
    var waited = 0;

    while (waited < maxWaitPeriod) {
      var report = getReport(workDir);
      if (report.getInterval() >= pollInterval) {
        logger.info(String.format("Found poll interval: [%d]", pollInterval));
        return report;
      }
      logger.info(String.format("Current poll interval is: [%d]", pollInterval));
      waited++;
      TimeUnit.SECONDS.sleep(1);
    }
    throw new IllegalStateException(
      String.format("Could not find report with poll interval [%d] within [%s] seconds", pollInterval, maxWaitPeriod));
  }

  private void testReportFileFields(
    LocalDateTime startTest,
    String expectedService,
    String workDir,
    String uniqueTestFile,
    DeploymentStatus status
  ) throws IOException {
    var reportPath = Paths.get(DEPLOYMENT_VOLUME, workDir, STATUS_FILE_NAME);
    assertThat(reportPath.toFile()).exists();
    var reportJson = FileUtils.readFileToString(reportPath.toFile(), UTF_8);
    var report = getMapper().readValue(reportJson, DeploymentStatusReport.class);

    assertThat(report.getStatus()).isEqualTo(status);
    assertThat(report.getWorkDir()).isEqualTo(workDir);
    assertThat(report.getPolled()).isAfter(startTest);
    assertThat(report.getPolled()).isBefore(now());
    assertThat(report.getService()).isEqualTo(expectedService);
    assertThat(report.getFiles().get(0)).isEqualTo(uniqueTestFile);
  }

  private DeploymentStatusReport getReport(String workDir) throws IOException {
    var reportPath = Paths.get(DEPLOYMENT_VOLUME, workDir, STATUS_FILE_NAME);
    assertThat(reportPath.toFile()).exists();
    var reportJson = FileUtils.readFileToString(reportPath.toFile(), UTF_8);
    return getMapper().readValue(reportJson, DeploymentStatusReport.class);
  }
}
