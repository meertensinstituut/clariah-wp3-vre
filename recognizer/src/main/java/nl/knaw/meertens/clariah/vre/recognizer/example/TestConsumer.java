package nl.knaw.meertens.clariah.vre.recognizer.example;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Properties;

import static java.util.Collections.singletonList;

public class TestConsumer {

    public static void consume(String server, String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("group.id", "vre_recognizer_test");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(singletonList(topic));
        System.out.println("Subscribed to topic " + topic);
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("consume dto [offset: %d; key: %s; value; %s]\n",
                        record.offset(), record.key(), record.value());
            }
        }
    }

}