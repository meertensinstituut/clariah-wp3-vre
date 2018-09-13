package nl.knaw.meertens.clariah.vre.integration.util;

import nl.knaw.meertens.clariah.vre.integration.Config;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectUtils {

    private static Logger logger = LoggerFactory.getLogger(ObjectUtils.class);

    public static long getObjectIdFromRegistry(String inputFile) throws SQLException {
        ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
                Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD);
        String query = "select * from object WHERE filepath LIKE '%" + inputFile + "%' LIMIT 1;";
        final long[] id = new long[1];
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                id[0] = (long) rs.getInt("id");
            }
            // When zero, no object has been found:
            assertThat(id[0]).isNotZero();
        });
        logger.info(String.format("uploaded file [%s] has object id [%d]", inputFile, id[0]));
        return id[0];
    }

}
