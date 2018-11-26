package nl.knaw.meertens.clariah.vre.switchboard.consumer;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;

import java.util.function.Consumer;

/**
 * Each time switchboard has polled deployment-service
 * a DeploymentConsumer is run to handle status changes.
 */
@FunctionalInterface
public interface DeploymentConsumer extends Consumer<DeploymentStatusReport> {}
