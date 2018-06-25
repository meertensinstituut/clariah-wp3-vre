package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import nl.knaw.meertens.clariah.vre.switchboard.file.OwncloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceStub;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.USER_TO_LOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getRequestRepository;
import static org.assertj.core.util.Lists.newArrayList;

public class SwitchboardJerseyTest extends JerseyTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static ResourceConfig resourceConfig = null;
    protected static String testFile = "admin/files/testfile-switchboard.txt";

    private static RequestRepository requestRepository = getRequestRepository();

    private static PollServiceImpl pollService = new PollServiceImpl(
            requestRepository,
            getMapper(),
            "http://localhost:1080"
    );

    private static OwncloudFileService owncloudFileService = new OwncloudFileService(
            OWNCLOUD_VOLUME,
            DEPLOYMENT_VOLUME,
            OUTPUT_DIR,
            INPUT_DIR,
            USER_TO_LOCK_WITH
    );
    private static ServicesRegistryServiceImpl servicesRegistryService = new ServicesRegistryServiceImpl(
            "http://localhost:1080",
            "abc",
            getMapper()
    );

    @BeforeClass
    public static void beforeAbstractTests() throws IOException {
        Path path = Paths.get(OWNCLOUD_VOLUME + "/" + testFile);
        File file = path.toFile();
        file.getParentFile().mkdirs();
        String someText = "De vermeende terugkeer van tante Rosie naar Reetveerdegem werd als " +
                "een aangename schok ervaren in de levens van onze volstrekt nutteloze mannen, waarvan ik er op dat " +
                "ogenblik een in wording was.";
        Files.write(path, newArrayList(someText), Charset.forName("UTF-8"));
    }

    @After
    public void afterAbstractTest() {
        owncloudFileService.unlock(testFile);
        requestRepository.clearAll();
    }

    @AfterClass
    public static void afterAbstractTests() {
        owncloudFileService.unlock(testFile);
    }

    @Override
    protected Application configure() {
        if(resourceConfig != null) {
            return resourceConfig;
        }

        resourceConfig = new ResourceConfig(
                SwitchboardDIBinder.getControllerClasses()
        );

        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                createObjectsRegistryServiceStub(),
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

    private ObjectsRegistryServiceStub createObjectsRegistryServiceStub() {
        ObjectsRegistryServiceStub result = new ObjectsRegistryServiceStub();
        ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
        testFileRecord.id = 1L;
        testFileRecord.filepath = testFile;
        testFileRecord.mimetype = "text/plain";
        result.setTestFileRecord(testFileRecord);
        return result;
    }

    public Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return target(String.format("exec/%s", expectedService))
                .request()
                .post(Entity.json(deploymentRequestDto));
    }

    public PollServiceImpl getPollService() {
        return pollService;
    }

}
