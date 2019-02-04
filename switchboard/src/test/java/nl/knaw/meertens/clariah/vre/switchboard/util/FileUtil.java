package nl.knaw.meertens.clariah.vre.switchboard.util;

import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardJerseyTest;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDto;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.NEXTCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class FileUtil {

  private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

  public static ObjectsRecordDto createTestFileWithRegistryObject(String content) throws IOException {
    var fileName = String.format("admin/files/testfile-switchboard-%s.txt", UUID.randomUUID());
    createNextcloudFile(fileName, content);
    var maxId = SwitchboardJerseyTest.getObjectsRegistryServiceStub().getMaxTestObject();
    Long newId = maxId + 1;
    var newObject = createRegistryObject(fileName, newId);
    SwitchboardJerseyTest.getObjectsRegistryServiceStub().addTestObject(newObject);
    return newObject;
  }

  public static void createResultFile(String workDir, String resultFilename, String content) {
    var path = Paths.get(DEPLOYMENT_VOLUME, workDir, OUTPUT_DIR, resultFilename);
    assertThat((path.toFile().getParentFile().mkdirs())).isTrue();
    path.toFile().getParentFile().mkdirs();
    try {
      Files.write(path, newArrayList(content), UTF_8);
    } catch (IOException e) {
      throw new RuntimeIOException("DeploymentServiceStub could not create result file", e);
    }
  }

  private static void createNextcloudFile(String fileName, String content) throws IOException {
    var path = Paths.get(NEXTCLOUD_VOLUME + "/" + fileName);
    var file = path.toFile();
    file.getParentFile().mkdirs();
    Files.write(path, newArrayList(content), Charset.forName("UTF-8"));
  }

  public static String getNextcloudFileContent(String relativePath) throws IOException {
    var path = Paths.get(NEXTCLOUD_VOLUME, relativePath);
    assertThat(path.toFile()).exists();
    return FileUtils.readFileToString(path.toFile(), UTF_8);
  }

  private static ObjectsRecordDto createRegistryObject(String filePath, long id) {
    var testFileRecord = new ObjectsRecordDto();
    testFileRecord.id = id;
    testFileRecord.filepath = filePath;
    testFileRecord.mimetype = "text/plain";
    return testFileRecord;
  }

  public static String getTestFileContent(String fileName) {
    try {
      return FileUtils.readFileToString(Objects.requireNonNull(FileUtils.toFile(
        Thread.currentThread()
              .getContextClassLoader()
              .getResource(fileName)
      )), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
