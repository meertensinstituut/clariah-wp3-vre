package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;

public class FinishViewerDeploymentConsumer extends FinishDeploymentConsumer {

  private FileService nextcloudFileService;

  public FinishViewerDeploymentConsumer(
    FileService nextcloudFileService,
    KafkaProducerService kafkaSwitchboardService
  ) {
    super(kafkaSwitchboardService);
    this.nextcloudFileService = nextcloudFileService;
  }

  @Override
  void handleFinishedDeployment(DeploymentStatusReport report) {
    nextcloudFileService.unstage(report.getWorkDir(), report.getFiles());

    var viewerFile = nextcloudFileService.unstageViewerOutputFile(
      report.getWorkDir(),
      report.getFiles().get(0),
      report.getService()
    );
    report.setViewerFile(viewerFile.toString());
    report.setViewerFileContent(nextcloudFileService.getContent(viewerFile.toString()));
    report.setWorkDir(report.getWorkDir());
  }

}
