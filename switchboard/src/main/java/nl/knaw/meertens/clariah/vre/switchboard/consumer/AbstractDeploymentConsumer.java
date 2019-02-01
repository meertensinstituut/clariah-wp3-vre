package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentResultDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.STOPPED;

/**
 * When a deployment is finished or stopped:
 * - run handleFinishDeployment()
 * - put result on kafka switchboard queue
 */
public abstract class AbstractDeploymentConsumer implements DeploymentConsumer {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private KafkaProducerService kafkaSwitchboardService;

  public AbstractDeploymentConsumer(KafkaProducerService kafkaSwitchboardService) {
    this.kafkaSwitchboardService = kafkaSwitchboardService;
  }

  abstract void handleFinish(DeploymentStatusReport report);

  abstract void handleStop(DeploymentStatusReport report);

  @Override
  public void accept(DeploymentStatusReport report) {
    if (!isFinishedOrStopped(report)) {
      logger.info(String.format("Status of [%s] is [%s]", report.getWorkDir(), report.getStatus()));
      return;
    }

    if (report.getStatus().equals(FINISHED)) {
      handleFinish(report);
    } else if (report.getStatus().equals(STOPPED)) {
      handleStop(report);
    }

    sendKafkaSwitchboardMsg(report);

    logger.info(String.format(
      "Handled deployment of [%s][%s] with status [%s]",
      report.getService(), report.getWorkDir(), report.getStatus()
    ));
  }

  private boolean isFinishedOrStopped(DeploymentStatusReport report) {
    return report.getStatus() == FINISHED || report.getStatus() == STOPPED;
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

}
