package nl.knaw.meertens.clariah.vre.recognizer.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.recognizer.Report;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.IdentificationType.Identity;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.util.Objects.isNull;

public class KafkaProducerService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String topic;

  private RecognizerKafkaProducer producer;
  private ObjectMapper mapper;

  public KafkaProducerService(RecognizerKafkaProducer recognizerKafkaProducer, String topic) {
    this.topic = topic;
    producer = recognizerKafkaProducer;

    mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public void produceToRecognizerTopic(Report report) throws IOException {
    RecognizerKafkaDto kafkaMsg = createKafkaMsg(report);
    producer.getKafkaProducer().send(new ProducerRecord<>(topic, "key", mapper.writeValueAsString(kafkaMsg)));
    logger.info("Message sent successfully");
  }

  private RecognizerKafkaDto createKafkaMsg(Report report) {
    RecognizerKafkaDto result = new RecognizerKafkaDto();
    result.objectId = report.getObjectId();
    result.path = report.getPath();
    result.action = report.getAction();
    result.oldPath = report.getOldPath();
    if (!isNull(report.getXml())) {
      result.fitsFullResult = report.getXml();
      Identity identity = report.getFits().getIdentification().getIdentity().get(0);
      result.fitsFormat = identity.getFormat();
      result.fitsMimetype = identity.getMimetype();
    }
    return result;
  }

}
