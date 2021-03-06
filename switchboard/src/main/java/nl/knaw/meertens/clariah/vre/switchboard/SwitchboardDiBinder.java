package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.knaw.meertens.clariah.vre.switchboard.consumer.DeploymentConsumerFactory;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusRepository;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.exception.CommonExceptionMapper;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecController;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecService;
import nl.knaw.meertens.clariah.vre.switchboard.health.HealthController;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.object.ObjectController;
import nl.knaw.meertens.clariah.vre.switchboard.object.ObjectService;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamController;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.tag.ObjectTagRegistry;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagController;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagRegistry;
import nl.knaw.meertens.clariah.vre.switchboard.tag.TagService;
import org.glassfish.jersey.internal.inject.AbstractBinder;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.DEPLOYMENT_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.STATUS_FILE_NAME;

/**
 * Dependency injection using HK2
 */
public class SwitchboardDiBinder extends AbstractBinder {

  /**
   * All controllers of switchboard app
   */
  private static Set<Class<?>> controllerClasses = newHashSet(
    HealthController.class,
    ExecController.class,
    ObjectController.class,
    ParamController.class,
    CommonExceptionMapper.class,
    TagController.class
  );

  private static ObjectMapper mapper = null;
  private static PollServiceImpl pollingService = null;
  private static DeploymentStatusRepository deploymentStatusRepository = null;
  private ObjectsRegistryService objectsRegistryService;
  private DeploymentService deploymentService;
  private ServicesRegistryService serviceRegistryService;
  private KafkaProducerService kafkaSwitchboardService;
  private TagRegistry tagRegistry;
  private ObjectTagRegistry objectTagRegistry;
  private DeploymentConsumerFactory deploymentConsumerFactory;

  SwitchboardDiBinder(
    ObjectsRegistryService objectsRegistryService,
    ServicesRegistryService servicesRegistryService,
    DeploymentService deploymentService,
    KafkaProducerService kafkaSwitchboardService,
    TagRegistry tagRegistry,
    ObjectTagRegistry objectTagRegistry,
    DeploymentConsumerFactory deploymentConsumerFactory
  ) {
    this.objectsRegistryService = objectsRegistryService;
    this.deploymentService = deploymentService;
    this.serviceRegistryService = servicesRegistryService;
    this.kafkaSwitchboardService = kafkaSwitchboardService;
    this.tagRegistry = tagRegistry;
    this.objectTagRegistry = objectTagRegistry;
    this.deploymentConsumerFactory = deploymentConsumerFactory;
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
   * Get default DeploymentStatusRepository
   */
  static DeploymentStatusRepository getRequestRepository() {
    if (isNull(deploymentStatusRepository)) {
      deploymentStatusRepository = new DeploymentStatusRepository(
        DEPLOYMENT_VOLUME,
        STATUS_FILE_NAME,
        getMapper()
      );
    }
    return deploymentStatusRepository;
  }

  /**
   * Get all controller classes
   */
  public static Set<Class<?>> getControllerClasses() {
    return controllerClasses;
  }

  @Override
  protected void configure() {
    bind(getMapper()).to(ObjectMapper.class);

    bind(new ExecService(
      getMapper(),
      objectsRegistryService,
      deploymentService,
      serviceRegistryService,
      kafkaSwitchboardService,
      deploymentConsumerFactory
    )).to(ExecService.class);

    bind(new ObjectService(
      objectsRegistryService,
      serviceRegistryService
    )).to(ObjectService.class);

    bind(new ParamService(
      serviceRegistryService
    )).to(ParamService.class);

    bind(new TagService(
      tagRegistry,
      objectTagRegistry
    )).to(TagService.class);
  }
}
