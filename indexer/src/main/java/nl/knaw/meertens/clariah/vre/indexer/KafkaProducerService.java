package nl.knaw.meertens.clariah.vre.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knaw.meertens.clariah.vre.indexer.dto.IndexerKafkaDTO;
import nl.knaw.meertens.clariah.vre.indexer.dto.ObjectsRecordDTO;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KafkaProducerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String topic;

    private IndexerKafkaProducer producer;
    private ObjectMapper mapper = new ObjectMapper();

    KafkaProducerService(IndexerKafkaProducer recognizerKafkaProducer, String topic) {
        this.topic = topic;
        producer = recognizerKafkaProducer;
    }

    public void produceToIndexerTopic(ObjectsRecordDTO record, String action) throws IOException {
        IndexerKafkaDTO kafkaMsg = createKafkaMsg(record, action);
        producer.getKafkaProducer().send(new ProducerRecord<>(topic, "key", mapper.writeValueAsString(kafkaMsg)));
        logger.info("Message sent successfully");
    }

    private IndexerKafkaDTO createKafkaMsg(ObjectsRecordDTO record, String action) {
        IndexerKafkaDTO result = new IndexerKafkaDTO();
        result.file = record.filepath;
        result.user = record.user_id;
        result.object_id = record.id;
        result.created = record.time_created;
        result.action = action;
        return result;
    }

}
