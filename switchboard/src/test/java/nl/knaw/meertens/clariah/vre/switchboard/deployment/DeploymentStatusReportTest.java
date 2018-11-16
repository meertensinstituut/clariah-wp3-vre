package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder;
import org.junit.Test;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DeploymentStatusReportTest {

  /**
   * Note: only tests primitive fields
   */
  @Test
  public void testConstructorCopiesAllFields() throws JsonProcessingException {
    DeploymentStatusReport original = random(DeploymentStatusReport.class);
    DeploymentStatusReport copy = new DeploymentStatusReport(original);

    String copyJson = SwitchboardDiBinder.getMapper().writeValueAsString(copy);
    String originalJson = SwitchboardDiBinder.getMapper().writeValueAsString(original);

    assertThat(original.getMsg()).isNotBlank();
    assertThat(originalJson).isEqualTo(copyJson);
  }

}