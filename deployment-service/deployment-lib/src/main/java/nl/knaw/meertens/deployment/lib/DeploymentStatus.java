package nl.knaw.meertens.deployment.lib;

import org.json.simple.JSONObject;

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

  public JSONObject toSimpleJson() {
    JSONObject status = new JSONObject();
    status.put("status", this.status);
    status.put("message", message);
    status.put("finished", finished);
    return status;
  }

  public DeploymentResponse toDeploymentResponse() {
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
