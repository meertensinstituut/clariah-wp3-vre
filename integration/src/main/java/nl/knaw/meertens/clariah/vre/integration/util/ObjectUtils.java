package nl.knaw.meertens.clariah.vre.integration.util;

import nl.knaw.meertens.clariah.vre.integration.Config;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ObjectUtils {

  private static Logger logger = LoggerFactory.getLogger(ObjectUtils.class);

  private static ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
    Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
  );

  public static long getNonNullObjectIdFromRegistry(String inputFile) {
    long id;
    id = getObjectIdFromRegistry(inputFile);
    Assertions.assertThat(id).isNotZero();
    return id;
  }

  static long getObjectIdFromRegistry(String inputFile) {
    String query = "select * from object WHERE filepath LIKE '%" + inputFile + "%' LIMIT 1;";
    // must be semi final:
    final long[] id = {0};
    try {
      objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
        while (rs.next()) {
          id[0] = (long) rs.getInt("id");
        }
      });
    } catch (SQLException e) {
      throw new RuntimeException("Could not get file in registry", e);
    }
    logger.info(String.format("uploaded file [%s] has object id [%d]", inputFile, id[0]));
    return id[0];
  }

  public static boolean fileExistsInRegistry(String expectedFilename, String mimeType, String fileFormat) {
    String query = "select * from object WHERE filepath LIKE '%" + expectedFilename + "%' LIMIT 1;";
    try {
      return objectsRepositoryService.processQueryWithFunction(query, (ResultSet rs) -> {
        boolean fileExists = false;
        while (rs.next()) {
          int id = rs.getInt("id");
          fileExists = id != 0 &&
            rs.getString("filepath").contains(expectedFilename) &&
            rs.getString("format").equals(fileFormat) &&
            rs.getString("mimetype").equals(mimeType);
        }
        return fileExists;
      });
    } catch (SQLException e) {
      throw new RuntimeException("Could not check file in registry", e);
    }
  }

}
