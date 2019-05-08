package nl.knaw.meertens.deployment.lib;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

public class SystemConf {

  private static final String defaultConfigPath = "/conf/conf.xml";

  public static final String ROOT_WORK_DIR;
  public static final String OUTPUT_DIR;
  public static final String INPUT_DIR;
  public static final String USER_CONF_FILE;
  public static final String QUEUE_LENGTH;
  public static final String DOCKER_SERVER;
  public static final String DOCKER_TLS_VERIFY;
  public static final String DOCKER_CERT_PATH;

  static  {
    XMLConfiguration xml;
    try {
      xml = new XMLConfiguration(defaultConfigPath);
    } catch (ConfigurationException e) {
      throw new RuntimeException(String.format(
        "Could not read conf file [%s]",
        SystemConf.defaultConfigPath
      ));
    }
    ROOT_WORK_DIR = xml.getString("workingDirectory");
    USER_CONF_FILE = xml.getString("userConfFile");
    INPUT_DIR = xml.getString("inputDirectory");
    OUTPUT_DIR = xml.getString("outputDirectory");
    QUEUE_LENGTH = xml.getString("configLength");
    DOCKER_SERVER = xml.getString("dockerServer");
    DOCKER_TLS_VERIFY = xml.getString("dockerTlsVerify");
    DOCKER_CERT_PATH = xml.getString("dockerCertPath");
  }

}
