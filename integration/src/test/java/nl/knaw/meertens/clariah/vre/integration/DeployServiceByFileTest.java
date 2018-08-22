package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.Unirest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class DeployServiceByFileTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testServiceIsFoundByFile_andServiceCanBeDeployed() throws Exception {
        String someContent = "'t Is vreemd, in al die jaren heb ik niet geweten dat het op kantoor zo gezellig kan zijn. In die kaas moest ik stikken, terwijl ik hier, tussen twee briefjes in, even kan luisteren naar innerlijke stemmen.";
            String inputFile = uploadTestFile(someContent);

        // wait for services to process new file:
        TimeUnit.SECONDS.sleep(6);

        long inputFileId = getObjectIdFromRegistry(inputFile);
        logger.info(String.format("input file has object id [%d]", inputFileId));

        String url = String.format("%s/object/%d/services", SWITCHBOARD_ENDPOINT, inputFileId);
        String json = Unirest.get(url).asString().getBody();
        assertThatJson(json).node("[0]").isPresent();
        assertThatJson(json).node("[0].name").isEqualTo("TEST");

        String workDir = startDeploymentWithInputFileId(inputFileId);
        checkDeploymentStatus(workDir, 20, "RUNNING");

    }
}
