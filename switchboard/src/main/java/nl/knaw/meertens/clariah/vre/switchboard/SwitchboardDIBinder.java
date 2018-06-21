package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecController;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecService;
import nl.knaw.meertens.clariah.vre.switchboard.object.ObjectController;
import nl.knaw.meertens.clariah.vre.switchboard.object.ObjectService;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamController;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.STATUS_FILE_NAME;

/**
 * Dependency injection using HK2
 */
public class SwitchboardDIBinder extends AbstractBinder {

    /**
     * All controllers of switchboard app
     */
    private static Set<Class<?>> controllerClasses = newHashSet(
            ExecController.class,
            ObjectController.class,
            ParamController.class
    );

    private static ObjectMapper mapper = null;
    private static PollServiceImpl pollingService = null;
    private static RequestRepository requestRepositoryService = null;


    private ObjectsRegistryService objectsRegistryService;
    private DeploymentService deploymentService;
    private ServicesRegistryService serviceRegistryService;

    SwitchboardDIBinder(
            ObjectsRegistryService objectsRegistryService,
            ServicesRegistryService servicesRegistryService,
            DeploymentService deploymentService
    ) {
        this.objectsRegistryService = objectsRegistryService;
        this.deploymentService = deploymentService;
        this.serviceRegistryService = servicesRegistryService;
    }

    @Override
    protected void configure() {
        bind(new ExecService(getMapper(), objectsRegistryService, deploymentService)).to(ExecService.class);
        bind(new ObjectService(objectsRegistryService,serviceRegistryService)).to(ObjectService.class);
        bind(new ParamService(serviceRegistryService)).to(ParamService.class);
        bind(getMapper()).to(ObjectMapper.class);
        ExceptionHandler.setMapper(getMapper());
    }

    /**
     * Get default ObjectMapper
     */
    public static ObjectMapper getMapper() {
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
                    getRequestRepository(),
                    getMapper(),
                    DEPLOYMENT_HOST_NAME
            );
        }
        return pollingService;
    }

    /**
     * Get default RequestRepository
     */
    static RequestRepository getRequestRepository() {
        if (isNull(requestRepositoryService)) {
            requestRepositoryService = new RequestRepository(
                    DEPLOYMENT_VOLUME,
                    STATUS_FILE_NAME,
                    getMapper()
            );
        }
        return requestRepositoryService;
    }

    /**
     * Get all controller classes
     */
    public static Set<Class<?>> getControllerClasses() {
        return controllerClasses;
    }
}