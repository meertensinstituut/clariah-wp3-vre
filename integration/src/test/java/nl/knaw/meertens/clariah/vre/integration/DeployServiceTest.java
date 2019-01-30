package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.integration.util.Poller;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.filesAreUnlocked;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentHasStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.deploymentIsFinished;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startDeploymentWithInputFileId;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileCanBeDownloaded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileIsLocked;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.newObjectIsAdded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.awaitOcc;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;

public class DeployServiceTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private KafkaConsumerService nextcloudKafkaConsumer;

    private String deploymentTestFile = "deployment-test.txt";
    private long id;

    @Before
    public void setup() {
        nextcloudKafkaConsumer = getNextcloudTopic();
    }

    /**
     * Test switchboard and deployment-service before, during and after deployment of TEST-service.
     * For details of TEST-service, see Test-class in deployment-service
     */
    @Test
    public void testDeployment_locksFiles_movesOutput_unlocksFiles() throws Exception {
        String testFileContent = getTestFileContent(deploymentTestFile);
        String testFilename = uploadTestFile(testFileContent);

        Poller.awaitAndGet(() -> fileCanBeDownloaded(testFilename, testFileContent));
        Poller.awaitAndGet(() -> fileExistsInRegistry(testFilename));
        long inputFileId = Poller.awaitAndGet(() -> getObjectIdFromRegistry(testFilename));
        logger.info(String.format("input file has object id [%d]", inputFileId));

        String workDir = startDeploymentWithInputFileId(inputFileId);
        logger.info(String.format("deployment has workdir [%s]", workDir));

        Poller.awaitAndGet(() -> deploymentHasStatus(workDir, "RUNNING"));

        awaitOcc();

        Poller.awaitAndGet(() -> fileCanBeDownloaded(testFilename, testFileContent));
        awaitAndGet(() -> fileIsLocked(testFilename));

        String newInputFile = uploadTestFile(testFileContent);

        awaitAndGet(() -> newObjectIsAdded(newInputFile));

        String resultFile = Poller.awaitAndGet(() -> deploymentIsFinished(workDir));

        Poller.awaitAndGet(() -> fileCanBeDownloaded(resultFile, getTestFileContent("test-result.txt")));

        awaitAndGet(() -> filesAreUnlocked(testFilename, getTestFileContent(deploymentTestFile)));

        checkKafkaMsgsAreCreatedForOutputFiles(resultFile);

        String secondNewInputFile = uploadTestFile(testFileContent);

        awaitAndGet(() -> newObjectIsAdded(secondNewInputFile));

    }

    private void checkKafkaMsgsAreCreatedForOutputFiles(String outputFilename) throws InterruptedException {
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
        KafkaConsumerService recognizerKafkaConsumer = new KafkaConsumerService(
                Config.KAFKA_ENDPOINT, Config.NEXTCLOUD_TOPIC_NAME, getRandomGroupName());
        recognizerKafkaConsumer.subscribe();
        recognizerKafkaConsumer.pollOnce();
        return recognizerKafkaConsumer;
    }

}
