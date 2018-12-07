package nl.knaw.meertens.deployment.api;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public abstract class AbstractController {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

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
    return handleException(msg, new JSONObject());
  }

  /**
   * Logs error msg and adds error msg to json body
   * @return Response with json
   */
  Response handleException(String msg, JSONObject body) {
    body.put("msg", msg);
    logger.error(msg);
    return Response
      .status(500)
      .entity(body.toJSONString())
      .type(APPLICATION_JSON)
      .build();
  }

}
