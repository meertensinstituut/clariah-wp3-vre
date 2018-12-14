package nl.knaw.meertens.deployment.lib;

import com.fasterxml.jackson.databind.JsonNode;
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

  private void setStatusFields(DeploymentStatus status) {
    body.put("status", status.getStatus());
    body.put("finished", status.isFinished());
    body.put("message", status.getMessage());
  }

  public DeploymentStatus getStatus() {
    return status;
  }

  public ObjectNode getBody() {
    return body;
  }

}
