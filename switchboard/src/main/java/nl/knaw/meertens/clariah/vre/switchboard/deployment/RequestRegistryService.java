package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestRegistryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final RequestRegistryService instance = new RequestRegistryService();
    private final Map<String, ExceptionalConsumer<DeploymentStatusReport>> consumers = new HashMap<>();
    private final Map<String, DeploymentStatusReport> requests = new HashMap<>();

    private RequestRegistryService() {}

    public static RequestRegistryService getInstance() {
        return instance;
    }

    public ExceptionalConsumer<DeploymentStatusReport> getConsumer(String workDir) {
        return consumers.get(workDir);
    }

    public DeploymentStatusReport getStatusReport(String workDir) {
        DeploymentStatusReport request = requests.get(workDir);
        if(request == null) {
            String keys = Arrays.toString(requests.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()).toArray());
            throw new IllegalArgumentException(String.format("Request with workDir [%s] does not exist in workDirs: [%s]", workDir, keys));
        }
        return request;
    }

    public void saveDeploymentRequest(
            DeploymentStatusReport statusReport,
            ExceptionalConsumer<DeploymentStatusReport> finishRequest
    ) {
        logger.info(String.format("Persist deployment request of workDir [%s]", statusReport.getWorkDir()));
        this.requests.put(statusReport.getWorkDir(), statusReport);
        this.consumers.put(statusReport.getWorkDir(), finishRequest);
    }

}
