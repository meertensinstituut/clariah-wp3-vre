package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.ObjectPath;
import org.junit.Test;

import java.net.URI;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DeploymentStatusReportTest {

  /**
   * Note: only tests primitive fields
   */
  @Test
  public void testConstructorCopiesAllFields() throws JsonProcessingException {
    var random = new Random();
    var original = new DeploymentStatusReport();
    original.setPolled(now());
    original.setStatus(FINISHED);
    original.setWorkDir(randomUUID().toString());
    original.setMsg("Msg " + random.nextInt());
    original.setViewerFileContent("File content " + random.nextInt());
    original.setInterval(random.nextInt());
    original.setOutputDir("output/dir" + random.nextInt());
    original.setService("service-" + random.nextInt());
    original.setUri(URI.create("uri-" + random.nextInt()));
    original.setFiles(newArrayList(new ObjectPath("testuser", "file-" + random.nextInt() + ".txt")));
    var copy = new DeploymentStatusReport(original);

    var copyJson = SwitchboardDiBinder.getMapper().writeValueAsString(copy);
    var originalJson = SwitchboardDiBinder.getMapper().writeValueAsString(original);

    assertThat(original.getMsg()).isNotBlank();
    assertThat(originalJson).isEqualTo(copyJson);
  }

}