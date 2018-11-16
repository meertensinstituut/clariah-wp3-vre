package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

public abstract class AbstractController {

  @Inject
  ObjectMapper mapper;

  protected <T> Response createResponse(T response) {
    int httpStatus = 200;
    return createResponse(response, httpStatus);
  }

  protected <T> Response createResponse(T response, int httpStatus) {
    try {
      return Response
        .status(httpStatus)
        .entity(mapper.writeValueAsString(response))
        .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Could not create json of response", e);
    }
  }


}
