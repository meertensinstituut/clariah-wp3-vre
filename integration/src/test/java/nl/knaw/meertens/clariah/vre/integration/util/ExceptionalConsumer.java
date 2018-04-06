package nl.knaw.meertens.clariah.vre.integration.util;

import java.util.function.Consumer;

/**
 * Consumer that allows its method to throw an exception
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
