package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentResultDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaOwncloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.STOPPED;
import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.fromKind;

public class FinishDeploymentConsumer implements PollDeploymentConsumer {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private ServicesRegistryService serviceRegistryService;
  private FileService nextcloudFileService;
  private KafkaProducerService kafkaSwitchboardService;
  private KafkaProducerService kafkaOwncloudService;

  public FinishDeploymentConsumer(
    ServicesRegistryService servicesRegistryService,
    FileService nextcloudFileService,
    KafkaProducerService kafkaSwitchboardService,
    KafkaProducerService kafkaNextcloudService
  ) {
    this.serviceRegistryService = servicesRegistryService;
    this.nextcloudFileService = nextcloudFileService;
    this.kafkaSwitchboardService = kafkaSwitchboardService;
    this.kafkaOwncloudService = kafkaNextcloudService;
  }

  @Override
  public void accept(DeploymentStatusReport report) {
    logger.info(String.format("Status of [%s] is [%s]", report.getWorkDir(), report.getStatus()));
    if (isFinishedOrStopped(report)) {
      completeDeployment(report);
    }
  }

  private boolean isFinishedOrStopped(DeploymentStatusReport report) {
    return report.getStatus() == FINISHED || report.getStatus() == STOPPED;
  }

  private void completeDeployment(DeploymentStatusReport report) {

    var service = serviceRegistryService.getServiceByName(report.getService());
    var serviceKind = fromKind(service.getKind());

    nextcloudFileService.unstage(report.getWorkDir(), report.getFiles());

    switch (serviceKind) {
      case SERVICE:
        completeServiceDeployment(report);
        break;
      case VIEWER:
        completeViewerDeployment(report);
        break;
      case EDITOR:
        completeEditorDeployment(report);
        break;
      default:
        throw new UnsupportedOperationException(String.format(
          "Could not complete deployment: unknown kind [%s]", serviceKind
        ));
    }

    sendKafkaSwitchboardMsg(report);

    logger.info(String.format(
      "Completed deployment of service [%s] with workdir [%s]",
      report.getService(),
      report.getWorkDir()
    ));
  }

  private void completeServiceDeployment(
    DeploymentStatusReport report
  ) {
    var outputFiles = nextcloudFileService.unstageServiceOutputFiles(
      report.getWorkDir(),
      report.getFiles().get(0)
    );
    if (outputFiles.isEmpty()) {
      logger.warn(String
        .format("Deployment [%s] with service [%s] did not produce any output files", report.getWorkDir(),
          report.getService()));
    } else {
      report.setOutputDir(outputFiles.get(0).getParent().toString());
    }
    report.setWorkDir(report.getWorkDir());
    sendKafkaOwncloudMsgs(outputFiles);
  }

  private void completeViewerDeployment(
    DeploymentStatusReport report
  ) {
    var viewerFile = nextcloudFileService.unstageViewerOutputFile(
      report.getWorkDir(),
      report.getFiles().get(0),
      report.getService()
    );
    report.setViewerFile(viewerFile.toString());
    report.setViewerFileContent(nextcloudFileService.getContent(viewerFile.toString()));
    report.setWorkDir(report.getWorkDir());
  }

  private void completeEditorDeployment(
    DeploymentStatusReport report
  ) {
    completeViewerDeployment(report);
  }

  private void sendKafkaSwitchboardMsg(
    DeploymentStatusReport report
  ) {
    var kafkaMsg = new KafkaDeploymentResultDto();
    kafkaMsg.service = report.getService();
    kafkaMsg.dateTime = LocalDateTime.now();
    kafkaMsg.status = report.getStatus();
    kafkaSwitchboardService.send(kafkaMsg);
  }

  private void sendKafkaOwncloudMsgs(
    List<Path> outputFiles
  ) {
    for (var file : outputFiles) {
      var msg = new KafkaOwncloudCreateFileDto();
      msg.action = "create";
      msg.path = file.toString();
      msg.timestamp = new Timestamp(System.currentTimeMillis()).getTime();
      msg.user = file.getName(0).toString();
      kafkaOwncloudService.send(msg);
    }
  }

}
