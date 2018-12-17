package nl.knaw.meertens.deployment.lib;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DeploymentResponse {

  private static JsonNodeFactory factory = new JsonNodeFactory(false);

  private DeploymentStatus status;
  private ObjectNode body;

  public DeploymentResponse(DeploymentStatus status) {
    this.status = status;
    this.body = factory.objectNode();
    setStatusFields(status);
  }

  public DeploymentResponse(DeploymentStatus status, ObjectNode body) {
    this.status = status;
    this.body = body;
    setStatusFields(status);
  }

  /**
   * Set status fields when they don't exist in body yet
   */
  private void setStatusFields(DeploymentStatus status) {
    if (!body.has("status")) {
      body.put("status", status.getStatus());
    }
    if (!body.has("finished")) {
      body.put("finished", status.isFinished());
    }
    if (!body.has("message")) {
      body.put("message", status.getMessage());
    }
  }

  public DeploymentStatus getStatus() {
    return status;
  }

  public ObjectNode getBody() {
    return body;
  }

}
