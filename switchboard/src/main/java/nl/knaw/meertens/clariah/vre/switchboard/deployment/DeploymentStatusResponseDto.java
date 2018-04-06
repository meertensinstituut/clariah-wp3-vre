package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentStatusResponseDto {
        public int data;
        public String id;
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
