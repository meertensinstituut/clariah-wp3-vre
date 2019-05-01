package nl.knaw.meertens.deployment.lib.handler;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Docker implements HandlerPlugin {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init() {
    logger.info("initialized!");
  }

  @Override
  public String handle(String serviceName, String serviceLocation)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
      IllegalAccessException, HandlerPluginException {
    // TODO: make the handle work with docker
    logger.info(String.format("Service location before Docker [%s]", serviceLocation));
    // docker handler
    Pattern pattern = Pattern.compile("^([^/]+)/([^/]+)/([^/]+)/(.*)$");
    Matcher matcher = pattern.matcher(serviceLocation);
    if (matcher.matches()) {

      String dockerRepo = matcher.group(1); // vre-repository
      String dockerImg = matcher.group(2); // lamachine
      String dockerTag = matcher.group(3); // tag-1.0
      // do all the docker magic
      // which results in the following variables to be known
      // - docker-container-id
      // - docker-container-ip = 192.3.4.5

      logger.info("#### creating docker client ####");
      logger.info(
          "#### run 'socat TCP-LISTEN:2375,reuseaddr,fork UNIX-CONNECT:/var/run/docker.sock &' on docker host before " +
              "running the app ####");
      DockerClient dockerClient = getDockerClient("tcp://host.docker.internal:2375");
      logger.info("#### succeed in docker client ####");
      Info info = dockerClient.infoCmd().exec();
      logger.info(String.format("Docker info [%s]", info.toString()));
      logger.info(String.format("Repo is: [%s]", dockerRepo));
      logger.info(String.format("Image is: [%s]", dockerImg));
      logger.info(String.format("Tag is: [%s]", dockerTag));

      List<SearchItem> dockerSearch =
          dockerClient.searchImagesCmd(dockerImg).exec();
      logger.info(String.format("Docker search on the container: [%s]", dockerSearch.toString()));

      logger.info("Pulling");
      PullImageCmd pullImageCmd = null;
      if (dockerRepo.equals("_")) {
        pullImageCmd = dockerClient.pullImageCmd(dockerImg)
                                   .withTag(dockerTag)
                                   .withAuthConfig(dockerClient.authConfig());
      } else {
        pullImageCmd = dockerClient.pullImageCmd(dockerImg)
                                   .withTag(dockerTag)
                                   .withRepository(dockerRepo)
                                   .withAuthConfig(dockerClient.authConfig());
      }


      try {
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
      } catch (InterruptedException e) {
        logger.error(String.format("Cannot pull the image [%s/%s/%s]; ", dockerRepo, dockerImg, dockerTag), e);
      }

      logger.info("After pulling");

      CreateContainerResponse container = dockerClient.createContainerCmd(dockerImg)
                                                      .withCmd("ls")
                                                      .exec();
      dockerClient.startContainerCmd(container.getId()).exec();
      dockerClient.stopContainerCmd(container.getId()).exec();

      logger.info("#### container running ####");

      String remainder = matcher.group(4);// nl.knaw.meertens.deployment.lib.handler.http://{docker-container-ip}/frog
      if (remainder.trim().length() > 0) {
        // replace variables
        remainder = remainder.replace("{docker-container-ip}", "1.2.3.4/dockerip");
        // String loc = remainder.replaceAll("\\{docker-container-ip\\}","1.2.3.4/dockerip"); // nl.knaw.meertens
        // .deployment.lib.handler.http://192.3.4.5/frog
        // loc = loc.replaceAll("{docker-container-id}", docker - container - id);
        // invoke next handler
        return DeploymentLib.invokeHandler(serviceName, remainder);

        // do all the docker shutdown magic
      }
      throw new HandlerPluginException("docker handler can't be used on its own!");
    }
    throw new HandlerPluginException("Invalid docker service location!");
  }

  private DockerClient getDockerClient(String dockerHost) {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                                                         .withDockerHost(dockerHost)
                                                         .build();

    // using jaxrs/jersey implementation here (netty impl is also available)
    DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
        .withReadTimeout(30000)
        .withConnectTimeout(30000)
        .withMaxTotalConnections(100)
        .withMaxPerRouteConnections(10);


    return DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(dockerCmdExecFactory).build();
  }
}

