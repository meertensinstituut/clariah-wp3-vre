package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryServiceImpl;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getPollService;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getRequestRepository;

@ApplicationPath("resources")
public class App extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResourceConfig.class);

    public App() {
        configureAppContext();
    }

    private void configureAppContext() {
        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                getObjectsRegistryService(),
                new DeploymentServiceImpl(
                        DEPLOYMENT_HOST_NAME,
                        getRequestRepository(),
                        getPollService()
                )
        );
        register(diBinder);
        packages("nl.knaw.meertens.clariah.vre.switchboard.exec");
    }

    private ObjectsRegistryService getObjectsRegistryService() {
        ObjectsRegistryService objectsRegistryService = null;
        try {
            objectsRegistryService = new ObjectsRegistryServiceImpl(
                    OBJECTS_DB_URL,
                    OBJECTS_DB_KEY
            );
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Could not create object registry", e));
        }
        return objectsRegistryService;
    }

    public static void main(String[] args) {
    }

}
