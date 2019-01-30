package nl.knaw.meertens.clariah.vre.integration.util;

import nl.knaw.meertens.clariah.vre.integration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectUtils {

    private static Logger logger = LoggerFactory.getLogger(ObjectUtils.class);

    private static ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
            Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
    );

    // must be semi final:
    private static long id;

    public static long getObjectIdFromRegistry(String inputFile) {
        id = 0L;
        String query = "select * from object WHERE filepath LIKE '%" + inputFile + "%' LIMIT 1;";
        try {
            objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
                while (rs.next()) {
                    id = (long) rs.getInt("id");
                }
                // When zero, no object has been found:
                assertThat(id).isNotZero();
            });
            logger.info(String.format("uploaded file [%s] has object id [%d]", inputFile, id));
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get file in registry", e);
        }
    }

    public static boolean fileExistsInRegistry(String expectedFilename) {
        String query = "select * from object WHERE filepath LIKE '%" + expectedFilename + "%' LIMIT 1;";
        try {
            return objectsRepositoryService.processQueryWithFunction(query, (ResultSet rs) -> {
                boolean fileExists = false;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    fileExists = id != 0
                      && rs.getString("filepath").contains(expectedFilename)
                      &&rs.getString("format").equals("Plain text")
                      &&rs.getString("mimetype").equals("text/plain");
                }
                return fileExists;
            });
        } catch (SQLException e) {
            throw new RuntimeException("Could not check file in registry", e);
        }
    }

}
