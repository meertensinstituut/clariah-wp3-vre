package nl.knaw.meertens.clariah.vre.integration.util;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.pollAndAssert;

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

    public void consumeAll(Consumer<ArrayList<ConsumerRecord<String, String>>> function) {
        logger.info("Start consuming all msgs from topic " + topic);
        ArrayList<ConsumerRecord<String, String>> list = newArrayList();
        pollAndAssert(() -> findNewMessagesAndCheckIfAny(function, list));
    }

    private void findNewMessagesAndCheckIfAny(
            Consumer<ArrayList<ConsumerRecord<String, String>>> function,
            ArrayList<ConsumerRecord<String, String>> list
    ) {
        findNewMessages(list);
        logger.info(String.format("Finished consuming msgs from topic [%s]", topic));
        if (list.size() > 0) {
            function.accept(list);
        } else {
            throw new AssertionError(String.format("No kafka msgs found in topic [%s]", topic));
        }
    }

    public void findNewMessages(ArrayList<ConsumerRecord<String, String>> list) {
        ConsumerRecords<String, String> result = consumer.poll(Duration.ofSeconds(1));
        list.addAll(newArrayList(result));
        logger.info("Add " + result.count() + " from " + topic + " to result list with " + list.size() + " new kafka msgs");
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