package nl.knaw.meertens.clariah.vre.switchboard.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String topic;

    private SwitchboardKafkaProducer producer;
    private ObjectMapper mapper;

    public KafkaProducerServiceImpl(String topic, String server, ObjectMapper mapper) {
        this.topic = topic;
        this.producer = new SwitchboardKafkaProducer(server);
        this.mapper = mapper;
    }

    @Override
    public void send(KafkaDto kafkaMsg) {
        try {
            producer.getKafkaProducer().send(new ProducerRecord<>(
                    topic, "key", mapper.writeValueAsString(kafkaMsg)
            ));
            logger.info(String.format("Kafka message sent successfully to [%s]", topic));
        } catch (JsonProcessingException e) {
            logger.error("Could not create json for kafka msg", e);
        }
    }

}
