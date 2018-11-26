package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import nl.knaw.meertens.clariah.vre.switchboard.consumer.DeploymentConsumer;

/**
 * Deployment service:
 * - starts requested service,
 * - retrieves status of deployed services
 * - stops running service (when possible)
 */
public interface DeploymentService {

  /**
   * Deploy requested service.
   */
  DeploymentStatusReport deploy(DeploymentRequest request, DeploymentConsumer deploymentConsumer);

  /**
   * Get status of deployed service
   */
  DeploymentStatusReport getStatus(String workDir);

  /**
   * Returns true when stop signal is send, false otherwise.
   */
  boolean stop(String workDir);

}
