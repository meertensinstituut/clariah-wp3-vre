package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.consumer.DeploymentConsumerFactory;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.NextcloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.ObjectPath;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentStartDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.param.Param;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamGroup;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamType;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDiBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.EDITOR_OUTPUT;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.EDITOR_TMP;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamService.getParamByName;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.STRING;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.getPath;

/**
 * ExecService:
 *
 * <p>When receives request:
 * - create config file
 * - send kafka msg of received request
 * - lock input files
 * - create soft links to input files
 * - deploy service
 *
 * <p>When deployed service finishes or is stopped:
 * - unlock files
 * - move dir with results to data dir
 * - send kafka msg
 */
public class ExecService {

  private static final int WORK_DIR_LENGTH = 8;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ObjectMapper mapper;
  private final KafkaProducerService kafkaSwitchboardService;
  private final FileService nextcloudFileService;
  private final DeploymentService deploymentService;
  private ObjectsRegistryService objectsRegistryService;
  private ServicesRegistryService serviceRegistryService;
  private DeploymentConsumerFactory finishDeploymentConsumer;

  public ExecService(
    ObjectMapper mapper,
    ObjectsRegistryService objectsRegistryService,
    DeploymentService deploymentService,
    ServicesRegistryService serviceRegistryService,
    KafkaProducerService kafkaSwitchboardService,
    DeploymentConsumerFactory deploymentConsumerFactory
  ) {
    this.mapper = mapper;
    this.objectsRegistryService = objectsRegistryService;
    this.kafkaSwitchboardService = kafkaSwitchboardService;
    this.serviceRegistryService = serviceRegistryService;
    this.finishDeploymentConsumer = deploymentConsumerFactory;
    this.nextcloudFileService = new NextcloudFileService();
    this.deploymentService = deploymentService;
  }

  public DeploymentRequest deploy(
    String serviceName,
    String body
  ) {
    var service = serviceRegistryService.getServiceByName(serviceName);
    var kind = ServiceKind.fromString(service.getKind());
    var request = prepareDeploymentRequest(serviceName, body, kind);
    var consumer = finishDeploymentConsumer.get(kind);
    List<ObjectPath> files = new ArrayList<>(request.getFiles().values());
    nextcloudFileService.stageFiles(request.getWorkDir(), files);
    var statusReport = deploymentService.deploy(request, consumer);
    request.setStatusReport(statusReport);
    return request;
  }

  public DeploymentStatusReport getStatus(String workDir) {
    return deploymentService.getStatus(workDir);
  }

  public void delete(String workDir) {
    deploymentService.delete(workDir);
  }

  private DeploymentRequest prepareDeploymentRequest(
    String serviceName,
    String body,
    ServiceKind kind
  ) {
    DeploymentRequest request;
    try {
      switch (kind) {
        case SERVICE:
          request = prepareServiceDeployment(serviceName, body);
          break;
        case VIEWER:
          request = prepareViewerDeployment(serviceName, body);
          break;
        case EDITOR:
          request = prepareEditorDeployment(serviceName, body);
          break;
        default:
          throw new UnsupportedOperationException(format("Unsupported service kind [%s]", kind));
      }
    } catch (IOException e) {
      throw new RuntimeException(format("Could not prepare deployment of [%s]", serviceName), e);
    }
    return request;
  }

  private DeploymentRequest prepareViewerDeployment(
    String service,
    String body
  ) throws IOException {
    var request = prepareServiceDeployment(service, body);
    var params = request.getParams();
    var outputParam = createViewerOutputParam(params);
    params.add(outputParam);
    createConfig(request);
    return request;
  }

  private DeploymentRequest prepareEditorDeployment(
    String service,
    String body
  ) throws IOException {
    var request = prepareServiceDeployment(service, body);
    var params = request.getParams();
    params.add(createEditorTmpParam(params));
    params.add(createEditorOutputParam(params));
    createConfig(request);
    return request;
  }

  /**
   * Name of viewer result file
   */
  private Param createViewerOutputParam(
    List<Param> params
  ) {
    var output = new Param();
    output.name = "output";
    output.type = STRING;
    output.value = getParamByName(params, "input");
    return output;
  }

  /**
   * Editor result file
   */
  private Param createEditorOutputParam(
    List<Param> params
  ) {
    var output = new Param();
    output.name = "output";
    output.type = STRING;

    var inputFile = getParamByName(params, "input");
    output.value = Path.of(
      getPath(inputFile),
      EDITOR_OUTPUT + "." + getExtension(inputFile)
    ).toString();
    return output;
  }

  /**
   * File that displays editor
   */
  private Param createEditorTmpParam(
    List<Param> params
  ) {
    var output = new Param();
    output.name = "tmp";
    output.type = STRING;
    output.value = EDITOR_TMP;
    return output;
  }


  private DeploymentRequest prepareServiceDeployment(
    String service,
    String body
  ) throws IOException {
    var request = createDeploymentRequest(service, body);
    createConfig(request);
    return request;
  }

  private DeploymentRequest createDeploymentRequest(
    String service,
    String body
  ) throws IOException {
    var workDir = createWorkDir();
    var request = mapServiceRequest(body, service, workDir);
    var paths = requestFilesFromRegistry(request);
    replaceObjectIdsWithPaths(request.getParams(), paths);
    request.setFiles(paths);
    sendKafkaRequestMsg(request);
    return request;
  }

  private String createWorkDir() {
    var name = UUID.randomUUID().toString();
    var path = Paths.get(DEPLOYMENT_VOLUME, name);
    assert (path.toFile().mkdirs());
    logger.info(format(
      "Created workDir [%s]",
      path.toString()
    ));
    return name;
  }

  private void createConfig(
    DeploymentRequest serviceRequest
  ) {
    var config = mapRequestToConfig(serviceRequest);
    var configPath = Paths.get(
      DEPLOYMENT_VOLUME,
      serviceRequest.getWorkDir(),
      CONFIG_FILE_NAME
    );
    try {
      var json = mapper.writeValueAsString(config);
      FileUtils.write(configPath.toFile(), json, UTF_8);
    } catch (IOException e) {
      throw new RuntimeIOException(format(
        "Could create config file [%s]",
        configPath.toString()
      ), e);
    }
  }

  private ConfigDto mapRequestToConfig(
    DeploymentRequest serviceRequest
  ) {
    var config = new ConfigDto();
    config.params = serviceRequest.getParams().stream().map(param -> {
      var fileDto = new ConfigParamDto();
      fileDto.name = param.name;
      fileDto.type = param.type;
      fileDto.value = param.value;
      if (param instanceof ParamGroup) {
        fileDto.params = ((ParamGroup) param).params;
      }
      return fileDto;
    }).collect(Collectors.toList());
    return config;
  }

  private DeploymentRequest mapServiceRequest(
    String body,
    String service,
    String workDir
  ) throws IOException {
    var deploymentRequestDto = mapper.readValue(body, DeploymentRequestDto.class);
    return new DeploymentRequest(
      service,
      workDir,
      LocalDateTime.now(),
      newArrayList(deploymentRequestDto.params)
    );
  }

  private HashMap<Long, ObjectPath> requestFilesFromRegistry(
    DeploymentRequest serviceRequest
  ) {
    HashMap<Long, ObjectPath> files = new HashMap<>();
    for (var param : serviceRequest.getParams()) {
      if (param.type.equals(ParamType.FILE)) {
        var objectId = Long.valueOf(param.value);
        var record = objectsRegistryService.getObjectById(objectId);
        files.put(objectId, new ObjectPath(record.filepath));
      }
    }
    return files;
  }

  private void replaceObjectIdsWithPaths(
    List<Param> params,
    HashMap<Long, ObjectPath> registryPaths
  ) {
    for (var param : params) {
      if (param.type.equals(ParamType.FILE)) {
        param.value = registryPaths.get(Long.valueOf(param.value)).toString();
      }
    }
  }

  private void sendKafkaRequestMsg(
    DeploymentRequest serviceRequest
  ) throws IOException {
    var msg = new KafkaDeploymentStartDto();
    msg.dateTime = serviceRequest.getDateTime();
    msg.service = serviceRequest.getService();
    msg.workDir = serviceRequest.getWorkDir();
    msg.deploymentRequest = mapper.writeValueAsString(serviceRequest.getParams());
    kafkaSwitchboardService.send(msg);
  }
}
