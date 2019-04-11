package nl.knaw.meertens.clariah.vre.recognizer.util;

import java.io.IOException;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.toFile;

public class FileUtil {

  public static String getTestFileContent(String fileName) {
    try {
      return readFileToString(requireNonNull(toFile(
        currentThread()
          .getContextClassLoader()
          .getResource(fileName)
      )), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(format("Could not load test file [%s]", fileName), e);
    }
  }

}
