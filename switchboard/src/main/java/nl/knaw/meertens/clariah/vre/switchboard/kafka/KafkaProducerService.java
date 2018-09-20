package nl.knaw.meertens.clariah.vre.switchboard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public interface KafkaProducerService {

    void send(KafkaDto kafkaMsg);

}

