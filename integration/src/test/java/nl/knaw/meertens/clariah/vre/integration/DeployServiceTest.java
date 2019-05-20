package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentHasStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.filesAreUnlocked;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.resultWhenDeploymentFinished;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startTestDeploymentWithInputFileId;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileInNextcloudHasContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileIsLocked;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.newObjectIsAdded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getNonNullObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class DeployServiceTest extends AbstractIntegrationTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private KafkaConsumerService nextcloudKafkaConsumer;

  private String deploymentTestFile = "deployment-test.txt";
  private long id;

  @Before
  public void setup() {
    nextcloudKafkaConsumer = getNextcloudTopic();
  }

  @Test
  public void testDeployment_locksFiles_movesOutput_unlocksFiles() throws Exception {
    var testFileContent = getTestFileContent(deploymentTestFile);
    var testFilename = uploadTestFile(testFileContent);

    await().until(() -> fileInNextcloudHasContent(testFilename, testFileContent));
    await().until(() -> fileExistsInRegistry(testFilename, "text/plain", "Plain text"));
    long inputFileId = awaitAndGet(() -> getNonNullObjectIdFromRegistry(testFilename));
    logger.info(String.format("input file has object id [%d]", inputFileId));

    var workDir = startTestDeploymentWithInputFileId(inputFileId);
    logger.info(String.format("deployment has workdir [%s]", workDir));

    await().until(() -> deploymentHasStatus(workDir, "RUNNING"));

    awaitOcc();

    await().until(() -> fileInNextcloudHasContent(testFilename, testFileContent));
    await().until(() -> fileIsLocked(testFilename));

    var newInputFile = uploadTestFile(testFileContent);

    await().until(() -> newObjectIsAdded(newInputFile));

    var resultFile = awaitAndGet(() -> resultWhenDeploymentFinished(workDir));

    await().until(() -> fileInNextcloudHasContent(resultFile, getTestFileContent("test-result.txt")));

    awaitOcc();

    await().until(() -> filesAreUnlocked(testFilename, getTestFileContent(deploymentTestFile)));

    checkKafkaMsgsAreCreatedForOutputFiles(resultFile);

    var secondNewInputFile = uploadTestFile(testFileContent);

    await().until(() -> newObjectIsAdded(secondNewInputFile));

  }

  private void checkKafkaMsgsAreCreatedForOutputFiles(String outputFilename) {
    logger.info(String.format("check kafka message is created for output file [%s]", outputFilename));
    nextcloudKafkaConsumer.consumeAll(consumerRecords -> {
      assertThat(consumerRecords.size()).isGreaterThan(0);
      List<String> resultActions = new ArrayList<>();
      consumerRecords.forEach(record -> {
        String filePath = JsonPath.parse(record.value()).read("$.path");
        if (filePath.contains(outputFilename)) {
          resultActions.add(JsonPath.parse(record.value()).read("$.action"));
        }
      });
      assertThat(resultActions).hasSize(1);
      assertThat(resultActions.get(0)).isEqualTo("create");
    });
  }

  private KafkaConsumerService getNextcloudTopic() {
    var recognizerKafkaConsumer = new KafkaConsumerService(
      Config.KAFKA_ENDPOINT, Config.NEXTCLOUD_TOPIC_NAME, getRandomGroupName()
    );
    recognizerKafkaConsumer.subscribe();
    recognizerKafkaConsumer.pollOnce();
    return recognizerKafkaConsumer;
  }

}
