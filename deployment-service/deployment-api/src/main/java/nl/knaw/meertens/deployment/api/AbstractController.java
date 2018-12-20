package nl.knaw.meertens.deployment.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public abstract class AbstractController {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private static JsonNodeFactory jsonFactory = new JsonNodeFactory(false);

  /**
   * Logs exception with error msg and creates json body with msg
   * @return Response with json
   */
  Response handleException(
    String msg,
    Throwable ex
  ) {
    logger.error(msg, ex);
    return handleException(msg);
  }

  /**
   * Logs error msg and creates json body with msg
   * @return Response with json
   */
  Response handleException(
    String msg
  ) {
    logger.error(msg);
    return handleException(msg, jsonFactory.objectNode());
  }

  /**
   * Logs error msg and adds error msg to json body
   * @return Response with json
   */
  Response handleException(String msg, ObjectNode body) {
    body.put("msg", msg);
    logger.error(msg);
    return Response
      .status(500)
      .entity(body.toString())
      .type(APPLICATION_JSON)
      .build();
  }

}
