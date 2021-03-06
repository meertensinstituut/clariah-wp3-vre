package nl.knaw.meertens.clariah.vre.switchboard.health;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HealthControllerTest extends AbstractControllerTest {

  @Test
  public void getHealth() {
    var response = target("/health")
      .request()
      .get();

    assertThat(response.getStatus()).isEqualTo(200);
    var json = response.readEntity(String.class);
    assertThat(JsonPath.parse(json).read("$.status", String.class)).isEqualTo("OK");
  }

}
