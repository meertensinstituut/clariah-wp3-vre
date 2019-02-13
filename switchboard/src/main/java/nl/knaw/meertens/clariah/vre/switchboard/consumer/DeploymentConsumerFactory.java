package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind;

import java.util.HashMap;
import java.util.Map;

public class DeploymentConsumerFactory {


  private Map<ServiceKind, DeploymentConsumer> deploymentConsumer = new HashMap<>();

  public DeploymentConsumerFactory(
    FileService nextcloudFileService,
    KafkaProducerService kafkaSwitchboardService,
    KafkaProducerService kafkaNextcloudService
  ) {

    this.deploymentConsumer.put(
      ServiceKind.EDITOR,
      new FinishEditorDeploymentConsumer(
        nextcloudFileService,
        kafkaSwitchboardService,
        kafkaNextcloudService
      )
    );

    this.deploymentConsumer.put(
      ServiceKind.VIEWER,
      new FinishViewerDeploymentConsumer(
        nextcloudFileService,
        kafkaSwitchboardService
      )
    );

    this.deploymentConsumer.put(
      ServiceKind.SERVICE,
      new FinishServiceDeploymentConsumer(
        nextcloudFileService,
        kafkaSwitchboardService,
        kafkaNextcloudService
      )
    );
  }

  public DeploymentConsumer get(ServiceKind kind) {
    return deploymentConsumer.get(kind);
  }

}
