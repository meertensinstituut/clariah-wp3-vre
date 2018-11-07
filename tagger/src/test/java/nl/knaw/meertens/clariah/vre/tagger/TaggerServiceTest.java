package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tagger.kafka.KafkaProducerService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TaggerServiceTest {

    private static final String mockHostName = "http://localhost:1080";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaConsumerService kafkaConsumerService = Mockito.mock(KafkaConsumerService.class);
    private KafkaProducerService kafkaProducer = Mockito.mock(KafkaProducerService.class);
    private final TagRegistry tagRegistry = new TagRegistry(mockHostName, "foo", objectMapper);
    private final ObjectTagRegistry objectTagRegistry = new ObjectTagRegistry(mockHostName, "bar");

    private static ClientAndServer mockServer;

    private final TaggerService taggerService = new TaggerService(
            objectMapper,
            kafkaConsumerService,
            kafkaProducer,
            tagRegistry,
            objectTagRegistry
    );

    @BeforeClass
    public static void setUp() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @Test
    public void consumeRecognizer_withKafkaMsg() {
        startObjectsRegistryMockServer(FileUtil.getTestFileContent("find-object.json"));
        startTagRegistryMockServer(1L);

        startObjectTagRegistryMockServer(1L);

        // TODO: test that correct tags are generated

        taggerService.consumeKafkaMsg(FileUtil.getTestFileContent("kafka-recognizer-msg.json"));
    }

    public void startObjectsRegistryMockServer(String objectJson) {
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/object/"),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{ \"resource\": [" + objectJson + "]}")
                );
    }

    public void startTagRegistryMockServer(Long id) {
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/tag/"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"resource\":[{\"id\":\"" + id + "\"}]}")
                );
    }

    public void startObjectTagRegistryMockServer(Long tagId) {
        mockServer
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/_proc/insert_object_tag")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"id\":\"" + tagId + "\"}")
                );
    }


}
