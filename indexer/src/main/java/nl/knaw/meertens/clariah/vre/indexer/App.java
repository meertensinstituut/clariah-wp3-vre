package nl.knaw.meertens.clariah.vre.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knaw.meertens.clariah.vre.indexer.dto.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.indexer.dto.RecognizerKafkaDTO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

  private static final Logger logger = LoggerFactory
      .getLogger(KafkaConsumerService.class);
  private static final String kafkaServer = "kafka:"
      + System.getenv("KAFKA_PORT");
  
  private static final String recognizerTopic = System
      .getenv("RECOGNIZER_TOPIC_NAME");
  private static final String indexerTopic = System
      .getenv("INDEXER_TOPIC_NAME");

  private static final String objectsDbUrl = "http://dreamfactory/api/v2/objects";
  private static final String objectsDbKey = System.getenv("APP_KEY_OBJECTS");
  private static final String objectsDbToken = System.getenv("OBJECTS_TOKEN");
  private static final String solrUrl = System.getenv("SOLR_URL");
  
  private static final String ACTION_CREATE = "create";
  
  
  private static final ObjectMapper objectMapper = new ObjectMapper();
  
  private static final KafkaConsumerService recognizerConsumerService = new KafkaConsumerService(
      kafkaServer, recognizerTopic, null);

  private static final KafkaProducerService kafkaProducer = new KafkaProducerService(
      new IndexerKafkaProducer(kafkaServer), indexerTopic);
  private static final ObjectsRepositoryService objectsRepository = new ObjectsRepositoryService(
      objectsDbUrl, objectsDbKey, objectsDbToken);
  private static final SolrService solr = new SolrService(solrUrl);
  
  public static void main(String[] args) {

    switch (args[0]) {
    case "consume":
      startConsuming();
      break;
    default:
      logger.info(
          "Run application with argument 'consume'.");
      break;
    }
  }

  private static void startConsuming() {
    consumeRecognizer();
  }

  private static void consumeRecognizer() {
    logger.info("Start consuming recognizer...");
    
    recognizerConsumerService.consumeWith((String json) -> {
      try {
        logger.info("parse "+json);
        RecognizerKafkaDTO msg = objectMapper.readValue(json,
            RecognizerKafkaDTO.class);
        if (StringUtils.isBlank(msg.path)) {
          throw new RuntimeException(
              "Field path is empty of: " + msg.toString());
        }
        logger.info("message read by indexer from topic "+indexerTopic+" : "+msg.objectId+" - "+msg.path);
        ObjectsRecordDTO record = objectsRepository.getRecordById(msg.objectId);
        if(record!=null) {
          solr.createDocument(record);
          kafkaProducer.produceToIndexerTopic(record, ACTION_CREATE);        
          logger.info("registered record "+record.id+" to kafka by indexer for "+record.user_id);
        } else {
          logger.info("could not get data from registry for "+msg.objectId+" - "+msg.path);
        }

      } catch (Exception e) {
        logger.info("What we've got here is failure to recognize: "+e.getMessage());        
      }
    });
  }

}
