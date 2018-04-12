package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ExceptionalConsumer;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.DeploymentFileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentResultDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentStartDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaOwncloudCreateFileDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryService;
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
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.App.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.App.KAFKA_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OWNCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.SWITCHBOARD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.STOPPED;

/**
 * ExecService:
 * - when receives request:
 * - create config file
 * - send kafka msg of received request
 * - lock input files
 * - create soft links to input files
 * - deploy service
 * - when deployed service finishes or is stopped:
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
        this.owncloudFileService = new DeploymentFileService(OWNCLOUD_VOLUME, DEPLOYMENT_VOLUME, OUTPUT_DIR, INPUT_DIR);
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

    private ExceptionalConsumer<DeploymentStatusReport> finishDeploymentConsumer = (report) -> {
        if (isFinishedOrStopped(report)) {
            completeDeployment(report);
        } else {
            logger.error(String.format("Finish method called with status [%s]", report.getStatus()));
        }
    };

    private boolean isFinishedOrStopped(DeploymentStatusReport report) {
        return report.getStatus() == FINISHED || report.getStatus() == STOPPED;
    }

    private DeploymentRequest prepareDeployment(
            String service,
            String body
    ) throws IOException {
        DeploymentRequest serviceRequest = mapServiceRequest(body, service);
        serviceRequest.setFiles(requestFiles(serviceRequest));
        createConfig(serviceRequest);
        sendKafkaRequestMsg(serviceRequest);
        return serviceRequest;
    }

    private void completeDeployment(DeploymentStatusReport report) throws IOException {
        List<Path> outputFiles = owncloudFileService.unstage(report.getWorkDir(), report.getFiles());
        Path path = outputFiles.get(0);
        report.setOutputDir(isNull(path) ? "" : path.getParent().toString());
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
            ExceptionHandler.handleException(e, "Could create config file [%s]", configPath.toString());
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
            msg.userPath = file.toString();
            msg.timestamp = new Timestamp(System.currentTimeMillis()).getTime();
            msg.user = file.getName(0).toString();
            kafkaOwncloudService.send(msg);
        }

    }

    private DeploymentRequest mapServiceRequest(
            String body,
            String service
    ) throws IOException {
        DeploymentRequestDto deploymentRequestDto = mapper.readValue(body, DeploymentRequestDto.class);
        String workDir = RandomStringUtils
                .randomAlphabetic(WORK_DIR_LENGTH)
                .toLowerCase();
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
