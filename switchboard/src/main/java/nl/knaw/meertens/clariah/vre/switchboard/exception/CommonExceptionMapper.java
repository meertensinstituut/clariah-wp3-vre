package nl.knaw.meertens.clariah.vre.switchboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Arrays;

import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;

/**
 * Jersey Exception Mapper that catches all Throwables,
 * logs them as an error and returns e.message as Response.
 */
public class CommonExceptionMapper implements ExceptionMapper<Throwable> {

    private static ObjectMapper mapper = getMapper();

    private static final Logger logger = LoggerFactory.getLogger(CommonExceptionMapper.class);

    @Override
    public Response toResponse(Throwable e) {
        logger.error(e.getMessage(), e);
        if(e instanceof NoReportFileException) {
            return createResponse(e, 404);
        }
        return createResponse(e, 500);
    }

    private Response createResponse(Throwable e, int httpStatus) {
        try {
            return Response
                    .status(httpStatus)
                    .entity(mapper.writeValueAsString(new SwitchboardMsg(
                            e.getMessage()
                    ))).build();
        } catch (JsonProcessingException eJson) {
            return createResponse(eJson, 500);
        }
    }
}
