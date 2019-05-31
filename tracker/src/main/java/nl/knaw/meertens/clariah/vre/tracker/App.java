package nl.knaw.meertens.clariah.vre.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.meertens.clariah.vre.tracker.kafka.KafkaConsumerService;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static nl.knaw.meertens.clariah.vre.tracker.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.tracker.Config.RECOGNIZER_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.tracker.Config.RECOGNIZER_TOPIC_NAME;

public class App {
  private static Logger logger = LoggerFactory.getLogger(App.class);

  private final ObjectMapper objectMapper = new ObjectMapper();

  private KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(
    KAFKA_SERVER,
    RECOGNIZER_TOPIC_NAME,
    RECOGNIZER_GROUP_NAME
  );

  private ProvService provService = new ProvService(InteropFramework.newXMLProvFactory());

  private App() {

    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.setSerializationInclusion(NON_NULL);

    logger.info("Tracker app started");

    var document = provService.createProv();
    doConversions(document, "/tmp/my-prov.json");
  }

  public void doConversions(Document document, String file) {
    var interopFramework = new InteropFramework();
    interopFramework.writeDocument(file, document);
    interopFramework.writeDocument(System.out, InteropFramework.ProvFormat.JSON, document);
  }

  public static void main(String[] args) {
    new App();
  }

}
