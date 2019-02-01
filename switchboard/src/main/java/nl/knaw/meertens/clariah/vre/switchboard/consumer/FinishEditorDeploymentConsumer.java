package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;

public class FinishEditorDeploymentConsumer extends FinishDeploymentConsumer {

  private FileService nextcloudFileService;

  public FinishEditorDeploymentConsumer(
    FileService nextcloudFileService,
    KafkaProducerService kafkaSwitchboardService
  ) {
    super(kafkaSwitchboardService);
    this.nextcloudFileService = nextcloudFileService;
  }

  @Override
  void handleFinish(DeploymentStatusReport report) {
    nextcloudFileService.unstage(report.getWorkDir(), report.getFiles());
    var toDisplay = nextcloudFileService.getContent(report.getFiles().get(0));
    report.setViewerFileContent(toDisplay);
    report.setWorkDir(report.getWorkDir());
  }

  @Override
  void handleStop(DeploymentStatusReport report) {
    // TODO:
    // - update edited file with content output file
    // - unlock edited file in nextcloud
    // - put update msg on nextcloud kafka log
  }

}
