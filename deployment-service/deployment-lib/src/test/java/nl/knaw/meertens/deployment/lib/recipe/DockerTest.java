package nl.knaw.meertens.deployment.lib.recipe;

import nl.knaw.meertens.deployment.lib.AbstractDeploymentTest;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.handler.Docker;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DockerTest extends AbstractDeploymentTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void docker_containerShouldRunAndStop() throws HandlerPluginException {
    Docker docker = new Docker();
    Stack<HandlerPlugin> handlers = new Stack<>();

    docker.init();
    String serviceLocation = "Docker:_/busybox/latest/Http://{docker-container-ip}/test";
    String[] handlerLoc = serviceLocation.split(":", 2);
    handlers.push(docker);

    assertThat(docker.runDockerContainer(handlerLoc[1], handlers));

    logger.info("before cleanup test");
    docker.cleanup();
    logger.info("after cleanup test");

  }

}