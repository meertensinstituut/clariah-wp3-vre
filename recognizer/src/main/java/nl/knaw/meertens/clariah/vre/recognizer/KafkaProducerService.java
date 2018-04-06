package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.recognizer.dto.RecognizerKafkaDTO;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.IdentificationType.Identity;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class KafkaProducerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String topic;

    private RecognizerKafkaProducer producer;
    private ObjectMapper mapper = new ObjectMapper();

    KafkaProducerService(RecognizerKafkaProducer recognizerKafkaProducer, String topic) {
        this.topic = topic;
        producer = recognizerKafkaProducer;
    }

    public void produceToRecognizerTopic(Report report) throws IOException {
        RecognizerKafkaDTO kafkaMsg = createKafkaMsg(report);
        producer.getKafkaProducer().send(new ProducerRecord<>(topic, "key", mapper.writeValueAsString(kafkaMsg)));
        logger.info("Message sent successfully");
    }

    private RecognizerKafkaDTO createKafkaMsg(Report report) {
        RecognizerKafkaDTO result = new RecognizerKafkaDTO();
        Identity identity = report.getFits().getIdentification().getIdentity().get(0);
        result.objectId = report.getObjectId();
        result.fitsFormat = identity.getFormat();
        result.fitsMimetype = identity.getMimetype();
        result.path = report.getPath();
        result.fitsFullResult = report.getXml();
        return result;
    }

}
