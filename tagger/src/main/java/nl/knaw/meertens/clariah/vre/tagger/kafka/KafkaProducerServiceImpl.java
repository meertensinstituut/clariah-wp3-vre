package nl.knaw.meertens.clariah.vre.tagger.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String topic;

    private KafkaProducer<String, String> producer;
    private ObjectMapper mapper;

    public KafkaProducerServiceImpl(String topic, String server, ObjectMapper mapper) {
        this.topic = topic;
        this.mapper = mapper;

        var props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("advertised.host.name", "kafka");
        props.put("advertised.port", 9092);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(props);
    }

    @Override
    public void send(TaggerKafkaDto kafkaMsg) {
        try {
            producer.send(new ProducerRecord<>(topic, "new-object-tag", mapper.writeValueAsString(kafkaMsg)));
            logger.info(String.format("Kafka message sent successfully to [%s]", topic));
        } catch (JsonProcessingException e) {
            logger.error("Could not create json of kafka msg", e);
        }
    }

}
