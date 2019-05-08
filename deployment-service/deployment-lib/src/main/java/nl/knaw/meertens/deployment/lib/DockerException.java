package nl.knaw.meertens.deployment.lib;

public class DockerException extends Exception {
  public DockerException(String message) {
    super(message);
  }

  public DockerException(String message, Exception exception) {
    super(message, exception);
  }

}
