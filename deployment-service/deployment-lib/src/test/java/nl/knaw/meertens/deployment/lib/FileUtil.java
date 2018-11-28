package nl.knaw.meertens.deployment.lib;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.util.Lists.newArrayList;

public class FileUtil {
  public static void createFile(String fileName, String resultSentence) throws IOException {
    Path path = Paths.get(fileName);
    path.toFile().getParentFile().mkdirs();
    Files.write(path, newArrayList(resultSentence), Charset.forName("UTF-8"));
  }

  public static void createWorkDir(String workDir) throws IOException {
    Path path = Paths.get(DeploymentLib.getSystemWorkDir(), workDir);
    path.toFile().mkdirs();
  }

  public static String getTestFileContent(String fileName) {
    try {
      return FileUtils.readFileToString(FileUtils.toFile(
        Thread.currentThread()
              .getContextClassLoader()
              .getResource(fileName)
      ), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
