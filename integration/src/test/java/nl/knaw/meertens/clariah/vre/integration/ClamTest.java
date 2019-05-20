package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentWithStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.getOutputFilePath;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startDeployment;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileInNextcloudContainsContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileInNextcloudHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ClamTest extends AbstractIntegrationTest {

  private String uctoServicename = "UCTO";

  private String uctoConfig = "{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + "%s" + "\"," +
    "\"params\":[{\"name\":\"language\",\"type\":\"enumeration\",\"value\":\"eng\"},{\"name\":\"author\"," +
    "\"type\":\"string\",\"value\":\"Willem S\"}]}],\"valid\":true}";

  private static Logger logger = LoggerFactory.getLogger(ClamTest.class);

  private ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
    Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
  );

  @Test
  public void canRunUctoProject() throws UnirestException, SQLException {
    var resourceFileName = "deployment-test.txt";
    var testFileContent = getTestFileContent(resourceFileName);
    var testFilename = uploadTestFile(testFileContent);

    await().until(() -> fileInNextcloudHasContent(testFilename, testFileContent));
    await().until(() -> fileExistsInRegistry(testFilename, "text/plain", "Plain text"));
    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(format("input file has object id [%d]", inputFileId));

    var workDir = startDeployment(uctoServicename, format(uctoConfig, inputFileId));

    var statusResponse = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    var resultFile = getOutputFilePath(statusResponse, testFilename.replace(".txt", ".xml"));

    awaitOcc();
    var resultFileContent = "<w xml:id=\"untitled.p.1.s.1.w.1\" class=\"WORD\">\n" +
      "          <t>En</t>\n" +
      "        </w>";

    logger.info(format("Check result file [%s] has content [%s]", resultFile, resultFileContent));
    await().until(() -> fileInNextcloudContainsContent(resultFile, resultFileContent));

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

}
