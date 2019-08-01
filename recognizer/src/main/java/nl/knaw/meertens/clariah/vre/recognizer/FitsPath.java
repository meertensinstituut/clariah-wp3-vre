package nl.knaw.meertens.clariah.vre.recognizer;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_FILES_ROOT;

/**
 * Absolute path of object path
 */
public class FitsPath {

  /**
   * Fits root dir that contains object paths
   */
  private final String root = FITS_FILES_ROOT;

  /**
   * Path of file as found in object registry
   */
  private String objectPath;

  private FitsPath(String objectPath) {
    this.objectPath = objectPath;
  }

  public static FitsPath of(String objectPath) {
    return new FitsPath(objectPath);
  }

  public static FitsPath of(Path object) {
    return of(object.toString());
  }

  /**
   * @return absolute path
   */
  public Path toPath() {
    return Paths.get(FITS_FILES_ROOT, objectPath);
  }

  /**
   * @return string of absolute path
   */
  @Override
  public String toString() {
    return toPath().toString();
  }

}
