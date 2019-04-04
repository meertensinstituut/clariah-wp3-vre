package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentHasStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentWithStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.filesAreUnlocked;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.filesAreUnlockedAfterEdit;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.newObjectIsAdded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class FoliaEditorTest extends AbstractIntegrationTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void testEditFoliaFileWithFoliaEditor() throws UnirestException, InterruptedException {
    String foliaContent = FileUtils.getTestFileContent("folia.xml");
    String uploadedFileName = UUID.randomUUID() + ".xml";
    String testFilename = uploadTestFile(uploadedFileName, foliaContent);

    // Path uploadedFile = Paths.get(uploadedFileName);
    // BasicFileAttributes attr = Files.readAttributes(uploadedFile, BasicFileAttributes.class);
    // logger.info("lastModifiedTime: " + attr.lastModifiedTime());

    await().until(() -> fileHasContent(testFilename, foliaContent));
    logger.info(String.format("Test file name is [%s]", testFilename));
    await().until(() -> fileExistsInRegistry(testFilename, "text/xml", "Extensible Markup Language"));

    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(String.format("input file has object id [%d]", inputFileId));

    String workDir = startFoliaEditorDeploymentWithInputFileId(inputFileId);
    logger.info(String.format("deployment has workdir [%s]", workDir));

    await().until(() -> deploymentHasStatus(workDir, "RUNNING"));

    HttpResponse<String> result = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    String body = result.getBody();
    String view = JsonPath.parse(body).read("$.viewerFileContent");
    assertThat(view).isEqualTo("<iframe src=\"http://localhost:9998/flat/editor/pub/full/untitled\" width=\"100%\" " +
        "height=\"800px\">Your browser does not support iframes. Direct link to editor: " +
        "\"http://localhost:9998/flat/editor/pub/full/untitled\"</iframe>");

    logger.info(String.format("URL to call [%s/exec/task/%s]", Config.SWITCHBOARD_ENDPOINT, workDir));
    HttpResponse<String> sendStopSignal = Unirest.delete(Config.SWITCHBOARD_ENDPOINT + "/exec/task/" + workDir).asString();
    assertThat(sendStopSignal.getStatus()).isEqualTo(200);

    awaitOcc();
    SECONDS.sleep(5);
    await().until(() -> filesAreUnlockedAfterEdit(testFilename, "<folia:text xml:id=\"untitled.text\">\n" +
        "    <folia:p xml:id=\"untitled.p.1\">\n" +
        "      <folia:s xml:id=\"untitled.p.1.s.1\">\n" +
        "        <folia:w folia:class=\"WORD\" xml:id=\"untitled.p.1.s.1.w.1\">\n" +
        "          <folia:t>aaa</folia:t>\n" +
        "        </folia:w>\n" +
        "        <folia:w folia:class=\"WORD\" xml:id=\"untitled.p.1.s.1.w.2\">\n" +
        "          <folia:t>is</folia:t>\n" +
        "        </folia:w>\n" +
        "        <folia:w folia:class=\"WORD\" xml:id=\"untitled.p.1.s.1.w.3\">\n" +
        "          <folia:t>not</folia:t>\n" +
        "        </folia:w>\n" +
        "        <folia:w space=\"no\" folia:class=\"WORD\" xml:id=\"untitled.p.1.s.1.w.4\">\n" +
        "          <folia:t>bbb</folia:t>\n" +
        "        </folia:w>\n" +
        "        <folia:w folia:class=\"PUNCTUATION\" xml:id=\"untitled.p.1.s.1.w.5\">\n" +
        "          <folia:t>.</folia:t>\n" +
        "        </folia:w>\n" +
        "      </folia:s>\n" +
        "    </folia:p>\n" +
        "  </folia:text>"));

    String secondNewInputFile = uploadTestFile(foliaContent);
    await().until(() -> newObjectIsAdded(secondNewInputFile));
  }

  private String startFoliaEditorDeploymentWithInputFileId(long inputFileId) throws UnirestException {
    HttpResponse<String> result = Unirest.post(Config.SWITCHBOARD_ENDPOINT + "/exec/FOLIAEDITOR")
                                         .header("Content-Type", "application/json; charset=UTF-8")
                                         .body("{\"params\":[{\"name\":\"input\",\"type\":\"file\",\"value\":" +
                                             inputFileId + "}]}")
                                         .asString();

    assertThat(result.getStatus()).isIn(200, 201, 202);
    return JsonPath.parse(result.getBody()).read("$.workDir");
  }

}
