package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.exception.NoReportFileException;
import org.apache.commons.io.FileUtils;
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
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_MEMORY_SPAN;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;

public class RequestRepository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String deploymentRoot;
    private final String statusFileName;
    private final ObjectMapper mapper;

    private final Map<String, DeploymentStatusReport> reports = new HashMap<>();
    private final Map<String, ExceptionalConsumer<DeploymentStatusReport>> consumers = new HashMap<>();
    private final Map<String, LocalDateTime> finished = new HashMap<>();

    public RequestRepository(
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
        if (!isNull(request)) {
            return request;
        }
        logger.info(String.format("Report of [%s] not available in memory", workDir));
        return findReportInWorkDir(workDir);
    }

    public void saveDeploymentRequest(
            DeploymentStatusReport report,
            ExceptionalConsumer<DeploymentStatusReport> reportConsumer
    ) {
        saveStatusReport(report);
        consumers.put(report.getWorkDir(), reportConsumer);
        logger.info(String.format(
                "Persisted deployment request of workDir [%s]",
                report.getWorkDir()
        ));
    }

    public List<DeploymentStatusReport> getAllStatusReports() {
        return new ArrayList<>(reports.values());
    }

    /**
     * Status reports are saved in a hashmap and
     * in a json file in workDir.
     *
     * A report is removed from the hashmap when it has
     * finished longer than DEPLOYMENT_MEMORY_SPAN-seconds ago.
     *
     * When the status of a deployment is requested,
     * it is added to the hashmap again
     */
    public void saveStatusReport(DeploymentStatusReport report) {
        String workDir = report.getWorkDir();
        reports.put(workDir, report);
        saveToFile(report);
        if (report.getStatus() == FINISHED) {
            handleFinishedRequest(workDir);
        }
    }

    private DeploymentStatusReport findReportInWorkDir(String workDir) {
        File statusFile = getStatusFilePath(workDir).toFile();
        String statusJson = "";
        if (!statusFile.exists()) {
            throw new NoReportFileException(String.format("Status of [%s] could not be found", workDir));
        }
        try {
            statusJson = FileUtils.readFileToString(statusFile, UTF_8);
        } catch (IOException e) {
            handleException(e, "Could not read [%s]", statusFile.toString());
        }
        try {
            return mapper.readValue(statusJson, DeploymentStatusReport.class);
        } catch (IOException e) {
            return handleException(e, "Could not parse [%s] to DeploymentStatusReport", statusJson);
        }
    }

    private void handleFinishedRequest(String workDir) {
        if (!finished.containsKey(workDir)) {
            finished.put(workDir, now());
        } else if (now().isAfter(finished.get(workDir).plusSeconds(DEPLOYMENT_MEMORY_SPAN))) {
            reports.remove(workDir);
            consumers.remove(workDir);
            finished.remove(workDir);
        }
    }

    private void saveToFile(DeploymentStatusReport report) {
        Path file = getStatusFilePath(report.getWorkDir());
        try {
            String json = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(report);
            FileUtils.write(file.toFile(), json, UTF_8);
        } catch (IOException e) {
            handleException(e, "Could create status report file [%s]", file.toString());
        }
    }

    private Path getStatusFilePath(String workDir) {
        return Paths.get(
                deploymentRoot,
                workDir,
                statusFileName
        );
    }

    public void clearAll() {
        this.reports.clear();
        this.consumers.clear();
        this.finished.clear();
    }

}
