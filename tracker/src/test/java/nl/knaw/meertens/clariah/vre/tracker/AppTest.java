package nl.knaw.meertens.clariah.vre.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.tracker.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.tracker.kafka.KafkaProducerService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AppTest {

  private static final String mockHostName = "http://localhost:1080";
  private static ClientAndServer mockServer;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private KafkaConsumerService kafkaConsumerService = Mockito.mock(KafkaConsumerService.class);
  private KafkaProducerService kafkaProducerService = Mockito.mock(KafkaProducerService.class);

  @BeforeClass
  public static void setUp() {
    mockServer = ClientAndServer.startClientAndServer(1080);
  }

  @Test
  public void testTest() {
    assertThat(true).isTrue();
  }

}
