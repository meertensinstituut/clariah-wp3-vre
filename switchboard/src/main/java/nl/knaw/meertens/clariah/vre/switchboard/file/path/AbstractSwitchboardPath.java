package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.SystemConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.FILES_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.NEXTCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.VRE_DIR;

/**
 * Path within VRE services ecosystem
 * to which Switchboard needs access
 */
public abstract class AbstractSwitchboardPath {

  /**
   * Deployment root dir that contains all workDirs and their temporary files
   */
  final String tmp = DEPLOYMENT_VOLUME;

  /**
   * Nextcloud root dir that contains users and their files
   */
  final String nextcloud = NEXTCLOUD_VOLUME;

  /**
   * Dir that contains files uploaded by user
   */
  final String files = FILES_DIR;

  /**
   * Hidden folder in user folder that contains vre specific files
   */
  final String vre = VRE_DIR;

  /**
   * Deployment dir that contains input files of a deployed service
   */
  final String input = INPUT_DIR;

  /**
   * Deployment dir that contains output files of a deployed service
   */
  final String output = OUTPUT_DIR;

  /**
   * Dir that contains all files of user
   * Is equal to user name
   */
  String user;

  /**
   * Folder of viewer service
   */
  String service;

  /**
   * File and parent dirs as created by user
   */
  String file;

  /**
   * Deployment dir that contains config, input and output of a deployed service
   */
  String workDir;

  /**
   * Nextcloud dir that contains output files of a finished deployment
   * Contains time stamp
   */
  String outputResult;

  /**
   * Object paths constist of:
   * {user}/{files}/{file}
   *
   * @return {file}
   */
  static String getFileFrom(String objectPath) {
    assertIsObjectPath(objectPath);
    var inputPath = Paths.get(objectPath);
    return inputPath
      .subpath(2, inputPath.getNameCount())
      .toString();
  }

  /**
   * Object path constists of:
   * {user}/{files}/{file}
   *
   * @return {user}
   */
  static String getUserFrom(String objectPath) {
    assertIsObjectPath(objectPath);
    var inputPath = Paths.get(objectPath);
    return inputPath.subpath(0, 1).toString();
  }

  /**
   * Assert that objectPath has the following structure:
   * {user}/files/{rest/of/file/path.txt}
   * Nb. Validity of user name is not checked

   * @throws IllegalArgumentException when assertion fails
   */
  private static void assertIsObjectPath(String objectPath) {
    var pattern = "(.*)/" + FILES_DIR + "/(.*)";
    var match = Pattern
      .compile(pattern)
      .matcher(objectPath)
      .matches();
    if (!match) {
      throw new IllegalArgumentException(String.format(
        "objectPath [%s] did not match pattern [%s]",
        objectPath, pattern
      ));
    }
  }

  /**
   * @return absolute path
   */
  public abstract Path toPath();

  /**
   * ObjectPath: path as found in object registry
   *
   * @return relative path
   */
  public abstract String toObjectPath();
}
