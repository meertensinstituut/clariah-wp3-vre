package nl.knaw.meertens.clariah.vre.example;

import java.util.Properties;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class TestProducer {

    public static void produce(String kafkaServer, String owncloudTopic) {
        Properties props = new Properties();

        props.put("bootstrap.servers", kafkaServer);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        Producer<String, String> producer = new KafkaProducer<>(props);
        String msg = "{\"action\":\"read\",\"user\":\"admin\",\"path\":\"\\/test.txt\",\"userPath\":\"\\/test.txt\",\"oldPath\":null,\"newPath\":null,\"timestamp\":1516287994}";
        producer.send(new ProducerRecord<>(owncloudTopic, "key", msg));
        System.out.println("Message sent successfully");
        producer.close();
    }
}