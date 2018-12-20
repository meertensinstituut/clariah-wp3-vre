package nl.knaw.meertens.deployment.lib;


public enum DeploymentStatus {

  CREATED(201, "Task created", false),
  RUNNING(202, "Task running", false),
  FINISHED(200, "Task finished", true),
  NOT_FOUND(404, "Task not found", false),
  ERROR(500, "Task could not be deployed", false);

  private final int status;
  private final String message;
  private boolean finished;

  DeploymentStatus(int status, String message, boolean finished) {
    this.status = status;
    this.message = message;
    this.finished = finished;
  }

  public DeploymentResponse toResponse() {
    return new DeploymentResponse(this);
  }

  public int getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public boolean isFinished() {
    return finished;
  }

}
