package nl.knaw.meertens.clariah.vre.recognizer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;

public class KafkaConsumerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String topic;
    private final KafkaConsumer<String, String> consumer;

    KafkaConsumerService(String server, String topic, String group) {
        this.topic = topic;
        consumer = configureConsumer(server, group);
    }

    private KafkaConsumer<String, String> configureConsumer(String server, String groupName) {
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("group.id", "vre_recognizer");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<>(props);
    }

    public void consumeWith(Consumer<String> consumerFunction) {
        consumer.subscribe(singletonList(topic));
        logger.info("Subscribed to topic " + topic);
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                consumeRecordWith(record, consumerFunction);
            }
        }
    }

    private void consumeRecordWith(ConsumerRecord<String, String> record, Consumer<String> func) {
        logger.info("consume dto ["
            + "offset: " + record.offset() + "; "
            + "key: " + record.key() + "; "
            + "value; " + record.value() + "]"
        );
        try {
            func.accept(record.value());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}