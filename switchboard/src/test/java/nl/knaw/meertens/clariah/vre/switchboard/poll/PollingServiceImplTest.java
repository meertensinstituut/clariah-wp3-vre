package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.Config;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil;
import nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.createTestFileWithRegistryObject;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PollingServiceImplTest extends AbstractControllerTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testDeploymentStatusReportFile() throws Exception {
        ObjectsRecordDTO object = createTestFileWithRegistryObject(resultSentence);
        String uniqueTestFile = object.filepath;

        LocalDateTime startTest = now();
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
        Response deployResponse = deploy(expectedService, deploymentRequestDto);
        String json = deployResponse.readEntity(String.class);

        String workDir = JsonPath.parse(json).read("$.workDir");
        MockServerUtil.startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        DeployUtil.waitUntil(request, RUNNING);

        testReportFileFields(startTest, expectedService, workDir, uniqueTestFile, RUNNING);
    }

    @Test
    public void testPollingInterval() throws Exception {
        ObjectsRecordDTO object = createTestFileWithRegistryObject(resultSentence);
        DeploymentRequestDto deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
        Response deployResponse = deploy("UCTO", deploymentRequestDto);
        String json = deployResponse.readEntity(String.class);
        String workDir = JsonPath.parse(json).read("$.workDir");

        DeploymentStatusReport report = waitUntillDeploymentIsPolled(workDir);

        LocalDateTime firstTimePolled = report.getPolled();
        assertThat(firstTimePolled.isBefore(now())).isTrue();
        assertThat(report.getInterval()).isLessThanOrEqualTo(3);
        report = getReportWhenPollInterval(workDir, 5);

        assertThat(report.getInterval()).isGreaterThanOrEqualTo(4);
        assertThat(report.getPolled().isAfter(firstTimePolled)).isTrue();
    }

    private DeploymentStatusReport waitUntillDeploymentIsPolled(String workDir) throws IOException, InterruptedException {
        int maxWaitPeriod = 20;
        int waited = 0;
        DeploymentStatusReport report;
        while(waited < maxWaitPeriod) {
            report = getReport(workDir);
            if(isNull(report.getPolled())) {
               logger.info(String.format("Deployment [%s] not polled yet", workDir));
            } else {
                return report;
            }
            waited++;
            TimeUnit.SECONDS.sleep(1);
        }
        throw new IllegalStateException(String.format("Deployment [%s] was not polled within [%s] seconds", workDir, maxWaitPeriod));
    }

    private DeploymentStatusReport getReportWhenPollInterval(String workDir, int pollInterval) throws IOException, InterruptedException {
        int maxWaitPeriod = 20;
        int waited = 0;

        while (waited < maxWaitPeriod) {
            DeploymentStatusReport report = getReport(workDir);
            if(report.getInterval() >= pollInterval) {
                logger.info(String.format("Found poll interval: [%d]", pollInterval));
                return report;
            }
            logger.info(String.format("Current poll interval is: [%d]", pollInterval));
            waited++;
            TimeUnit.SECONDS.sleep(1);
        }
        throw new IllegalStateException(String.format("Could not find report with poll interval [%d] within [%s] seconds", pollInterval, maxWaitPeriod));
    }

    private void testReportFileFields(
            LocalDateTime startTest,
            String expectedService,
            String workDir,
            String uniqueTestFile,
            DeploymentStatus status
    ) throws IOException {
        Path reportPath = Paths.get(Config.DEPLOYMENT_VOLUME, workDir, Config.STATUS_FILE_NAME);
        assertThat(reportPath.toFile()).exists();
        String reportJson = FileUtils.readFileToString(reportPath.toFile(), UTF_8);
        DeploymentStatusReport report = getMapper().readValue(reportJson, DeploymentStatusReport.class);

        assertThat(report.getStatus()).isEqualTo(status);
        assertThat(report.getWorkDir()).isEqualTo(workDir);
        assertThat(report.getPolled()).isAfter(startTest);
        assertThat(report.getPolled()).isBefore(now());
        assertThat(report.getService()).isEqualTo(expectedService);
        assertThat(report.getFiles().get(0)).isEqualTo(uniqueTestFile);
    }

    private DeploymentStatusReport getReport(String workDir) throws IOException {
        Path reportPath = Paths.get(Config.DEPLOYMENT_VOLUME, workDir, Config.STATUS_FILE_NAME);
        assertThat(reportPath.toFile()).exists();
        String reportJson = FileUtils.readFileToString(reportPath.toFile(), UTF_8);
        return getMapper().readValue(reportJson, DeploymentStatusReport.class);
    }
}
