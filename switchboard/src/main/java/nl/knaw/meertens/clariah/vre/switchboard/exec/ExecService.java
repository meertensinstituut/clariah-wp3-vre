package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentService;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.DeploymentFileService;
import nl.knaw.meertens.clariah.vre.switchboard.file.FileService;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentResultDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaDeploymentStartDto;
import nl.knaw.meertens.clariah.vre.switchboard.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.App.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.App.KAFKA_HOST_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.SWITCHBOARD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.handleException;
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
    private final KafkaProducerService kafkaProducerService;
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
        this.kafkaProducerService = new KafkaProducerService(SWITCHBOARD_TOPIC_NAME, KAFKA_HOST_NAME, mapper);
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
        DeploymentStatusReport statusReport = deploymentService.start(request, (report) -> {
            if (isFinishedOrStopped(report)) {
                completeDeployment(request, files, report);
            } else {
                logger.error(String.format("Finish method called with status [%s]", report.getStatus()));
            }
        });
        request.setStatusReport(statusReport);
        return request;
    }

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

    private void completeDeployment(
            DeploymentRequest serviceRequest,
            List<String> inputFiles,
            DeploymentStatusReport report
    ) throws IOException {
        logger.info(String.format(
                "Complete deployment of service [%s] with workdir [%s]",
                serviceRequest.getService(),
                serviceRequest.getWorkDir()
        ));
        String outputDir = owncloudFileService.unstage(serviceRequest.getWorkDir(), inputFiles);
        report.setOutputDir(getPathRelativeToOwncloud(outputDir));
        report.setWorkDir(serviceRequest.getWorkDir());
        sendKafkaResultMsg(serviceRequest, report);
    }

    private String getPathRelativeToOwncloud(String outputDir) {
        return new File(OWNCLOUD_VOLUME).toURI().relativize(new File(outputDir).toURI()).getPath();
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
        return deploymentService.pollStatus(workDir);
    }

    private void sendKafkaResultMsg(
            DeploymentRequest serviceRequest,
            DeploymentStatusReport report
    ) throws IOException {
        KafkaDeploymentResultDto kafkaMsg = new KafkaDeploymentResultDto();
        kafkaMsg.service = serviceRequest.getService();
        kafkaMsg.dateTime = LocalDateTime.now();
        kafkaMsg.status = report.getStatus();
        kafkaProducerService.produceToSwitchboardTopic(kafkaMsg);
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
        kafkaProducerService.produceToSwitchboardTopic(msg);
    }

}
