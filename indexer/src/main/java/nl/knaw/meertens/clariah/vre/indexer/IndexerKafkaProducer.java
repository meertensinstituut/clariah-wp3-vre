package nl.knaw.meertens.clariah.vre.indexer;

import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Properties;

/**
 * KafkaProducer configured with Recognizer settings
 */
public class IndexerKafkaProducer {

    private final KafkaProducer<String, String> kafkaProducer;

    public IndexerKafkaProducer(String server) {
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("advertised.host.name", "kafka");
        props.put("advertised.port", 9092);
//        props.put("acks", "all");
//        props.put("retries", 0);
//        props.put("batch.size", 16384);
//        props.put("linger.ms", 1);
//        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(props);
    }

    public KafkaProducer<String, String> getKafkaProducer() {
        return kafkaProducer;
    }

}
