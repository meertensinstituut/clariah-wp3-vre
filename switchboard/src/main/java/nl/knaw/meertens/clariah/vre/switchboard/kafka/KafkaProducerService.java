package nl.knaw.meertens.clariah.vre.switchboard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KafkaProducerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String topic;

    private SwitchboardKafkaProducer producer;
    private ObjectMapper mapper;

    public KafkaProducerService(String topic, String server, ObjectMapper mapper) {
        this.topic = topic;
        this.producer = new SwitchboardKafkaProducer(server);
        this.mapper = mapper;
    }

    public void send(KafkaDto kafkaMsg) throws IOException {
        producer.getKafkaProducer().send(new ProducerRecord<>(
                topic, "key", mapper.writeValueAsString(kafkaMsg)
        ));
        logger.info("Message sent successfully");
    }

}

