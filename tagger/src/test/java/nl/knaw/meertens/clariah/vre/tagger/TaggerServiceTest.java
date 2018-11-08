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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

public class TaggerServiceTest {

    private static final String mockHostName = "http://localhost:1080";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaConsumerService kafkaConsumerService = Mockito.mock(KafkaConsumerService.class);
    private KafkaProducerService kafkaProducerService = Mockito.mock(KafkaProducerService.class);
    private final TagRegistry tagRegistry = new TagRegistry(mockHostName, "foo", objectMapper);
    private final ObjectTagRegistry objectTagRegistry = new ObjectTagRegistry(mockHostName, "bar");
    private final AutomaticTagsService automaticTagsService = new AutomaticTagsService(objectMapper, mockHostName, "baz");

    private static ClientAndServer mockServer;

    private final TaggerService taggerService = new TaggerService(
            objectMapper,
            kafkaConsumerService,
            kafkaProducerService,
            tagRegistry,
            objectTagRegistry,
            automaticTagsService
    );

    @BeforeClass
    public static void setUp() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @Test
    public void consumeRecognizer_withKafkaMsg() {
        var id = 1L;
        startObjectsRegistryMockServer(FileUtil.getTestFileContent("find-object.json"), id);

        startTagRegistryMockServer(id, "creation-time-ymdhm", 1);
        startTagRegistryMockServer(id, "creation-time-ymd", 1);
        startTagRegistryMockServer(id, "creation-time-ym", 1);
        startTagRegistryMockServer(id, "creation-time-y", 1);
        startTagRegistryMockServer(id, "path", 1);
        // filepath `admin/files/test.txt` contains two directories:
        startTagRegistryMockServer(id, "dir", 2);

        // for every tag call also a object tag call should be made:
        startObjectTagRegistryMockServer(id, 7);

        taggerService.consumeKafkaMsg(FileUtil.getTestFileContent("kafka-recognizer-msg.json"));

        verify(kafkaProducerService, times(7)).send(any());
    }

    private void startObjectsRegistryMockServer(String objectJson, long id) {
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/object")
                                .withQueryStringParameter(param("filter", "(id="+id+")")),
                        Times.exactly(1)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{ \"resource\": [" + objectJson + "]}")
                );
    }

    private void startTagRegistryMockServer(Long id, String type, int times) {
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/tag/")
                                .withQueryStringParameter(param("filter", ".*type="+type+".*")),
                        Times.exactly(times)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"resource\":[{\"id\":\"" + id + "\"}]}")
                );
    }

    private void startObjectTagRegistryMockServer(Long tagId, int times) {
        mockServer
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/_proc/insert_object_tag"),
                        Times.exactly(times)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"id\":\"" + tagId + "\"}")
                );
    }


}
