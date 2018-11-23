package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import java.util.function.Consumer;

/**
 * Each time switchboard has polled deployment-service
 * to check the status of a deployment
 * a PollDeploymentConsumer is run.
 *
 */
@FunctionalInterface
public interface PollDeploymentConsumer extends Consumer<DeploymentStatusReport> {}
