package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.RecognizerKafkaProducer;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectRepository;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectSemanticTypeRepository;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_FILES_ROOT;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.NEXTCLOUD_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.NEXTCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_SEMANTIC_TYPE_TABLE;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_TABLE;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.RECOGNIZER_TOPIC_NAME;

public class App {

  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private static final ObjectMapper objectMapper;

  private static final KafkaConsumerService nextcloudConsumerService;

  private static final KafkaProducerService kafkaProducer;

  private static final FitsService fitsService;

  private static final MimetypeService mimetypeService;

  private static final SemanticTypeService semanticTypeService;

  private static final ObjectSemanticTypeRepository objectSemanticTypeRepository;

  private static final ObjectRepository objectRepository;

  private static final ObjectsService objectsService;

  private static final RecognizerService recognizerService;

  static {
    objectMapper = ObjectMapperFactory.getInstance();

    nextcloudConsumerService = new KafkaConsumerService(
      KAFKA_SERVER,
      NEXTCLOUD_TOPIC_NAME,
      NEXTCLOUD_GROUP_NAME
    );

    kafkaProducer = new KafkaProducerService(
      new RecognizerKafkaProducer(KAFKA_SERVER),
      RECOGNIZER_TOPIC_NAME
    );

    fitsService = new FitsService(
      FITS_URL,
      FITS_FILES_ROOT
    );

    objectSemanticTypeRepository = new ObjectSemanticTypeRepository(
      OBJECTS_DB_URL,
      OBJECTS_DB_KEY,
      OBJECT_SEMANTIC_TYPE_TABLE,
      objectMapper
    );

    mimetypeService = new MimetypeService();

    semanticTypeService = new SemanticTypeService(
      mimetypeService
    );

    objectRepository = new ObjectRepository(
      OBJECTS_DB_URL,
      OBJECTS_DB_KEY,
      OBJECT_TABLE,
      objectMapper
    );

    objectsService = new ObjectsService(
      FITS_FILES_ROOT,
      semanticTypeService,
      objectRepository,
      objectSemanticTypeRepository,
      mimetypeService
    );

    recognizerService = new RecognizerService(
      objectMapper,
      nextcloudConsumerService,
      objectsService,
      mimetypeService,
      fitsService,
      kafkaProducer
    );

  }

  public static void main(String[] args) {
    logger.info("Start consuming nextcloud topic...");
    recognizerService.consumeOwncloud();
  }

}
