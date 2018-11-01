package nl.knaw.meertens.clariah.vre.integration.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

public class KafkaConsumerService {

    private final String topic;
    private final KafkaConsumer<String, String> consumer;
    private Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    public KafkaConsumerService(String server, String topic, String group) {
        this.topic = topic;
        consumer = configureConsumer(server, group);
    }

    private KafkaConsumer<String, String> configureConsumer(String server, String groupName) {
        Properties props = new Properties();
        props.put("bootstrap.servers", server);
        props.put("group.id", groupName);
        props.put("client.id", groupName);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<>(props);
    }

    public void consumeAll(Consumer<ArrayList<ConsumerRecord<String, String>>> function) throws InterruptedException {
        logger.info("Start consuming all msgs from topic " + topic);
        ArrayList<ConsumerRecord<String, String>> list = newArrayList();

        int pollingPeriod = 5;
        for(int i = 0; i < pollingPeriod; i++) {
            TimeUnit.SECONDS.sleep(1);
            ConsumerRecords<String, String> result = consumer.poll(100);
            list.addAll(newArrayList(result));
            logger.info("Found " + result.count() + " new msgs from " + topic);
        }
        logger.info("Finished consuming msgs from " + topic);
        if(list.size() != 0) {
            function.accept(list);
        }
    }

    public void subscribe() {
        logger.info("Subscribe to topic " + topic);
        consumer.subscribe(singletonList(topic));
    }

    public void pollOnce() {
        logger.info("Poll topic " + topic);
        consumer.poll(100);
    }
}