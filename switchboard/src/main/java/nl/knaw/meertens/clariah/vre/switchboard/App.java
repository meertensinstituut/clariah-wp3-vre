package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.tag.ObjectTagRegistry;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagRegistry;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.KAFKA_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.NEXTCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SERVICES_DB_KEY;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SERVICES_DB_URL;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SWITCHBOARD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder.getPollService;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder.getRequestRepository;

@ApplicationPath("resources")
public class App extends ResourceConfig {

  public App() {
    super(SwitchboardDiBinder.getControllerClasses());
    configureAppContext();
  }

  public static void main(String[] args) {
  }

  private void configureAppContext() {
    SwitchboardDiBinder diBinder = new SwitchboardDiBinder(
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
      ),
      new ObjectTagRegistry(
        OBJECTS_DB_URL,
        OBJECTS_DB_KEY
      )
    );
    register(diBinder);
  }

}
