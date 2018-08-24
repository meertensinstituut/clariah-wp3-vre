package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

public class DeploymentStatusReport implements Cloneable {

    /**
     * Date and time that deployment was polled
     */
    private LocalDateTime polled;

    /**
     * Interval in seconds between previous and next poll
     */
    private int interval = Config.MIN_POLL_INTERVAL;

    private DeploymentStatus status;
    private String msg;
    private String outputDir;
    private String service;
    private URI uri;
    private String workDir;
    private List<String> files;
    private String viewerFile;
    private String viewerFileContent;

    public DeploymentStatusReport() {
        super();
    }

    /**
     * Make a copy
     * @param original
     */
    public DeploymentStatusReport(DeploymentStatusReport original) {
        this.polled = original.polled;
        this.interval = original.interval;
        this.status = original.status;
        this.msg = original.msg;
        this.outputDir = original.outputDir;
        this.service = original.service;
        this.uri = original.uri;
        this.workDir = original.workDir;
        this.files = original.files;
        this.viewerFile = original.viewerFile;
        this.viewerFileContent = original.viewerFileContent;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public String getWorkDir() {
        return workDir;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public List<String> getFiles() {
        return files;
    }

    public LocalDateTime getPolled() {
        return polled;
    }

    public void setPolled(LocalDateTime polled) {
        this.polled = polled;
    }


    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setViewerFile(String outputFile) {
        this.viewerFile = outputFile;
    }

    public String getViewerFile() {
        return viewerFile;
    }

    public void setViewerFileContent(String viewerFileContent) {
        this.viewerFileContent = viewerFileContent;
    }

    public String getViewerFileContent() {
        return viewerFileContent;
    }
}
