package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import nl.knaw.meertens.clariah.vre.switchboard.file.OwncloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceStub;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.tag.ObjectTagRegistry;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagRegistry;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;

/**
 * Wrapper around JerseyTest which is used
 * to start Jersey Test Container only once.
 */
public class SwitchboardJerseyTest extends JerseyTest {

    private static final String mockHostName = "http://localhost:1080";
    private static final String mockRegistryKey = "abc";

    private static ResourceConfig resourceConfig;

    private static RequestRepository requestRepository = SwitchboardDIBinder.getRequestRepository();

    private static PollServiceImpl pollService = new PollServiceImpl(
            requestRepository,
            SwitchboardDIBinder.getMapper(),
            mockHostName
    );

    private static OwncloudFileService nextcloudFileService = new OwncloudFileService();

    private static ServicesRegistryServiceImpl servicesRegistryService = new ServicesRegistryServiceImpl(
            mockHostName,
            mockRegistryKey,
            SwitchboardDIBinder.getMapper()
    );

    private static ObjectsRegistryServiceStub objectsRegistryServiceStub = new ObjectsRegistryServiceStub();

    private KafkaProducerService kafkaSwitchboardServiceMock;
    private KafkaProducerService kafkaOwncloudServiceMock;

    @Override
    protected Application configure() {
        setMocks();

        if(resourceConfig != null) {
            return resourceConfig;
        }

        resourceConfig = new ResourceConfig(
                SwitchboardDIBinder.getControllerClasses()
        );

        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                objectsRegistryServiceStub,
                servicesRegistryService,
                new DeploymentServiceImpl(
                        mockHostName,
                        requestRepository,
                        pollService
                ),
                kafkaSwitchboardServiceMock,
                kafkaOwncloudServiceMock,
                new TagRegistry(
                        mockHostName,
                        mockRegistryKey,
                        getMapper()
                ),
                new ObjectTagRegistry(
                        mockHostName,
                        mockRegistryKey,
                        getMapper()
                )
        );
        resourceConfig.register(diBinder);
        return resourceConfig;
    }

    private void setMocks() {
        kafkaSwitchboardServiceMock = Mockito.mock(KafkaProducerService.class);
        kafkaOwncloudServiceMock = Mockito.mock(KafkaProducerService.class);
    }

    public Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return target(String.format("exec/%s", expectedService))
                .request()
                .post(Entity.json(deploymentRequestDto));
    }

    public static OwncloudFileService getOwncloudFileService() {
        return nextcloudFileService;
    }

    public static RequestRepository getRequestRepository() {
        return requestRepository;
    }

    public static ObjectsRegistryServiceStub getObjectsRegistryServiceStub() {
        return objectsRegistryServiceStub;
    }

    public KafkaProducerService getKafkaSwitchboardServiceMock() {
        return kafkaSwitchboardServiceMock;
    }

    public KafkaProducerService getKafkaOwncloudServiceMock() {
        return kafkaOwncloudServiceMock;
    }

}
