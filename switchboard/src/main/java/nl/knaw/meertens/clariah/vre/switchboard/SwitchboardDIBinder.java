package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepositoryService;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryService;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.App.DEPLOYMENT_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.STATUS_FILE_NAME;

/**
 * Dependency injection using HK2
 */
public class SwitchboardDIBinder extends AbstractBinder {

    private static ObjectMapper mapper = null;
    private static PollServiceImpl pollingService = null;
    private static RequestRepositoryService requestRepositoryService = null;

    private ObjectsRegistryService objectsRegistryService;
    private DeploymentService deploymentService;

    SwitchboardDIBinder(
            ObjectsRegistryService objectsRegistryService,
            DeploymentService deploymentService
    ) {
        this.objectsRegistryService = objectsRegistryService;
        this.deploymentService = deploymentService;
    }

    @Override
    protected void configure() {
        bind(new ExecService(
                getMapper(),
                objectsRegistryService,
                deploymentService
        )).to(
                ExecService.class
        );
        bind(getMapper()).to(ObjectMapper.class);
    }

    /**
     * Get default ObjectMapper
     */
    static ObjectMapper getMapper() {
        if (isNull(mapper)) {
            mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(WRITE_DATES_AS_TIMESTAMPS);
            mapper.setSerializationInclusion(NON_NULL);
        }
        return mapper;
    }

    /**
     * Get default PollService
     */
    static PollService getPollService() {
        if (isNull(pollingService)) {
            pollingService = new PollServiceImpl(
                    getRequestRepositoryService(),
                    getMapper(),
                    DEPLOYMENT_HOST_NAME
            );
        }
        return pollingService;
    }

    /**
     * Get default RequestRepositoryService
     */
    static RequestRepositoryService getRequestRepositoryService() {
        if (isNull(requestRepositoryService)) {
            requestRepositoryService = new RequestRepositoryService(
                    DEPLOYMENT_VOLUME,
                    STATUS_FILE_NAME,
                    getMapper()
            );
        }
        return requestRepositoryService;
    }

}