package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.PollDeploymentConsumer;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.OwncloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentResultDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentStartDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaOwncloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.param.Param;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamGroup;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamType;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.STOPPED;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.STRING;
import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.SERVICE;
import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.VIEWER;
import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.fromKind;

/**
 * ExecService:
 * <p>
 * When receives request:
 * - create config file
 * - send kafka msg of received request
 * - lock input files
 * - create soft links to input files
 * - deploy service
 * <p>
 * When deployed service finishes or is stopped:
 * - unlock files
 * - move dir with results to data dir
 * - send kafka msg
 */
public class ExecService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int WORK_DIR_LENGTH = 8;

    private final ObjectMapper mapper;
    private final KafkaProducerService kafkaSwitchboardService;
    private final KafkaProducerService kafkaOwncloudService;
    private final FileService nextcloudFileService;
    private final DeploymentService deploymentService;

    private ObjectsRegistryService objectsRegistryService;
    private ServicesRegistryService serviceRegistryService;

    public ExecService(
            ObjectMapper mapper,
            ObjectsRegistryService objectsRegistryService,
            DeploymentService deploymentService,
            ServicesRegistryService serviceRegistryService,
            KafkaProducerService kafkaSwitchboardService,
            KafkaProducerService kafkaOwncloudService
    ) {
        this.mapper = mapper;
        this.objectsRegistryService = objectsRegistryService;
        this.kafkaSwitchboardService = kafkaSwitchboardService;
        this.kafkaOwncloudService = kafkaOwncloudService;
        this.serviceRegistryService = serviceRegistryService;
        this.nextcloudFileService = new OwncloudFileService();
        this.deploymentService = deploymentService;
    }

    public DeploymentRequest deploy(
            String serviceName,
            String body
    ) {
        var service = serviceRegistryService.getServiceByName(serviceName);
        var kind = ServiceKind.fromKind(service.getKind());
        var request = prepareDeploymentRequest(serviceName, body, kind);
        List<String> files = new ArrayList<>(request.getFiles().values());
        nextcloudFileService.stageFiles(request.getWorkDir(), files);
        var statusReport = deploymentService.deploy(request, finishDeploymentConsumer);
        request.setStatusReport(statusReport);
        return request;
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
                default:
                    throw new UnsupportedOperationException(String.format("Unsupported deployment of service with kind [%s]", kind));
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not prepare deployment of [%s]", serviceName), e);
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

    private Param createViewerOutputParam(
            List<Param> params
    ) {
        var output = new Param();
        output.name = "output";
        output.type = STRING;
        output.value = params
                .stream()
                .filter(p -> p.name.equals("input"))
                .findFirst()
                .orElseGet(() -> {
                    throw new IllegalStateException("No input field in params for deployment of viewer");
                })
                .value;
        return output;
    }

    private DeploymentRequest prepareServiceDeployment(
            String service,
            String body
    ) throws IOException {
        DeploymentRequest request = createDeploymentRequest(service, body);
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
        var name = RandomStringUtils
                .randomAlphabetic(WORK_DIR_LENGTH)
                .toLowerCase();
        var path = Paths.get(DEPLOYMENT_VOLUME, name);
        assert (path.toFile().mkdirs());
        logger.info(String.format(
                "Created workDir [%s]",
                path.toString()
        ));
        return name;
    }

    /**
     * Consumer that finishes a deployment
     */
    public PollDeploymentConsumer<DeploymentStatusReport> finishDeploymentConsumer = (report) -> {
        logger.info(String.format("Status of [%s] is [%s]", report.getWorkDir(), report.getStatus()));
        if (isFinishedOrStopped(report)) {
            completeDeployment(report);
        }
    };

    private boolean isFinishedOrStopped(DeploymentStatusReport report) {
        return report.getStatus() == FINISHED || report.getStatus() == STOPPED;
    }

    private void completeDeployment(DeploymentStatusReport report) {

        var service = serviceRegistryService.getServiceByName(report.getService());
        var serviceKind = fromKind(service.getKind());

        nextcloudFileService.unstage(report.getWorkDir(), report.getFiles());

        if (serviceKind.equals(SERVICE)) {
            completeServiceDeployment(report);
        } else if (serviceKind.equals(VIEWER)) {
            completeViewerDeployment(report);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Could not complete deployment because service kind was not SERVICE or VIEWER but [%s]",
                    serviceKind
            ));
        }

        sendKafkaSwitchboardMsg(report);

        logger.info(String.format(
                "Completed deployment of service [%s] with workdir [%s]",
                report.getService(),
                report.getWorkDir()
        ));
    }

    private void completeServiceDeployment(
            DeploymentStatusReport report
    ) {
        var outputFiles = nextcloudFileService.unstageServiceOutputFiles(
                report.getWorkDir(),
                report.getFiles().get(0)
        );
        if (outputFiles.isEmpty()) {
            logger.warn(String.format("Deployment [%s] with service [%s] did not produce any output files", report.getWorkDir(), report.getService()));
        } else {
            report.setOutputDir(outputFiles.get(0).getParent().toString());
        }
        report.setWorkDir(report.getWorkDir());
        sendKafkaOwncloudMsgs(outputFiles);
    }

    private void completeViewerDeployment(
            DeploymentStatusReport report
    ) {
        var viewerFile = nextcloudFileService.unstageViewerOutputFile(
                report.getWorkDir(),
                report.getFiles().get(0),
                report.getService()
        );
        report.setViewerFile(viewerFile.toString());
        report.setViewerFileContent(nextcloudFileService.getContent(viewerFile.toString()));
        report.setWorkDir(report.getWorkDir());
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
            throw new RuntimeIOException(String.format(
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

    public DeploymentStatusReport getStatus(String workDir) {
        return deploymentService.getStatus(workDir);
    }

    private void sendKafkaSwitchboardMsg(
            DeploymentStatusReport report
    ) {
        var kafkaMsg = new KafkaDeploymentResultDto();
        kafkaMsg.service = report.getService();
        kafkaMsg.dateTime = LocalDateTime.now();
        kafkaMsg.status = report.getStatus();
        kafkaSwitchboardService.send(kafkaMsg);
    }

    private void sendKafkaOwncloudMsgs(
            List<Path> outputFiles
    ) {
        for (var file : outputFiles) {
            var msg = new KafkaOwncloudCreateFileDto();
            msg.action = "create";
            msg.path = file.toString();
            msg.timestamp = new Timestamp(System.currentTimeMillis()).getTime();
            msg.user = file.getName(0).toString();
            kafkaOwncloudService.send(msg);
        }
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

    private HashMap<Long, String> requestFilesFromRegistry(
            DeploymentRequest serviceRequest
    ) {
        HashMap<Long, String> files = new HashMap<>();
        for (var param : serviceRequest.getParams()) {
            if (param.type.equals(ParamType.FILE)) {
                var objectId = Long.valueOf(param.value);
                var record = objectsRegistryService.getObjectById(objectId);
                files.put(objectId, record.filepath);
            }
        }
        return files;
    }

    private void replaceObjectIdsWithPaths(
            List<Param> params,
            HashMap<Long, String> registryPaths
    ) {
        for (var param : params) {
            if (param.type.equals(ParamType.FILE)) {
                param.value = registryPaths.get(Long.valueOf(param.value));
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
