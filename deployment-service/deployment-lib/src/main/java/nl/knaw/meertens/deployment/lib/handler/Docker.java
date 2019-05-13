package nl.knaw.meertens.deployment.lib.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import nl.knaw.meertens.deployment.lib.DockerException;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.HandlerPluginException;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.deployment.lib.SystemConf.DOCKER_CERT_PATH;
import static nl.knaw.meertens.deployment.lib.SystemConf.DOCKER_SERVER;
import static nl.knaw.meertens.deployment.lib.SystemConf.DOCKER_TLS_VERIFY;

public class Docker implements HandlerPlugin {
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private String dockerId;
  private String dockerHostName = UUID.randomUUID().toString();
  private DockerClient dockerClient;
  Pattern pattern = Pattern.compile("^([^/]+)/([^/]+)/([^/]+)/(.*)$");
  Matcher matcher;

  @Override
  public void init() throws HandlerPluginException, DockerException {
    logger.info("initializing!");
    logger.info("#### creating docker client ####");
    logger.info(
        "#### run 'socat TCP-LISTEN:2375,reuseaddr,fork UNIX-CONNECT:/var/run/docker.sock &' on docker host before " +
            "running the app ####");
    this.dockerClient = this.getDockerClient(DOCKER_SERVER, DOCKER_TLS_VERIFY, DOCKER_CERT_PATH);

    logger.info("#### succeed in docker client ####");
    Info info = dockerClient.infoCmd().exec();
    logger.info(String.format("Docker info [%s]", info.toString()));
    logger.info("initialized!");

  }

  public boolean runDockerContainer(String serviceLocation, Stack<HandlerPlugin> handlers)
      throws HandlerPluginException {
    handlers.push(this);

    logger.info(String.format("Service location before Docker [%s]", serviceLocation));
    // docker handler

    matcher = pattern.matcher(serviceLocation);
    if (matcher.matches()) {

      String dockerRepo = matcher.group(1); // vre-repository
      String dockerImg = matcher.group(2); // lamachine
      String dockerTag = matcher.group(3); // tag-1.0

      logger.info(String.format("Repo is: [%s]", dockerRepo));
      logger.info(String.format("Image is: [%s]", dockerImg));
      logger.info(String.format("Tag is: [%s]", dockerTag));

      logger.debug(String.format("dockerClient is [%s]", dockerClient));
      List<SearchItem> dockerSearch = dockerClient.searchImagesCmd(dockerImg).exec();
      logger.info(String.format("Docker search on the container: [%s]", dockerSearch.toString()));

      logger.info("Pulling");
      PullImageCmd pullImageCmd;
      if (dockerRepo.equals("_") || dockerRepo.toLowerCase().equals("none")) {
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

      logger.info("Image pulled; Running container; ls command will be running");

      CreateContainerResponse container = dockerClient.createContainerCmd(dockerImg)
                                                      .withCmd("ls")
                                                      .withHostName(dockerHostName)
                                                      .withName(dockerHostName)
                                                      .exec();

      this.dockerId = container.getId();
      dockerClient.startContainerCmd(this.dockerId).exec();
      logger.info("#### container running ####");

      return true;
    }
    throw new HandlerPluginException("Invalid docker service location!");
  }

  @Override
  public ObjectNode handle(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
      IllegalAccessException, HandlerPluginException, RecipePluginException {

    this.runDockerContainer(serviceLocation, handlers);

    String remainder = matcher.group(4);// nl.knaw.meertens.deployment.lib.handler.http://{docker-container-ip}/frog
    if (remainder.trim().length() > 0) {
      // replace variables
      remainder = remainder.replace("{docker-container-ip}", dockerHostName);
      // String loc = remainder.replaceAll("\\{docker-container-ip\\}","1.2.3.4/dockerip"); // nl.knaw.meertens
      // .deployment.lib.handler.http://192.3.4.5/frog
      // loc = loc.replaceAll("{docker-container-id}", docker - container - id);
      // invoke next handler
      return DeploymentLib.invokeHandler(workDir, service, remainder, handlers);
    }
    throw new HandlerPluginException("docker handler can't be used on its own!");

  }

  @Override
  public void cleanup() {
    try {
      dockerClient.stopContainerCmd(this.dockerId).exec();
      dockerClient.removeContainerCmd(this.dockerId).exec();
      logger.info("Cleanup docker handler done");
    } catch (Exception e) {
      logger.error(String.format("Cannot cleanup docker handler; [%s]", e));
    }
  }

  private DockerClient getDockerClient(String dockerHost, String dockerTls, String dockerTlsPath)
      throws HandlerPluginException, DockerException {
    DockerClientConfig config;
    if (dockerHost.length() > 1 && dockerTls.equals("1") && dockerTlsPath.length() > 0) {
      config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                                        .withDockerHost(dockerHost)
                                        .withDockerTlsVerify(true)
                                        .withDockerCertPath(dockerTlsPath)
                                        .build();
    } else if (dockerHost.length() > 1 && dockerTls.equals("0")) {
      config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                                        .withDockerHost(dockerHost)
                                        .build();
    } else {
      throw new HandlerPluginException(String
          .format(
              "Cannot initialize Docker client with following configuration. dockerHost: [%s]; dockerTls: [%s]; " +
                  "dockerTlsPath: [%s]", dockerHost, dockerTls, dockerTlsPath));
    }
    // using jaxrs/jersey implementation here (netty impl is also available)
    DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
        .withReadTimeout(30000)
        .withConnectTimeout(30000)
        .withMaxTotalConnections(100)
        .withMaxPerRouteConnections(10);

    try {
      DockerClient dockerClient;
      dockerClient = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(dockerCmdExecFactory).build();
      if (isNull(dockerClient)) {
        throw new DockerException("Failure when getting docker client, dockerClient is null");
      } else {
        return dockerClient;
      }
    } catch (Exception e) {
      throw new DockerException("Failure when getting docker client", e);
    }

  }
}

