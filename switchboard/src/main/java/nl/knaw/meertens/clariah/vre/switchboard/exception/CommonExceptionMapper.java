package nl.knaw.meertens.clariah.vre.switchboard.exception;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Jersey Exception Mapper that catches all Throwables,
 * logs them as an error and returns e.message as Response.
 */
@Provider
public class CommonExceptionMapper extends AbstractController implements ExceptionMapper<Throwable> {

  private static final Logger logger = LoggerFactory.getLogger(CommonExceptionMapper.class);

  @Produces({APPLICATION_JSON})
  @Override
  public Response toResponse(Throwable throwable) {
    logger.error(throwable.getMessage(), throwable);
    if (throwable instanceof NoReportFileException || throwable instanceof NotFoundException) {
      return createResponse(throwable, 404);
    }
    return createResponse(throwable, 500);
  }

  private Response createResponse(Throwable throwable, int httpStatus) {
    var msg = new SwitchboardMsg(throwable.getMessage());
    return createResponse(msg, httpStatus);
  }
}
