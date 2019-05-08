package nl.knaw.meertens.clariah.vre.integration.util;

@FunctionalInterface
public interface Procedure {
  void execute() throws AssertionError;
}
