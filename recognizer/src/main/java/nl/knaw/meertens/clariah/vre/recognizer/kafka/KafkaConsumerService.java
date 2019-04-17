package nl.knaw.meertens.clariah.vre.recognizer.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

public class KafkaConsumerService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final KafkaConsumer<String, String> consumer;
  private final String topic;
  private final int kafkaMaxAtempts = 60;

  public KafkaConsumerService(String server, String topic, String group) {
    this.consumer = configureConsumer(server, group);
    this.topic = topic;
  }

  private KafkaConsumer<String, String> configureConsumer(String server, String groupName) {
    var props = new Properties();
    props.put("bootstrap.servers", server);
    props.put("advertised.host.name", "kafka");
    props.put("advertised.port", 9092);
    props.put("group.id", groupName);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

    var tenMinutes = 10 * 60 * 1000;
    props.put("session.timeout.ms", "" + tenMinutes);

    // should be greater than session timeout:
    props.put("request.timeout.ms", "" + (tenMinutes + 1));

    return new KafkaConsumer<>(props);
  }

  public void consumeWith(Consumer<String> consumerFunction) {
    consumer.subscribe(singletonList(topic));
    logger.info(format("Subscribed to topic [%s]", topic));
    while (true) {
      logger.info(format("Polling topic [%s]", topic));
      var records = consumer.poll(Duration.ofSeconds(1));
      for (var record : records) {
        consumeRecordWith(record, consumerFunction);
      }
    }
  }

  private void consumeRecordWith(ConsumerRecord<String, String> record, Consumer<String> func) {
    logger.info(format(
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
