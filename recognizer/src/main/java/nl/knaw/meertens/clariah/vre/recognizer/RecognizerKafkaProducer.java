package nl.knaw.meertens.clariah.vre.recognizer;

import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Properties;

/**
 * KafkaProducer configured with Recognizer settings
 */
public class RecognizerKafkaProducer {

    private final KafkaProducer<String, String> kafkaProducer;

    public RecognizerKafkaProducer(String server) {
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("advertised.host.name", "kafka");
        props.put("advertised.port", 9092);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(props);
    }

    public KafkaProducer<String, String> getKafkaProducer() {
        return kafkaProducer;
    }

}
