package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.ObjectPath;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaNextcloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class FinishServiceDeploymentConsumer extends AbstractDeploymentConsumer {

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
  void handleFinish(DeploymentStatusReport report) {
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
      report.setOutputDir(outputFiles
        .get(0)
        .toPath()
        .getParent()
        .toString()
      );
    }

    report.setWorkDir(report.getWorkDir());
    var paths = outputFiles
      .stream()
      .map(ObjectPath::toPath)
      .collect(toList());
    sendKafkaNextcloudMsgs(paths);
  }

  @Override
  void handleStop(DeploymentStatusReport report) {
    throw new UnsupportedOperationException("Service of type service cannot be stopped");
  }

  private void sendKafkaNextcloudMsgs(
    List<Path> outputFiles
  ) {
    for (var file : outputFiles) {
      var msg = new KafkaNextcloudCreateFileDto();
      msg.action = "create";
      msg.path = file.toString();
      msg.timestamp = new Timestamp(System.currentTimeMillis()).getTime();
      msg.user = file.getName(0).toString();
      kafkaNextcloudService.send(msg);
    }
  }


}
