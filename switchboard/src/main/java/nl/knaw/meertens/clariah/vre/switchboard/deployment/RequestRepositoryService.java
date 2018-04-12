package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.handleException;

public class RequestRepositoryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String deploymentRoot;
    private final String statusFileName;
    private final ObjectMapper mapper;

    private final Map<String, DeploymentStatusReport> reports = new HashMap<>();
    private final Map<String, ExceptionalConsumer<DeploymentStatusReport>> consumers = new HashMap<>();

    public RequestRepositoryService(
            String deploymentRoot,
            String statusFileName,
            ObjectMapper mapper
    ) {
        this.deploymentRoot = deploymentRoot;
        this.statusFileName = statusFileName;
        this.mapper = mapper;
    }

    public ExceptionalConsumer<DeploymentStatusReport> getConsumer(String workDir) {
        return consumers.get(workDir);
    }

    public DeploymentStatusReport getStatusReport(String workDir) {
        DeploymentStatusReport request = reports.get(workDir);
        if (request == null) {
            return throwWorkDirNotFound(workDir);
        }
        return request;
    }

    public void saveDeploymentRequest(
            DeploymentStatusReport statusReport,
            ExceptionalConsumer<DeploymentStatusReport> finishRequest
    ) {
        this.reports.put(statusReport.getWorkDir(), statusReport);
        this.consumers.put(statusReport.getWorkDir(), finishRequest);
        logger.info(String.format(
                "Persisted deployment request of workDir [%s]",
                statusReport.getWorkDir()
        ));
    }

    public List<DeploymentStatusReport> getAllStatusReports() {
        return new ArrayList<>(reports.values());
    }

    public void saveStatusReport(DeploymentStatusReport updatedReport) {
        reports.put(updatedReport.getWorkDir(), updatedReport);
        updateStatusReportFile(updatedReport);
    }

    private void updateStatusReportFile(DeploymentStatusReport updatedReport) {
        Path file = Paths.get(
                deploymentRoot,
                updatedReport.getWorkDir(),
                statusFileName
        );
        try {
            String json = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(updatedReport);
            FileUtils.write(file.toFile(), json, UTF_8);
        } catch (IOException e) {
            handleException(e, "Could create status report file [%s]", file.toString());
        }
    }

    private DeploymentStatusReport throwWorkDirNotFound(String workDir) {
        String keys = Arrays.toString(
                reports.entrySet()
                        .stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList())
                        .toArray()
        );
        throw new IllegalArgumentException(String.format(
                "Request with workDir [%s] does not exist in workDirs: [%s]",
                workDir, keys
        ));
    }

}
