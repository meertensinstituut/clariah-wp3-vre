package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerServiceImpl;
import nl.knaw.meertens.clariah.vre.tagger.object_tag.ObjectTagRegistry;
import nl.knaw.meertens.clariah.vre.tagger.tag.AutomaticTagsService;
import nl.knaw.meertens.clariah.vre.tagger.tag.TagRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static nl.knaw.meertens.clariah.vre.tagger.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.tagger.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.tagger.Config.RECOGNIZER_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.Config.RECOGNIZER_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.tagger.Config.TAGGER_TOPIC_NAME;

public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaConsumerService kafkaConsumerService = new KafkaConsumerService(
            KAFKA_SERVER,
            RECOGNIZER_TOPIC_NAME,
            RECOGNIZER_GROUP_NAME
    );

    private KafkaProducerService kafkaProducer = new KafkaProducerServiceImpl(
            TAGGER_TOPIC_NAME,
            KAFKA_SERVER,
            objectMapper
    );

    private final TagRegistry tagRegistry = new TagRegistry(
            OBJECTS_DB_URL,
            OBJECTS_DB_KEY,
            objectMapper
    );

    private final ObjectTagRegistry objectTagRegistry = new ObjectTagRegistry(
            OBJECTS_DB_URL,
            OBJECTS_DB_KEY
    );

    private final AutomaticTagsService automaticTagsService = new AutomaticTagsService(
            objectMapper,
            OBJECTS_DB_URL,
            OBJECTS_DB_KEY
    );

    private TaggerService taggerService = new TaggerService(
            objectMapper,
            kafkaConsumerService,
            kafkaProducer,
            tagRegistry,
            objectTagRegistry,
            automaticTagsService
    );

    private App() {

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(NON_NULL);

        taggerService.consumeRecognizer();
    }

    public static void main(String[] args) {
        new App();
    }

}