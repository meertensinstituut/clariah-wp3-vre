package nl.knaw.meertens.clariah.vre.stress;

import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.AbstractIntegrationTest;
import nl.knaw.meertens.clariah.vre.integration.Config;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.between;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDateTime.now;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentWithStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.getOutputFilePath;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startDeployment;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileInNextcloudHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileInNextcloudContainsContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGetFor;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitCheckFor;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_MINUTE;

public class LargeFileTest extends AbstractIntegrationTest {

  private String uctoServicename = "UCTO";

  private String uctoConfig = "{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + "%s" + "\"," +
    "\"params\":[{\"name\":\"language\",\"type\":\"enumeration\",\"value\":\"eng\"},{\"name\":\"author\"," +
    "\"type\":\"string\",\"value\":\"Willem S\"}]}],\"valid\":true}";

  private String resultFoliaPart = "" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.2\" class=\"WORD\" space=\"no\">\n" +
    "          <t>O</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.3\" class=\"PUNCTUATION\">\n" +
    "          <t>,</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.4\" class=\"WORD\">\n" +
    "          <t>spreek</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.5\" class=\"WORD\">\n" +
    "          <t>niet</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.6\" class=\"WORD\">\n" +
    "          <t>over</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.7\" class=\"WORD\">\n" +
    "          <t>een</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.8\" class=\"WORD\" space=\"no\">\n" +
    "          <t>noodlot</t>\n" +
    "        </w>\n" +
    "        <w xml:id=\"untitled.p.4364.s.1.w.9\" class=\"PUNCTUATION\">\n" +
    "          <t>.</t>\n" +
    "        </w>";

  private static Logger logger = LoggerFactory.getLogger(LargeFileTest.class);

  private ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
    Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
  );


  @Test
  public void testElineVereTxtCanBeConvertedToFolia() throws UnirestException {
    var startTime = now();

    var resourceFileName = "eline-vere.txt";
    var testFileContent = getTestFileContent(resourceFileName, LargeFileTest.class);
    var testFilename = uploadTestFile(testFileContent);

    await().until(() -> fileInNextcloudHasContent(testFilename, testFileContent));
    await().until(() -> fileExistsInRegistry(testFilename, "text/plain", "Plain text"));
    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(format("input file has object id [%d]", inputFileId));

    var workDir = startDeployment(uctoServicename, format(uctoConfig, inputFileId));

    var statusResponse = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    var resultFile = getOutputFilePath(statusResponse, testFilename.replace(".txt", ".xml"));

    awaitOcc();

    await().atMost(ONE_MINUTE).until(() -> fileInNextcloudContainsContent(resultFile, resultFoliaPart));

    // semantic types:
    long resultFileId = awaitAndGetFor(() -> getNonNullObjectIdFromRegistry(resultFile), ofMinutes(2));
    var query = "select * from object_semantic_type where object_id = " + resultFileId;
    awaitCheckFor(() -> {
      try {
        objectsRepositoryService.processQuery(query, (rs) -> {
          assertThat(rs.next()).isTrue();
          assertThat(rs.getString("semantic_type")).isEqualTo("folia.token");
        });
      } catch (SQLException e) {
        throw new RuntimeException("Could not get object from registry");
      }
    }, ofMinutes(1));

    var endTime = now();
    var duration = between(startTime, endTime);

    var txtFileSize = byteCountToDisplaySize(testFileContent.getBytes(UTF_8).length);

    var downloadResult = downloadFile(resultFile);
    var downloadSize = byteCountToDisplaySize(downloadResult.getBody().getBytes(UTF_8).length);

    logger.info(format(
      "Creating FoLiA [%s] from [%s, %s] took [%d] seconds",
      downloadSize, resourceFileName, txtFileSize, duration.getSeconds()
    ));

  }

}
