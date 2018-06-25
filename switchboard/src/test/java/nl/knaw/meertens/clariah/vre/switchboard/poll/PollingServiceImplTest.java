package nl.knaw.meertens.clariah.vre.switchboard.poll;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.Config;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.DEPLOYED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PollingServiceImplTest extends AbstractControllerTest {

    @Before
    public void beforeDeploymentServiceImplTest() {
        startDeployMockServer(200);
    }

    @Test
    public void testDeploymentStatusReportFile() throws Exception {
        startStatusMockServer(RUNNING.getHttpStatus(), "{}");

        LocalDateTime startTest = LocalDateTime.now();
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();
        Response deployResponse = deploy(expectedService, deploymentRequestDto);
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        Invocation.Builder getStatusRequistBuilder = target(String.format("exec/task/%s", workDir)).request();

        Response statusResponse;
        do {
            TimeUnit.SECONDS.sleep(1);
            statusResponse = getStatusRequistBuilder.get();
        } while(statusResponse.getStatus() == DEPLOYED.getHttpStatus());

        assertThat(statusResponse.getStatus()).isEqualTo(RUNNING.getHttpStatus());
        testReportFields(startTest, expectedService, workDir, 1, RUNNING);

        TimeUnit.SECONDS.sleep(1);
        testReportFields(startTest, expectedService, workDir, 2, RUNNING);

        new MockServerClient("localhost", 1080).reset();
        startStatusMockServer(FINISHED.getHttpStatus(), "{}");
        TimeUnit.SECONDS.sleep(2);

        statusResponse = getStatusRequistBuilder.get();
        assertThat(statusResponse.getStatus()).isEqualTo(FINISHED.getHttpStatus());

        testReportFields(startTest, expectedService, workDir, 3, FINISHED);

    }

    private void testReportFields(
            LocalDateTime startTest,
            String expectedService,
            String workDir,
            int minInterval,
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
        assertThat(report.getFiles().get(0)).isEqualTo(testFile);
        assertThat(report.getInterval()).isGreaterThan(minInterval);
    }
}
