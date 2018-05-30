package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ExceptionalConsumer;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.OwncloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentResultDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentStartDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaOwncloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.KAFKA_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.SWITCHBOARD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.USER_TO_LOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.STOPPED;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;

/**
 * ExecService:
 *
 * When receives request:
 * - create config file
 * - send kafka msg of received request
 * - lock input files
 * - create soft links to input files
 * - deploy service
 *
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
    private final FileService owncloudFileService;
    private final DeploymentService deploymentService;

    private ObjectsRegistryService objectsRegistryService;

    public ExecService(
            ObjectMapper mapper,
            ObjectsRegistryService objectsRegistryService,
            DeploymentService deploymentService
    ) {
        this.mapper = mapper;
        this.objectsRegistryService = objectsRegistryService;
        this.kafkaSwitchboardService = new KafkaProducerService(SWITCHBOARD_TOPIC_NAME, KAFKA_HOST_NAME, mapper);
        this.kafkaOwncloudService = new KafkaProducerService(OWNCLOUD_TOPIC_NAME, KAFKA_HOST_NAME, mapper);
        this.owncloudFileService = new OwncloudFileService(OWNCLOUD_VOLUME, DEPLOYMENT_VOLUME, OUTPUT_DIR, INPUT_DIR, USER_TO_LOCK_WITH);
        this.deploymentService = deploymentService;
    }

    public DeploymentRequest deploy(
            String service,
            String body
    ) throws IOException {
        DeploymentRequest request = prepareDeployment(service, body);
        List<String> files = new ArrayList<>(request.getFiles().values());
        owncloudFileService.stage(request.getWorkDir(), files);
        DeploymentStatusReport statusReport = deploymentService.deploy(request, finishDeploymentConsumer);
        request.setStatusReport(statusReport);
        return request;
    }

    private DeploymentRequest prepareDeployment(
            String service,
            String body
    ) throws IOException {
        String workDir = createWorkDir();
        DeploymentRequest request = mapServiceRequest(body, service, workDir);
        request.setFiles(requestFiles(request));
        createConfig(request);
        sendKafkaRequestMsg(request);
        return request;
    }

    private String createWorkDir() {
        String name = RandomStringUtils
                .randomAlphabetic(WORK_DIR_LENGTH)
                .toLowerCase();
        Path path = Paths.get(DEPLOYMENT_VOLUME, name);
        assert(path.toFile().mkdirs());
        logger.info(String.format(
                "Created workDir [%s]",
                path.toString()
        ));
        return name;
    }

    private ExceptionalConsumer<DeploymentStatusReport> finishDeploymentConsumer = (report) -> {
        logger.info(String.format("Status of [%s] is [%s]", report.getWorkDir(), report.getStatus()));
        if (isFinishedOrStopped(report)) {
            completeDeployment(report);
        }
    };

    private boolean isFinishedOrStopped(DeploymentStatusReport report) {
        return report.getStatus() == FINISHED || report.getStatus() == STOPPED;
    }

    private void completeDeployment(DeploymentStatusReport report) throws IOException {
        List<Path> outputFiles = owncloudFileService.unstage(report.getWorkDir(), report.getFiles());
        report.setOutputDir(outputFiles.isEmpty() ? "" : outputFiles.get(0).getParent().toString());
        report.setWorkDir(report.getWorkDir());
        sendKafkaSwitchboardMsg(report);
        sendKafkaOwncloudMsgs(outputFiles);
        logger.info(String.format(
                "Completed deployment of service [%s] with workdir [%s]",
                report.getService(),
                report.getWorkDir()
        ));
    }

    private void createConfig(
            DeploymentRequest serviceRequest
    ) {
        ConfigDto config = mapRequestToConfig(serviceRequest);
        Path configPath = Paths.get(
                DEPLOYMENT_VOLUME,
                serviceRequest.getWorkDir(),
                CONFIG_FILE_NAME
        );
        try {
            String json = mapper.writeValueAsString(config);
            FileUtils.write(configPath.toFile(), json, UTF_8);
        } catch (IOException e) {
            handleException(e, "Could create config file [%s]", configPath.toString());
        }
    }

    private ConfigDto mapRequestToConfig(DeploymentRequest serviceRequest) {
        ConfigDto config = new ConfigDto();
        config.params = serviceRequest.getParams().stream().map(file -> {
            ConfigParamDto fileDto = new ConfigParamDto();
            fileDto.name = file.name;
            fileDto.type = file.type;
            fileDto.params = file.params;
            fileDto.value = file.value;
            return fileDto;
        }).collect(Collectors.toList());
        return config;
    }

    public DeploymentStatusReport getStatus(String workDir) {
        return deploymentService.getStatus(workDir);
    }

    private void sendKafkaSwitchboardMsg(
            DeploymentStatusReport report
    ) throws IOException {
        KafkaDeploymentResultDto kafkaMsg = new KafkaDeploymentResultDto();
        kafkaMsg.service = report.getService();
        kafkaMsg.dateTime = LocalDateTime.now();
        kafkaMsg.status = report.getStatus();
        kafkaSwitchboardService.send(kafkaMsg);
    }

    private void sendKafkaOwncloudMsgs(List<Path> outputFiles) throws IOException {
        for (Path file : outputFiles) {
            KafkaOwncloudCreateFileDto msg = new KafkaOwncloudCreateFileDto();
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
        DeploymentRequestDto deploymentRequestDto = mapper.readValue(body, DeploymentRequestDto.class);
        return new DeploymentRequest(
                service,
                workDir,
                LocalDateTime.now(),
                deploymentRequestDto.params
        );
    }

    private HashMap<Long, String> requestFiles(DeploymentRequest serviceRequest) {
        HashMap<Long, String> files = new HashMap<>();
        for (ParamDto param : serviceRequest.getParams()) {
            Long objectId = Long.valueOf(param.value);
            ObjectsRecordDTO record = objectsRegistryService.getObjectById(objectId);
            files.put(objectId, record.filepath);
            param.value = record.filepath;
        }
        return files;
    }

    private void sendKafkaRequestMsg(DeploymentRequest serviceRequest) throws IOException {
        KafkaDeploymentStartDto msg = new KafkaDeploymentStartDto();
        msg.dateTime = serviceRequest.getDateTime();
        msg.service = serviceRequest.getService();
        msg.workDir = serviceRequest.getWorkDir();
        msg.deploymentRequest = mapper.writeValueAsString(serviceRequest.getParams());
        kafkaSwitchboardService.send(msg);
    }

}
