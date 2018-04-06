package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static nl.knaw.meertens.clariah.vre.switchboard.App.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.*;
import static org.assertj.core.util.Lists.newArrayList;

/**
 * Stub deployment service that 'deploys' services
 * which 1) generate a stubresult.txt after 5 seconds
 * and 2) can be stopped.
 */
public class DeploymentServiceStub implements DeploymentService {

    private static final DeploymentServiceStub instance = new DeploymentServiceStub();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Running, stopped and finished services:
     */
    private Map<String, DeploymentRequest> allServices = new HashMap<>();
    private Map<String, LocalDateTime> finishTime = new HashMap<>();
    private Map<String, ExceptionalConsumer<DeploymentStatusReport>> consumers = new HashMap<>();
    private List<String> stopped = new ArrayList<>();
    private List<String> finished = new ArrayList<>();

    private static final int runningTimeInSeconds = 5;
    public static final String STUBRESULT_FILENAME = "stubresult.txt";

    private DeploymentServiceStub () {
        startPolling();
    }

    /**
     * Use a singleton to keep track of deployed services
     */
    public static DeploymentServiceStub getInstance() {
        return instance;
    }

    @Override
    public DeploymentStatusReport start(DeploymentRequest request, ExceptionalConsumer<DeploymentStatusReport> finishRequest) {
        if (request.getFiles().isEmpty()) {
            throw new IllegalArgumentException(
                    "Deployment request always needs at least one input file");
        }
        allServices.put(request.getWorkDir(), request);
        consumers.put(request.getWorkDir(), finishRequest);
        finishTime.put(request.getWorkDir(), LocalDateTime.now().plusSeconds(runningTimeInSeconds));
        DeploymentStatusReport result = new DeploymentStatusReport();
        result.setMsg("Stub deployed");
        result.setUri(URI.create("http://example.com"));
        result.setStatus(DeploymentStatus.DEPLOYED);
        return result;
    }

    @Override
    public DeploymentStatusReport pollStatus(String workDir){
        if (!allServices.containsKey(workDir)) {
            List<String> keys = allServices.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
            throw new IllegalArgumentException(String.format(
                    "deployed service [%s] does not exist in [%s]", workDir, keys));
        }
        DeploymentRequest request = allServices.get(workDir);
        if (stopped.contains(workDir)) {
            return createStoppedResult();
        }
        if (finished.contains(workDir)) {
            return createFinishedResult(request);
        }
        return createRunningResult();
    }

    @Override
    public boolean stop(String workDir) {
        if (stopped.contains(workDir)) {
            throw new IllegalArgumentException(String.format(
                    "Service [%s] was already stopped", workDir));
        }
        return stopped.add(workDir);
    }

    private void startPolling() {
        logger.info("Start polling deployed services...");
        TimerTask poller = new TimerTask() {
            public void run() {
                pollAndConsume();
            }
        };
        Timer timer = new Timer("pollerTimer");
        long oneSecond = 1000L;
        timer.scheduleAtFixedRate(poller, oneSecond, oneSecond);
    }

    private void pollAndConsume() {
        allServices.forEach((workDir, deploymentRequest) -> {
            if(!finished.contains(workDir) && !stopped.contains(workDir)) {
                polRunningService(workDir, deploymentRequest);
            }
        });
    }

    private void polRunningService(String workDir, DeploymentRequest deploymentRequest) {
        if (LocalDateTime.now().isAfter(finishTime.get(workDir))) {
            finished.add(workDir);
            logger.info(String.format("Deployment [%s] finished.", workDir));
        }
        DeploymentStatusReport report = pollStatus(workDir);
        consumers.get(workDir).accept(report);
        logger.info(String.format("Polled [%s], instance of service [%s], status [%s].",
                workDir, deploymentRequest.getService(), report.getStatus()));
    }

    private DeploymentStatusReport createFinishedResult(DeploymentRequest deploymentRequest) {
        createResultFile(deploymentRequest.getWorkDir());
        DeploymentStatusReport result = new DeploymentStatusReport();
        result.setStatus(DeploymentStatus.FINISHED);
        result.setMsg("{\"result\" : \"Computer says no.\"}");
        return result;
    }

    private DeploymentStatusReport createStoppedResult() {
        DeploymentStatusReport result = new DeploymentStatusReport();
        result.setStatus(DeploymentStatus.STOPPED);
        result.setMsg("{\"result\" : \"0\"}");
        return result;
    }

    private DeploymentStatusReport createRunningResult() {
        DeploymentStatusReport result = new DeploymentStatusReport();
        result.setStatus(DeploymentStatus.RUNNING);
        result.setMsg("{\"progress\" : \"42%\"}");
        return result;
    }

    private void createResultFile(String workDir) {
        Path path = Paths.get(DEPLOYMENT_VOLUME, workDir, OUTPUT_DIR, STUBRESULT_FILENAME);
        path.toFile().getParentFile().mkdirs();
        try {
            Files.write(path, newArrayList("Geen resultaat is ook een resultaat."), Charset.forName("UTF-8"));
        } catch (IOException e) {
            handleException(e, "DeploymentServiceStub could not create result file");
        }
    }

}
