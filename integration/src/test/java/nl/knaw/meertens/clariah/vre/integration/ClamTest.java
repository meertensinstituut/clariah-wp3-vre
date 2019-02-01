package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentWithStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ClamTest extends AbstractIntegrationTest {
  private static Logger logger = LoggerFactory.getLogger(ClamTest.class);

  @Test
  public void canRunUctoProject() throws UnirestException {
    String deploymentTestFile = "deployment-test.txt";
    String testFileContent = getTestFileContent(deploymentTestFile);
    String testFilename = uploadTestFile(testFileContent);

    await().until(() -> fileHasContent(testFilename, testFileContent));
    await().until(() -> fileExistsInRegistry(testFilename));
    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(String.format("input file has object id [%d]", inputFileId));

    String workDir = canCreateAndRunUctoProject(inputFileId);
    logger.info(String.format("workDir is [%s]", workDir));
    String resultFile = awaitAndGet(() -> deploymentIsFinished(workDir, testFilename));

    awaitOcc();
    String resultFileContent = "<w xml:id=\"untitled.p.1.s.1.w.1\" class=\"WORD\">\n" +
        "          <t>En</t>\n" +
        "        </w>";

    logger.info(String.format("result file [%s] has content [%s]", resultFile, resultFileContent));
    await().until(() -> resultFileCanBeDownloaded(resultFile, resultFileContent));
  }

  private String canCreateAndRunUctoProject(long inputFileId) throws UnirestException {
    HttpResponse<String> result = Unirest
        .post(Config.SWITCHBOARD_ENDPOINT + "/exec/UCTO")
        .header("Content-Type", "application/json; charset=UTF-8")
        .body("{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + inputFileId + "\"," +
            "\"params\":[{\"name\":\"language\",\"type\":\"enumeration\",\"value\":\"eng\"},{\"name\":\"author\"," +
            "\"type\":\"string\",\"value\":\"Willem S\"}]}],\"valid\":true}")
        .asString();
    assertThat(result.getStatus()).isIn(200, 201, 202);
    String workDir = JsonPath.parse(result.getBody()).read("$.workDir");

    logger.info(String.format("deployment has workdir [%s]", workDir));

    return workDir;
  }

  private static String deploymentIsFinished(String workDir, String testFileName) {
    logger.info(String.format("check deployment [%s] is finished", workDir));
    HttpResponse<String> statusResponse = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    String outputFilePath = getOutputFilePath(statusResponse, testFileName);
    logger.info(String.format("deployment has result file [%s]", outputFilePath));
    return outputFilePath;
  }

  private static String getOutputFilePath(HttpResponse<String> finishedDeployment, String testFileName) {
    String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
    Path pathAbsolute = Paths.get(outputDir);
    Path pathBase = Paths.get("admin/files/");
    Path pathRelative = pathBase.relativize(pathAbsolute);
    String resultFileName = testFileName.replace(".txt", ".xml");
    String outputPath = Paths.get(pathRelative.toString(), resultFileName).toString();
    logger.info(String.format("output file path is [%s]", outputPath));
    return outputPath;
  }

  private static boolean resultFileCanBeDownloaded(String inputFile, String someContent) {
    logger.info(String.format("check file [%s] can be downloaded", inputFile));
    HttpResponse<String> downloadResult = downloadFile(inputFile);
    return downloadResult.getBody().contains(someContent)
      && downloadResult.getStatus() == 200;
  }

}
