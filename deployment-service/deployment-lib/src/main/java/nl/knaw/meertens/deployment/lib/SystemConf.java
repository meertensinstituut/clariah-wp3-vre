package nl.knaw.meertens.deployment.lib;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

public class SystemConf {


  private static final String defaultConfigPath = "/conf/conf.xml";

  public static final String systemWorkDir;
  public static final String userConfFile;
  public static final String outputDirectory;
  public static final String inputDirectory;
  public static final String queueLength;

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
    systemWorkDir = xml.getString("workingFolder");
    userConfFile = xml.getString("userConfFile");
    inputDirectory = xml.getString("inputDirectory");
    outputDirectory = xml.getString("outputDirectory");
    queueLength = xml.getString("configLength");
  }

}
