package nl.knaw.meertens.clariah.vre.switchboard;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PollingServiceImplTest extends AbstractSwitchboardTest {

    @Before
    public void beforeDeploymentServiceImplTest() {
        startDeployMockServer(200);
    }

    @Test
    public void testIsPolled() throws Exception {
        assertThat(true).isTrue();
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto());

        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        TimeUnit.SECONDS.sleep(1);

        Path configPath = Paths.get(Config.DEPLOYMENT_VOLUME, workDir, Config.CONFIG_FILE_NAME);

        assertThat(configPath.toFile()).exists();
        String config = FileUtils.readFileToString(configPath.toFile(), UTF_8);

    }
}
