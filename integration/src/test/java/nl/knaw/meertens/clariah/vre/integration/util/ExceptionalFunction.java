package nl.knaw.meertens.clariah.vre.integration.util;

import java.util.function.Function;

/**
 * Consumer that allows its method to throw an exception
 *
 * @param <T>
 */
@FunctionalInterface
public interface ExceptionalFunction<T, R> extends Function<T, R> {

  @Override
  default R apply(T elem) {
    try {
      return applyWithException(elem);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  R applyWithException(T elem) throws Exception;

}
