package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagRegistry;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagService;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.KAFKA_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.NEXTCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SERVICES_DB_KEY;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SERVICES_DB_URL;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SWITCHBOARD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getPollService;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getRequestRepository;

@ApplicationPath("resources")
public class App extends ResourceConfig {

    public static void main(String[] args) {}

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
                ),
                new KafkaProducerServiceImpl(
                        SWITCHBOARD_TOPIC_NAME,
                        KAFKA_HOST_NAME,
                        getMapper()
                ),
                new KafkaProducerServiceImpl(
                        NEXTCLOUD_TOPIC_NAME,
                        KAFKA_HOST_NAME,
                        getMapper()
                ),
                new TagRegistry(
                        OBJECTS_DB_URL,
                        OBJECTS_DB_KEY,
                        getMapper()
                )
        );
        register(diBinder);
    }

}
