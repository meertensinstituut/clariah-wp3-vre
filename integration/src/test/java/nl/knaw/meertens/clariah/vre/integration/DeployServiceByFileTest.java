package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.Unirest;
import nl.knaw.meertens.clariah.vre.integration.util.Poller;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentHasStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startDeploymentWithInputFileId;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileCanBeDownloaded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;

public class DeployServiceByFileTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testServiceIsFoundByFile_andServiceCanBeDeployed() throws Exception {
        String someContent = "'t Is vreemd, in al die jaren heb ik niet geweten " +
                "dat het op kantoor zo gezellig kan zijn. " +
                "In die kaas moest ik stikken, terwijl ik hier, " +
                "tussen twee briefjes in, even kan luisteren naar innerlijke stemmen.";
        String inputFile = uploadTestFile(someContent);
        Poller.awaitAndGet(() -> fileCanBeDownloaded(inputFile, someContent));

        long inputFileId = Poller.awaitAndGet(() -> getObjectIdFromRegistry(inputFile));
        logger.info(String.format("input file has object id [%d]", inputFileId));

        String url = String.format("%s/object/%d/services", Config.SWITCHBOARD_ENDPOINT, inputFileId);
        String json = Unirest.get(url).asString().getBody();
        assertThatJson(json).node("[0]").isPresent();
        assertThatJson(json).node("[0].name").isEqualTo("TEST");

        String workDir = startDeploymentWithInputFileId(inputFileId);
        Poller.awaitAndGet(() -> deploymentHasStatus(workDir, "RUNNING"));

    }
}
