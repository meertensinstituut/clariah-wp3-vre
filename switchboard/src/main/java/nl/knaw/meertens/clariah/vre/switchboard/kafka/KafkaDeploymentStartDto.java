package nl.knaw.meertens.clariah.vre.switchboard.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class KafkaDeploymentStartDto extends KafkaDeploymentDto {

    public String deploymentRequest;

    public String workDir;
}
