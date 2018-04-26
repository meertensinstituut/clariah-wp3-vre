package nl.knaw.meertens.clariah.vre.kafka;

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
    private final KafkaConsumer<String, String> consumer;
    private final String topic;

    public KafkaConsumerService(String server, String topic, String group) {
        this.consumer = configureConsumer(server, group);
        this.topic = topic;
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
        logger.info(String.format("Subscribed to topic [%s]", topic));
        while (true) {
            logger.info(String.format("Polling topic [%s]", topic));
            ConsumerRecords<String, String> records = consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                consumeRecordWith(record, consumerFunction);
            }
        }
    }

    private void consumeRecordWith(ConsumerRecord<String, String> record, Consumer<String> func) {
        logger.info(String.format(
                "consume dto [offset: %d; key: %s; value; %s]",
                record.offset(), record.key(), record.value()
        ));
        try {
            func.accept(record.value());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}