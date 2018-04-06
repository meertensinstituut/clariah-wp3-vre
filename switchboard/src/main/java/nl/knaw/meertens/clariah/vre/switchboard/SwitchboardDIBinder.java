package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryService;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * Dependency injection using HK2
 */
public class SwitchboardDIBinder extends AbstractBinder {

    private static ObjectMapper mapper = null;
    private ObjectsRegistryService objectsRegistryService;
    private DeploymentService deploymentService;

    public SwitchboardDIBinder(
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

    static ObjectMapper getMapper() {
        if (mapper != null) {
            return mapper;
        }
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(NON_NULL);
        return mapper;
    }

}