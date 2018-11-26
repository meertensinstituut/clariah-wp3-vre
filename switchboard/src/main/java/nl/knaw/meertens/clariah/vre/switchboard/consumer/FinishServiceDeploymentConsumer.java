package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaOwncloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;

public class FinishServiceDeploymentConsumer extends FinishDeploymentConsumer {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private final FileService nextcloudFileService;
  private KafkaProducerService kafkaNextcloudService;

  public FinishServiceDeploymentConsumer(
    FileService nextcloudFileService,
    KafkaProducerService kafkaSwitchboardService,
    KafkaProducerService kafkaNextcloudService
  ) {
    super(kafkaSwitchboardService);
    this.nextcloudFileService = nextcloudFileService;
    this.kafkaNextcloudService = kafkaNextcloudService;
  }

  @Override
  void handleFinishedDeployment(DeploymentStatusReport report) {
    nextcloudFileService.unstage(report.getWorkDir(), report.getFiles());

    var outputFiles = nextcloudFileService.unstageServiceOutputFiles(
      report.getWorkDir(),
      report.getFiles().get(0)
    );

    if (outputFiles.isEmpty()) {
      logger.warn(String.format(
        "Deployment [%s] with service [%s] did not produce any output files",
        report.getWorkDir(), report.getService()
      ));
    } else {
      report.setOutputDir(outputFiles.get(0).getParent().toString());
    }

    report.setWorkDir(report.getWorkDir());
    sendKafkaOwncloudMsgs(outputFiles);
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
      kafkaNextcloudService.send(msg);
    }
  }


}
