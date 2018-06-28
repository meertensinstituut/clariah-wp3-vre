package nl.knaw.meertens.clariah.vre.switchboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;

public class ExceptionHandler {

    private static ObjectMapper mapper = getMapper();

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    private ExceptionHandler() {}

    /**
     * Handle exceptions with a msg template for String.format
     * Note: all format specifiers in template should be of type %s
     */
    public static <T> T handleException(Throwable e, String template, Object... args) {
        String msg = String.format(template, Arrays.stream(args).map(Object::toString).toArray());
        throw new RuntimeException(msg, e);
    }

    public static Response handleControllerException(Exception e) {
        logger.error(e.getMessage(), e);
        if(e instanceof NoReportFileException) {
            return createResponse(e, 404);
        }
        return createResponse(e, 500);
    }

    private static Response createResponse(Exception e, int httpStatus) {
        try {
            return Response
                    .status(httpStatus)
                    .entity(mapper.writeValueAsString(new SwitchboardMsg(
                            e.getMessage()
                    ))).build();
        } catch (JsonProcessingException e2) {
            return handleControllerException(e2);
        }
    }

    public static void setMapper(ObjectMapper mapper) {
        ExceptionHandler.mapper = mapper;
    }
}
