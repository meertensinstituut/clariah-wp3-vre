package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder.getMapper;

/**
 * Path of an config file of a deployed service
 * `/{tmp}/{workDir}/{configFile}`
 */
public class DeploymentConfigFile extends AbstractSwitchboardPath {

  private final String configFile;

  private DeploymentConfigFile(String workDir) {
    this.workDir = workDir;
    this.configFile = CONFIG_FILE_NAME;
  }

  public static DeploymentConfigFile from(String workDir) {
    return new DeploymentConfigFile(workDir);
  }

  @Override
  public Path toPath() {
    return Paths.get(tmp, workDir, configFile);
  }

  @Override
  public ObjectPath toObjectPath() {
    throw new IllegalStateException("A config file cannot be converted into a objectPath");
  }

  public ConfigDto getConfig() {
    String configString;
    try {
      configString = FileUtils.readFileToString(toPath().toFile(), UTF_8);
      return getMapper().readValue(configString, ConfigDto.class);
    } catch (IOException e) {
      throw new RuntimeException(format("Could not read config file of [%s]", this.workDir), e);
    }
  }

  public String getTmp() {
    return tmp;
  }

  public String getWorkDir() {
    return workDir;
  }

  public String getConfigFile() {
    return configFile;
  }


}
