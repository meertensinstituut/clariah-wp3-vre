package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestRepoServiceStub {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final RequestRepoServiceStub instance = new RequestRepoServiceStub();
    private final Map<String, ExceptionalConsumer<DeploymentStatusReport>> consumers = new HashMap<>();
    private final Map<String, DeploymentRequest> requests = new HashMap<>();

    private RequestRepoServiceStub() {}

    public static RequestRepoServiceStub getInstance() {
        return instance;
    }

    public ExceptionalConsumer<DeploymentStatusReport> getConsumer(String workDir) {
        return consumers.get(workDir);
    }

    public DeploymentRequest getRequest(String workDir) {
        DeploymentRequest request = requests.get(workDir);
        if(request == null) {
            String keys = Arrays.toString(requests.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()).toArray());
            throw new IllegalArgumentException(String.format("Request with workDir [%s] does not exist in workDirs: [%s]", workDir, keys));
        }
        return request;
    }

    public void saveDeploymentRequest(
            DeploymentRequest request,
            ExceptionalConsumer<DeploymentStatusReport> finishRequest
    ) {
        logger.info(String.format("Persist deployment request of workDir [%s]", request.getWorkDir()));
        this.requests.put(request.getWorkDir(), request);
        this.consumers.put(request.getWorkDir(), finishRequest);
    }

}
