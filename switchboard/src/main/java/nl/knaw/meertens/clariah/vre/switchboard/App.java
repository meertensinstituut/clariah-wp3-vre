package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecController;
import nl.knaw.meertens.clariah.vre.switchboard.object.ObjectController;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getPollService;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getRequestRepository;

@ApplicationPath("resources")
public class App extends ResourceConfig {

    public App() {
        super(SwitchboardDIBinder.getControllerClasses());
        configureAppContext();
    }

    private void configureAppContext() {
        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                new ObjectsRegistryServiceImpl(
                        OBJECTS_DB_URL,
                        OBJECTS_DB_KEY,
                        getMapper()
                ),
                new ServicesRegistryServiceImpl(
                        SERVICES_DB_URL,
                        SERVICES_DB_KEY,
                        getMapper()
                ),
                new DeploymentServiceImpl(
                        DEPLOYMENT_HOST_NAME,
                        getRequestRepository(),
                        getPollService()
                )
        );
        register(diBinder);
    }

    public static void main(String[] args) {
    }

}
