package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.annotation.JsonFormat;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.util.stream.Collectors.groupingBy;

public class DeploymentRequest {

    /**
     * Map<Object ID, File path relative to data directory>
     */
    private HashMap<Long, String> files;

    private DeploymentStatusReport statusReport;

    @JsonFormat(shape=STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
    private final LocalDateTime dateTime;

    private final String service;
    private final String workDir;
    private final List<ParamDto> dto;

    public DeploymentRequest(String service, String workDir, LocalDateTime dateTime, List<ParamDto> dto) {
        this.service = service;
        this.workDir = workDir;
        this.dateTime = dateTime;
        this.dto = dto;
        this.files = new HashMap<>();
    }

    public DeploymentStatusReport getStatusReport() {
        return statusReport;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public List<ParamDto> getParams() {
        return dto;
    }

    public HashMap<Long, String> getFiles() {
        return files;
    }

    public void setFiles(HashMap<Long, String> files) {
        this.files = files;
    }

    public String getService() {
        return service;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setStatusReport(DeploymentStatusReport statusReport) {
        this.statusReport = statusReport;
    }
}
