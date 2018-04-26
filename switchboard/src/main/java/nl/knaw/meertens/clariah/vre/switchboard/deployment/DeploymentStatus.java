package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import java.util.Arrays;

/**
 * Valid http responses during deployment of
 * service and polling of deployed service.
 */
public enum DeploymentStatus {

    FINISHED(200, true),

    RUNNING(202, true),

    NOT_FOUND(404, true),

    /**
     * Not implemented
     */
    STOPPED(0, false),

    /**
     * Status is ALREADY_RUNNING when requesting existing deployment
     */
    ALREADY_RUNNING(403, false),

    /**
     * Status is DEPLOYED during deployment request, RUNNING afterwards
     */
    DEPLOYED(200, false);

    private final int httpStatus;

    /**
     * A http status is 'pollable' when it can be received during
     * poll request, as opposed to deployment request
     */
    private final boolean pollable;

    DeploymentStatus(int httpStatus, boolean pollable) {
        this.httpStatus = httpStatus;
        this.pollable = pollable;
    }

    /**
     * Status that a deployed service can return
     * when polling for its status
     */
    public boolean isPollStatus() {
        return pollable;
    }

    /**
     * Status that the deployment service can return
     * when deploying a new status
     */
    public boolean isDeployStatus() {
        return !pollable;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Get poll status corresponding to http status code
     */
    public static DeploymentStatus getPollStatus(int httpStatus) {
        for (DeploymentStatus deploymentStatus : DeploymentStatus.values()) {
            if (deploymentStatus.isPollStatus() && deploymentStatus.httpStatus == httpStatus) {
                return deploymentStatus;
            }
        }
        throw new IllegalArgumentException(String.format(
                "Response of status request is unexpected: was [%s] but should be in [%s]",
                httpStatus, Arrays.toString(getAllPollStatuses())
        ));
    }

    /**
     * Get deployment status corresponding to http status code
     */
    public static DeploymentStatus getDeployStatus(int httpStatus) {
        for (DeploymentStatus deploymentStatus : DeploymentStatus.values()) {
            if (!deploymentStatus.pollable && deploymentStatus.httpStatus == httpStatus) {
                return deploymentStatus;
            }
        }
        throw new IllegalArgumentException(String.format(
                "Response of requested deployment is unexpected: was [%s] but should be in [%s]",
                httpStatus, Arrays.toString(getAllDeployStatuses())
        ));
    }

    private static Object[] getAllDeployStatuses() {
        return Arrays.stream(DeploymentStatus.values())
                .filter(DeploymentStatus::isDeployStatus)
                .toArray();
    }

    private static Object[] getAllPollStatuses() {
        return Arrays.stream(DeploymentStatus.values())
                .filter(DeploymentStatus::isPollStatus)
                .toArray();
    }

}
