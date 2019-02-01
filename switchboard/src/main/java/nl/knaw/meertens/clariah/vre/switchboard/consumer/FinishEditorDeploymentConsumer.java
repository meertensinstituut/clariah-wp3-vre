package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.NextcloudInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaNextcloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;

import java.nio.file.Path;
import java.sql.Timestamp;

public class FinishEditorDeploymentConsumer extends FinishDeploymentConsumer {

  private final KafkaProducerService kafkaNextcloudService;
  private FileService nextcloudFileService;

  public FinishEditorDeploymentConsumer(
    FileService nextcloudFileService,
    KafkaProducerService kafkaSwitchboardService,
    KafkaProducerService kafkaNextcloudService
  ) {
    super(kafkaSwitchboardService);
    this.nextcloudFileService = nextcloudFileService;
    this.kafkaNextcloudService = kafkaNextcloudService;
  }

  @Override
  void handleFinish(DeploymentStatusReport report) {
    var content = nextcloudFileService.getDeployContent(
      report.getWorkDir(),
      report.getFiles().get(0)
    );
    report.setViewerFileContent(content);
    report.setWorkDir(report.getWorkDir());
  }

  @Override
  void handleStop(DeploymentStatusReport report) {
    var inputObjectPath = report.getFiles().get(0);
    var fromPath = DeploymentInputFile
      .from(report.getWorkDir(), inputObjectPath);
    var toPath = NextcloudInputFile
      .from(inputObjectPath);

    nextcloudFileService.moveFile(fromPath, toPath);
    nextcloudFileService.unlock(inputObjectPath);
    sendKafkaNextcloudUpdateMsg(toPath.toPath());
  }

  private void sendKafkaNextcloudUpdateMsg(Path inputFile) {
    var msg = new KafkaNextcloudCreateFileDto();
    msg.action = "update";
    msg.path = inputFile.toString();
    msg.timestamp = new Timestamp(System.currentTimeMillis()).getTime();
    msg.user = inputFile.getName(0).toString();
    kafkaNextcloudService.send(msg);
  }

}
