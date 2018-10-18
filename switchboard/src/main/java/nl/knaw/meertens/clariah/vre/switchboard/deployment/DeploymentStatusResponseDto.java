package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentStatusResponseDto {

    /**
     * Message of deployment service
     */
    public String message;

    /**
     * Json status field:
     */
    public int status;

    /**
     * Header status field:
     */
    public int httpStatus;
}
