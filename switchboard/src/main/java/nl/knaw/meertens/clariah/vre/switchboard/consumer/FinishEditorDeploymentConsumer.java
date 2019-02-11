package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentConfigFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentTmpFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.NextcloudInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaNextcloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamService;

import java.nio.file.Path;
import java.sql.Timestamp;

import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.EDITOR_TMP;

public class FinishEditorDeploymentConsumer extends AbstractDeploymentConsumer {

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
      EDITOR_TMP
    );
    report.setViewerFileContent(content);
    report.setWorkDir(report.getWorkDir());
  }

  @Override
  void handleStop(DeploymentStatusReport report) {
    var config = DeploymentConfigFile
      .from(report.getWorkDir())
      .getConfig();
    var editorOutputPath = ParamService
      .getConfigParamByName(config.params, "output");
    var fromPath = DeploymentTmpFile
      .from(report.getWorkDir(), editorOutputPath);
    var toPath = NextcloudInputFile
      .from(report.getFiles().get(0));

    nextcloudFileService.moveFile(fromPath, toPath);
    nextcloudFileService.unlock(toPath.toObjectPath());
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
