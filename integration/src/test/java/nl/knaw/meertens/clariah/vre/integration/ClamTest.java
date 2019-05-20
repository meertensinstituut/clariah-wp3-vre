package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.sql.SQLException;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentWithStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ClamTest extends AbstractIntegrationTest {
  private static Logger logger = LoggerFactory.getLogger(ClamTest.class);

  private ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
    Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
  );

  @Test
  public void canRunUctoProject() throws UnirestException, SQLException {
    var resourceFileName = "deployment-test.txt";
    var testFileContent = getTestFileContent(resourceFileName);
    var testFilename = uploadTestFile(testFileContent);

    await().until(() -> fileHasContent(testFilename, testFileContent));
    await().until(() -> fileExistsInRegistry(testFilename, "text/plain", "Plain text"));
    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(format("input file has object id [%d]", inputFileId));

    var workDir = canCreateAndRunUctoProject(inputFileId);
    logger.info(format("workDir is [%s]", workDir));
    var resultFile = awaitAndGet(() -> deploymentIsFinished(workDir, testFilename));

    awaitOcc();
    var resultFileContent = "<w xml:id=\"untitled.p.1.s.1.w.1\" class=\"WORD\">\n" +
      "          <t>En</t>\n" +
      "        </w>";

    logger.info(format("Check result file [%s] has content [%s]", resultFile, resultFileContent));
    await().until(() -> resultFileCanBeDownloaded(resultFile, resultFileContent));

    // semantic types:
    long resultFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(resultFile));
    var query = "select * from object_semantic_type where object_id = " + resultFileId;
    objectsRepositoryService.processQuery(query, (rs) -> {
      assertThat(rs.next()).isTrue();
      var semanticType = rs.getString("semantic_type");
      assertThat(semanticType).isEqualTo("folia.token");
      assertThat(rs.next()).isFalse();
    });
  }

  private static String deploymentIsFinished(String workDir, String testFileName) {
    logger.info(format("check deployment [%s] is finished", workDir));
    var statusResponse = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    return getOutputFilePath(statusResponse, testFileName);
  }

  private static String getOutputFilePath(HttpResponse<String> finishedDeployment, String testFileName) {
    String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
    var pathAbsolute = Paths.get(outputDir);
    var pathBase = Paths.get("admin/files/");
    var pathRelative = pathBase.relativize(pathAbsolute);
    var resultFileName = testFileName.replace(".txt", ".xml");
    var outputPath = Paths.get(pathRelative.toString(), resultFileName).toString();
    logger.info(format("output file path is [%s]", outputPath));
    return outputPath;
  }

  private static boolean resultFileCanBeDownloaded(String inputFile, String someContent) {
    logger.info(format("check file [%s] can be downloaded", inputFile));
    var downloadResult = downloadFile(inputFile);
    return downloadResult.getBody().contains(someContent)
      && downloadResult.getStatus() == 200;
  }

  private String canCreateAndRunUctoProject(long inputFileId) throws UnirestException {
    var result = Unirest
      .post(Config.SWITCHBOARD_ENDPOINT + "/exec/UCTO")
      .header("Content-Type", "application/json; charset=UTF-8")
      .body("{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + inputFileId + "\"," +
        "\"params\":[{\"name\":\"language\",\"type\":\"enumeration\",\"value\":\"eng\"},{\"name\":\"author\"," +
        "\"type\":\"string\",\"value\":\"Willem S\"}]}],\"valid\":true}")
      .asString();
    assertThat(result.getStatus()).isIn(200, 201, 202);
    String workDir = JsonPath.parse(result.getBody()).read("$.workDir");

    logger.info(format("deployment has workdir [%s]", workDir));

    return workDir;
  }

}
