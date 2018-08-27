package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import nl.knaw.meertens.clariah.vre.switchboard.file.OwncloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceStub;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.USER_TO_LOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.VRE_DIR;

/**
 * Wrapper around JerseyTest which is used
 * to start Jersey Test Container only once.
 */
public class SwitchboardJerseyTest extends JerseyTest {

    private static ResourceConfig resourceConfig;

    private static RequestRepository requestRepository = SwitchboardDIBinder.getRequestRepository();

    private static PollServiceImpl pollService = new PollServiceImpl(
            requestRepository,
            SwitchboardDIBinder.getMapper(),
            "http://localhost:1080"
    );

    private static OwncloudFileService owncloudFileService = new OwncloudFileService(
            DEPLOYMENT_VOLUME,
            OUTPUT_DIR,
            INPUT_DIR,
            USER_TO_LOCK_WITH,
            VRE_DIR
    );

    private static ServicesRegistryServiceImpl servicesRegistryService = new ServicesRegistryServiceImpl(
            "http://localhost:1080",
            "abc",
            SwitchboardDIBinder.getMapper()
    );

    private static ObjectsRegistryServiceStub objectsRegistryServiceStub = new ObjectsRegistryServiceStub();

    @Override
    protected Application configure() {
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
                        "http://localhost:1080",
                        requestRepository,
                        pollService
                )
        );
        resourceConfig.register(diBinder);
        return resourceConfig;
    }

    public Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return target(String.format("exec/%s", expectedService))
                .request()
                .post(Entity.json(deploymentRequestDto));
    }

    public static OwncloudFileService getOwncloudFileService() {
        return owncloudFileService;
    }

    public static RequestRepository getRequestRepository() {
        return requestRepository;
    }

    public static ObjectsRegistryServiceStub getObjectsRegistryServiceStub() {
        return objectsRegistryServiceStub;
    }
}
