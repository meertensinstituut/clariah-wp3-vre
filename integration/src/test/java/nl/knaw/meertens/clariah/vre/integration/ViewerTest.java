package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentHasStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentWithStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.filesAreUnlocked;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.newObjectIsAdded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ViewerTest extends AbstractIntegrationTest {

  private final static String someContent = "De literatuurbeoefening beslaat de ganse horizon van de literator. " +
    "Vijfennegentig procent van de literatuur betreft het superieure ik, het heerlijke zelf, " +
    "het verrukkelijke ego van de literator en zijn onbenullige avonturen.";
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void testViewingFileWithSimplestViewer() throws UnirestException {
    String testFilename = uploadTestFile(someContent);

    await().until(() -> fileHasContent(testFilename, someContent));
    await().until(() -> fileExistsInRegistry(testFilename, "text/plain", "Plain text"));

    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(String.format("input file has object id [%d]", inputFileId));

    String workDir = startViewDeploymentWithInputFileId(inputFileId);
    logger.info(String.format("deployment has workdir [%s]", workDir));

    await().until(() -> deploymentHasStatus(workDir, "RUNNING"));

    HttpResponse<String> result = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    String body = result.getBody();
    String view = JsonPath.parse(body).read("$.viewerFileContent");
    assertThat(view).isEqualTo("<pre>" + someContent + "</pre>");

    awaitOcc();

    await().until(() -> filesAreUnlocked(testFilename, someContent));

    String secondNewInputFile = uploadTestFile(someContent);
    await().until(() -> newObjectIsAdded(secondNewInputFile));
  }

  private String startViewDeploymentWithInputFileId(long inputFileId) throws UnirestException {
    HttpResponse<String> result = Unirest
      .post(Config.SWITCHBOARD_ENDPOINT + "/exec/VIEWER")
      .header("Content-Type", "application/json; charset=UTF-8")
      .body("{\"params\":[{\"name\":\"input\",\"type\":\"file\",\"value\":" +
        inputFileId + "}]}")
      .asString();

    assertThat(result.getStatus()).isIn(200, 201, 202);
    return JsonPath.parse(result.getBody()).read("$.workDir");
  }

}
