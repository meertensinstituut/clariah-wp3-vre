package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.OUTPUT_DIR;

/**
 * Path of a output dir stored in nextcloud has the following structure:
 * `/{nextcloud}/{user}/{files}/{outputResult}/`
 */
public class NextcloudOutputDir extends AbstractSwitchboardPath {

  private NextcloudOutputDir(String user, String outputResult) {
    this.user = user;
    this.outputResult = outputResult;
  }

  public static NextcloudOutputDir from(DeploymentInputFile file) {
    return new NextcloudOutputDir(
      file.user,
      generateOutputPath(file)
    );
  }

  /**
   * Put timestamped output dir next to input file
   */
  private static String generateOutputPath(DeploymentInputFile file) {
    String parent = FilenameUtils.getPath(file.getFile());
    return Paths.get(parent, generateOutputDir()).toString();
  }

  private static String generateOutputDir() {
    return OUTPUT_DIR +
      "-" +
      now().format(ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
  }

  @Override
  public Path toPath() {
    return Paths.get(nextcloud, user, files, outputResult);
  }

  @Override
  public ObjectPath toObjectPath() {
    throw new UnsupportedOperationException();
  }

  public String getNextcloud() {
    return nextcloud;
  }

  public String getUser() {
    return user;
  }

  public String getFiles() {
    return files;
  }

  public String getOutputResult() {
    return outputResult;
  }
}
