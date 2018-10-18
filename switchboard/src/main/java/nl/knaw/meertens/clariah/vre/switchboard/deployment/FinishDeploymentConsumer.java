package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import java.util.function.Consumer;

/**
 * Consumer that should be run after a deployment
 * to unlock files, move results, etc.
 * <p>
 * Allows its accept-method to throw an exception,
 * which is converted to a RuntimeException.
 *
 * @param <T>
 */
@FunctionalInterface
public interface FinishDeploymentConsumer<T> extends Consumer<T> {

    @Override
    default void accept(final T elem) {
        try {
            acceptWithException(elem);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    void acceptWithException(T elem) throws Exception;

}
