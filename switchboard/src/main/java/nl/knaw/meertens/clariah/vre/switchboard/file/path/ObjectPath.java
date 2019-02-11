package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.FILES_DIR;

/**
 * Path of a file in nextcloud as found in object registry
 *
 * <p>Object path constists of:
 * {user}/{files}/{path/to/file}
 * Where {files} is FILES_DIR
 */
public class ObjectPath {

  private final Path objectPath;
  private final String file;
  private final String user;

  public ObjectPath(
    String objectPath
  ) {
    assertIsObjectPath(objectPath);
    this.objectPath = Path.of(objectPath);
    this.file = getFileFrom(objectPath);
    this.user = getUserFrom(objectPath);
  }

  @JsonCreator
  public ObjectPath(
    @JsonProperty("user") String user,
    @JsonProperty("file") String file
  ) {
    this.user = user;
    this.file = file;
    this.objectPath = Path.of(user, FILES_DIR, file);
  }

  /**
   * Create object path from sequence of strings
   */
  public static ObjectPath of(String first, String... more) {
    return new ObjectPath(Path.of(first, more).toString());
  }

  /**
   * Assert that objectPath has the following structure:
   * {user}/{files}/{rest/of/file/path.txt}
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

  private static String getFileFrom(String objectPath) {
    var inputPath = Paths.get(objectPath);
    return inputPath
      .subpath(2, inputPath.getNameCount())
      .toString();
  }

  private static String getUserFrom(String objectPath) {
    var inputPath = Paths.get(objectPath);
    return inputPath.subpath(0, 1).toString();
  }

  public Path toPath() {
    return objectPath;
  }

  public String toString() {
    return objectPath.toString();
  }

  public String getFile() {
    return file;
  }

  public String getUser() {
    return user;
  }
}
