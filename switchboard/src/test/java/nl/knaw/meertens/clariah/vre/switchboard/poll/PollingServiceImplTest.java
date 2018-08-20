package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.Config;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PollingServiceImplTest extends AbstractControllerTest {

    @Test
    public void testDeploymentStatusReportFile() throws Exception {
        ObjectsRecordDTO object = createTestFileWithRegistryObject();
        String uniqueTestFile = object.filepath;

        LocalDateTime startTest = LocalDateTime.now();
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("" + object.id);
        Response deployResponse = deploy(expectedService, deploymentRequestDto);
        String json = deployResponse.readEntity(String.class);

        String workDir = JsonPath.parse(json).read("$.workDir");
        startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        waitUntil(request, RUNNING);

        testReportFileFields(startTest, expectedService, workDir, uniqueTestFile, RUNNING);
    }

    @Test
    public void testPollingInterval() throws Exception {
        ObjectsRecordDTO object = createTestFileWithRegistryObject();
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("" + object.id);
        Response deployResponse = deploy("UCTO", deploymentRequestDto);
        String json = deployResponse.readEntity(String.class);
        String workDir = JsonPath.parse(json).read("$.workDir");

        int pollInterval = getPollIntervalFromReportFile(workDir);
        assertThat(pollInterval).isGreaterThanOrEqualTo(1);
        assertThat(pollInterval).isLessThanOrEqualTo(3);

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", "UCTO");
        TimeUnit.SECONDS.sleep(pollInterval + 5);
        waitUntil(request, FINISHED);

        pollInterval = getPollIntervalFromReportFile(workDir);
        assertThat(pollInterval).isLessThanOrEqualTo(4);
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
        assertThat(report.getPolled()).isBefore(LocalDateTime.now());
        assertThat(report.getService()).isEqualTo(expectedService);
        assertThat(report.getFiles().get(0)).isEqualTo(uniqueTestFile);
    }

    private int getPollIntervalFromReportFile(String workDir) throws IOException {
        Path reportPath = Paths.get(Config.DEPLOYMENT_VOLUME, workDir, Config.STATUS_FILE_NAME);
        assertThat(reportPath.toFile()).exists();
        String reportJson = FileUtils.readFileToString(reportPath.toFile(), UTF_8);
        DeploymentStatusReport report = getMapper().readValue(reportJson, DeploymentStatusReport.class);
        return report.getInterval();
    }
}
