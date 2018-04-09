package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceStub;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryServiceStub;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;

@ApplicationPath("resources")
public class App extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResourceConfig.class);

    public static final String DEPLOYMENT_HOST_NAME = "http://deployment:8080";

    public static final String KAFKA_HOST_NAME = "kafka:" + System.getenv("KAFKA_PORT");
    public static final String SWITCHBOARD_TOPIC_NAME = System.getenv("SWITCHBOARD_TOPIC_NAME");
    public static final String OWNCLOUD_TOPIC_NAME = System.getenv("OWNCLOUD_TOPIC_NAME");
    public static final String DEPLOYMENT_VOLUME = System.getenv("DEPLOYMENT_VOLUME");
    public static final String OWNCLOUD_VOLUME = System.getenv("OWNCLOUD_VOLUME");
    public static final String OUTPUT_DIR = "output";
    public static final String INPUT_DIR = "input";
    public static final String CONFIG_FILE_NAME = "config.json";
    public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
    public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
    public static final String OBJECTS_DB_TOKEN = System.getenv("OBJECTS_TOKEN");

    public App() {
        configAppContext();
    }

    private void configAppContext() {
        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                getObjectsRegistryService(),
                new DeploymentServiceImpl(getMapper(), DEPLOYMENT_HOST_NAME)
        );
        ObjectsRegistryServiceStub stub = new ObjectsRegistryServiceStub();
        ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
        testFileRecord.filepath = "/admin/files/testfile-switchboard.txt";
        testFileRecord.id = 1L;
        stub.setTestFileRecord(testFileRecord);

        register(diBinder);
        packages("nl.knaw.meertens.clariah.vre.switchboard.exec");
    }

    private ObjectsRegistryService getObjectsRegistryService() {
        ObjectsRegistryService objectsRegistryService;
        try {
            objectsRegistryService = new ObjectsRegistryServiceImpl(
                    OBJECTS_DB_URL,
                    OBJECTS_DB_KEY,
                    OBJECTS_DB_TOKEN
            );
        } catch (IllegalArgumentException e) {
            logger.error(String.format(
                    "Could not create object registry: [%s]. Now using stub.",
                    e.getMessage()
            ));
            ObjectsRegistryServiceStub stub = new ObjectsRegistryServiceStub();
            ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
            testFileRecord.filepath = "/admin/files/testfile-switchboard.txt";
            testFileRecord.id = 1L;
            stub.setTestFileRecord(testFileRecord);
            objectsRegistryService = stub;
        }
        return objectsRegistryService;
    }

    public static void main(String[] args) {
    }

}
