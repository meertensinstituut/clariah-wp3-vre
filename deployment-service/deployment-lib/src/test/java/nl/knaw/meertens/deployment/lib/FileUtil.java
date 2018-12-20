package nl.knaw.meertens.deployment.lib;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.USER_CONF_FILE;
import static org.assertj.core.util.Lists.newArrayList;

public class FileUtil {
  private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

  public static void createFile(String fileName, String content) throws IOException {
    Path path = Paths.get(fileName);
    path.toFile().getParentFile().mkdirs();
    Files.write(path, newArrayList(content), Charset.forName("UTF-8"));
    logger.info(format("Created file [%s]", path));
  }

  public static void createWorkDir(String workDir) throws IOException {
    Path path = Paths.get(ROOT_WORK_DIR, workDir);
    path.toFile().mkdirs();
  }

  public static void createInputFile(String workDir, String inputContent, String inputFilename) throws IOException {
    Path inputPath = Paths.get(ROOT_WORK_DIR, workDir, INPUT_DIR, inputFilename);
    createFile(inputPath.toString(), FileUtil.getTestFileContent(inputContent));
  }

  public static void createConfigFile(String workDir, String fileName) throws IOException {
    Path configPath = Paths.get(ROOT_WORK_DIR, workDir, USER_CONF_FILE);
    String testFileContent = FileUtil.getTestFileContent(fileName);
    createFile(configPath.toString(), testFileContent);

  }

  public static String getTestFileContent(String fileName) {
    File resource = new File("src/test/resources/" + fileName);
    try {
      return FileUtils.readFileToString(resource, UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
