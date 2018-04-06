package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import java.util.function.Consumer;

/**
 * Consumer that allows its method to throw an exception,
 * which is then converted to a RuntimeException
 * @param <T>
 */
@FunctionalInterface
public interface ExceptionalConsumer<T> extends Consumer<T> {

    @Override
    default void accept(final T elem){
        try {
            acceptWithException(elem);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    void acceptWithException(T elem) throws Exception;

}
