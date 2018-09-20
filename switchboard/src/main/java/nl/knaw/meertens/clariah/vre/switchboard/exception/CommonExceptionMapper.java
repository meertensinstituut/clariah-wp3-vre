package nl.knaw.meertens.clariah.vre.switchboard.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.MediaType.*;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;

/**
 * Jersey Exception Mapper that catches all Throwables,
 * logs them as an error and returns e.message as Response.
 */
@Provider
public class CommonExceptionMapper extends AbstractController implements ExceptionMapper<Throwable> {

    private static ObjectMapper mapper = getMapper();

    private static final Logger logger = LoggerFactory.getLogger(CommonExceptionMapper.class);

    @Produces({APPLICATION_JSON})
    @Override
    public Response toResponse(Throwable e) {
        logger.error(e.getMessage(), e);
        if (e instanceof NoReportFileException || e instanceof NotFoundException) {
            return createResponse(e, 404);
        }
        return createResponse(e, 500);
    }

    private Response createResponse(Throwable e, int httpStatus) {
        SwitchboardMsg msg = new SwitchboardMsg(e.getMessage());
        return createResponse(msg, httpStatus);
    }
}
