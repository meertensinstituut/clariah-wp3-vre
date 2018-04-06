package nl.knaw.meertens.clariah.vre.switchboard.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Properties;

/**
 * KafkaProducer configured with Switchboard settings
 */
class SwitchboardKafkaProducer {

    private final KafkaProducer<String, String> kafkaProducer;

    public SwitchboardKafkaProducer(String server) {
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
