package nl.knaw.meertens.deployment.lib.recipe;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import nl.knaw.meertens.deployment.lib.AbstractDeploymentTest;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DockerException;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import nl.knaw.meertens.deployment.lib.handler.Docker;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class DockerTest extends AbstractDeploymentTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void docker_containerShouldRunAndStop() throws HandlerPluginException, DockerException {
    Docker docker;
    Stack<HandlerPlugin> handlers = new Stack<>();
    var init_ok = false;

    try {
      docker = new Docker();
      init_ok = true;
    } catch (Exception e) {
      logger.debug("No docker ability, but can go on...");
      docker = null;
    }

    if (init_ok) {
      String serviceLocation = "Docker:_/busybox/latest/Http://{docker-container-ip}/test";
      String[] handlerLoc = serviceLocation.split(":", 2);
      handlers.push(docker);

      assertThat(docker.runDockerContainer(handlerLoc[1], handlers));

      logger.info("before cleanup test");
      docker.cleanup();
      logger.info("after cleanup test");
    }

  }

  @Test
  public void testRunDockerContainer()
      throws Exception {

    var dockerClient = Mockito.mock(DockerClient.class);
    var pullImageResponse = Mockito.mock(PullImageCmd.class);


    when(dockerClient.pullImageCmd(anyString())).thenReturn(pullImageResponse);
    when(pullImageResponse.withTag(anyString())).thenReturn(pullImageResponse);
    when(pullImageResponse.withAuthConfig(any())).thenReturn(pullImageResponse);
    var execResult = Mockito.mock(PullImageResultCallback.class);
    when(pullImageResponse.exec(any())).thenReturn(execResult);
    when(execResult.awaitCompletion()).thenReturn(execResult);

    var container = Mockito.mock(CreateContainerResponse.class);
    var createContainerCmd = Mockito.mock(CreateContainerCmd.class);
    when(dockerClient.createContainerCmd(anyString())).thenReturn(createContainerCmd);
    when(createContainerCmd.withCmd(anyString())).thenReturn(createContainerCmd);
    when(createContainerCmd.withName(anyString())).thenReturn(createContainerCmd);
    when(createContainerCmd.withHostName(anyString())).thenReturn(createContainerCmd);
    when(createContainerCmd.exec()).thenReturn(container);
    when(container.getId()).thenReturn("abc");

    var startContainerCmd = Mockito.mock(StartContainerCmd.class);
    when(dockerClient.startContainerCmd(anyString())).thenReturn(startContainerCmd);
    // doNothing().when(startContainerCmd.exec());


    var docker = new Docker(dockerClient);
    var handlers = new Stack<HandlerPlugin>();
    String serviceLocation = "Docker:_/busybox/latest/Http://{docker-container-ip}/test";
    logger.debug(String.format("docker obj is: [%s]", docker));
    String[] handlerLoc = serviceLocation.split(":", 2);
    docker.runDockerContainer(handlerLoc[1], handlers);
    docker.cleanup();
  }

}