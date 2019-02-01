package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.Unirest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentHasStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startDeploymentWithInputFileId;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.awaitility.Awaitility.await;

public class DeployServiceByFileTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testServiceIsFoundByFile_andServiceCanBeDeployed() throws Exception {
        String someContent = "'t Is vreemd, in al die jaren heb ik niet geweten " +
                "dat het op kantoor zo gezellig kan zijn. " +
                "In die kaas moest ik stikken, terwijl ik hier, " +
                "tussen twee briefjes in, even kan luisteren naar innerlijke stemmen.";
        String inputFile = uploadTestFile(someContent);
        await().until(() -> fileHasContent(inputFile, someContent));

        long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(inputFile));
        logger.info(String.format("input file has object id [%d]", inputFileId));

        String url = String.format("%s/object/%d/services", Config.SWITCHBOARD_ENDPOINT, inputFileId);
        String json = Unirest.get(url).asString().getBody();
        assertThatJson(json).node("[0]").isPresent();
        assertThatJson(json).node("[0].name").isEqualTo("TEST");

        String workDir = startDeploymentWithInputFileId(inputFileId);
        await().until(() -> deploymentHasStatus(workDir, "RUNNING"));

    }
}
