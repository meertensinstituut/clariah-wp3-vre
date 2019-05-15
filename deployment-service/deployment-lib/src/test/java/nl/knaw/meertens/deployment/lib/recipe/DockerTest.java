package nl.knaw.meertens.deployment.lib.recipe;

import nl.knaw.meertens.deployment.lib.AbstractDeploymentTest;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DockerException;
import nl.knaw.meertens.deployment.lib.FileUtil;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.handler.Docker;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

import static nl.knaw.meertens.deployment.lib.FileUtil.createFile;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.USER_CONF_FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DockerTest extends AbstractDeploymentTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void docker_containerShouldRunAndStop() throws HandlerPluginException, DockerException {
    Docker docker;
    Stack<HandlerPlugin> handlers = new Stack<>();
    boolean init_ok = false;

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

    Docker docker = new Docker();
    var handlers = new Stack<HandlerPlugin>();
    String serviceLocation = "Docker:_/busybox/latest/Http://{docker-container-ip}/test";
    logger.debug(String.format("docker obj is: [%s]", docker));
    String[] handlerLoc = serviceLocation.split(":", 2);
    docker.runDockerContainer(handlerLoc[1], handlers);
    docker.cleanup();
  }

}