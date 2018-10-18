package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import java.util.Arrays;

/**
 * Valid http responses during deployment of
 * service and polling of deployed service.
 */
public enum DeploymentStatus {

    /**
     * 201 Created
     * Status is DEPLOYED during deployment request, RUNNING afterwards
     */
    DEPLOYED(201),

    /**
     * 202 Accepted
     */
    RUNNING(202),

    /**
     * 200 OK
     */
    FINISHED(200),

    /**
     * 404 Not Found
     */
    NOT_FOUND(404),

    /**
     * Not implemented
     */
    STOPPED(0),

    /**
     * 403 Forbidden
     * Status is ALREADY_RUNNING when requesting existing deployment
     */
    ALREADY_RUNNING(403);

    private final int httpStatus;

    DeploymentStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * Get deployment status corresponding to http status code
     */
    public static DeploymentStatus getDeployStatus(int httpStatus) {
        for (var deploymentStatus : values()) {
            if (deploymentStatus.httpStatus == httpStatus) {
                return deploymentStatus;
            }
        }
        throw new IllegalArgumentException(String.format(
                "Response of requested deployment is unexpected: was [%s] but should be in [%s]",
                httpStatus, Arrays.toString(getAllDeployStatuses())
        ));
    }

    private static DeploymentStatus[] getAllDeployStatuses() {
        return (DeploymentStatus[]) Arrays
                .stream(values())
                .toArray();
    }

}
