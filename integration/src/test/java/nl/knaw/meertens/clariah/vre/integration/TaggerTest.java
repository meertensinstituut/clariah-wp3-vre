package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.integration.AbstractIntegrationTest.getRandomGroupName;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.pollAndAssert;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggerTest extends AbstractIntegrationTest {

    private KafkaConsumerService taggerTopic;

    @Before
    public void setUp() {
        taggerTopic = getRecognizerTopic();
    }

    @Test
    public void generateSystemTags_afterUploadNewFile() throws UnirestException, InterruptedException {
        // Create object:
        final String expectedFilename = uploadTestFile();
        Long objectId = pollAndAssert(() -> getObjectIdFromRegistry(expectedFilename));

        taggerTopic.consumeAll(records -> {
            assertThat(records.size()).isEqualTo(7);
            records.forEach(record -> {
                String msg = JsonPath.parse(record.value()).read("$.msg");
                assertThat(msg).isEqualTo("Created new object tag");
                assertThatJson(record.value()).node("owner")
                        .isPresent()
                        .isEqualTo("system");
                assertThatJson(record.value()).node("object")
                        .isPresent()
                        .isEqualTo(objectId);
                assertThatJson(record.value()).node("tag")
                        .isPresent();
            });

        });
    }

    private KafkaConsumerService getRecognizerTopic() {
        KafkaConsumerService taggerKafkaConsumer = new KafkaConsumerService(
                Config.KAFKA_ENDPOINT,
                Config.TAGGER_TOPIC_NAME,
                getRandomGroupName()
        );
        taggerKafkaConsumer.subscribe();
        taggerKafkaConsumer.pollOnce();
        return taggerKafkaConsumer;
    }

}
