package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.FILES_DIR;

/**
 * Path of a file in nextcloud as found in object registry
 *
 * <p>Object path constists of:
 * {user}/{files}/{path/to/file}
 * Where {files} is FILES_DIR
 */
public class ObjectPath {

  private final Path path;
  private final String file;
  private final String user;

  public ObjectPath(
    String path
  ) {
    assertIsObjectPath(path);
    this.path = Path.of(path);
    this.file = getFileFrom(path);
    this.user = getUserFrom(path);
  }

  @JsonCreator
  public ObjectPath(
    @JsonProperty("user") String user,
    @JsonProperty("file") String file
  ) {
    this.user = user;
    this.file = file;
    this.path = Path.of(user, FILES_DIR, file);
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
  private static void assertIsObjectPath(String objectPath) throws IllegalArgumentException {
    var pattern = "(.*)/" + FILES_DIR + "/(.*)";
    var match = Pattern
      .compile(pattern)
      .matcher(objectPath)
      .matches();
    if (!match) {
      throw new IllegalArgumentException(format(
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
    return path;
  }

  public String toString() {
    return path.toString();
  }


  public String getFile() {
    return file;
  }

  public String getUser() {
    return user;
  }
}
