package nl.knaw.meertens.clariah.vre.switchboard.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public abstract class KafkaDeploymentDto {

    @JsonFormat(shape=STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
    public LocalDateTime dateTime;

    public String service;

}
