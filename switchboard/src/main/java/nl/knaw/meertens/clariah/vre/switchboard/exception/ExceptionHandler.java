package nl.knaw.meertens.clariah.vre.switchboard.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.SwitchboardMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Arrays;

public class ExceptionHandler {

    private static ObjectMapper mapper;

    private ExceptionHandler() {}

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    public static <T> T handleException(Throwable e, String template, String... args) {
        String msg = String.format(template, Arrays.stream(args).toArray());
        throw new RuntimeException(msg, e);
    }

    public static Response handleControllerException(Exception e) throws JsonProcessingException {
        logger.error(e.getMessage(), e);
        if(e instanceof NoReportFileException) {
            return createResponse(e, 404);
        }
        return createResponse(e, 500);
    }

    private static Response createResponse(Exception e, int httpStatus) throws JsonProcessingException {
        return Response
                .status(httpStatus)
                .entity(mapper.writeValueAsString(new SwitchboardMsg(
                        e.getMessage()
                ))).build();
    }

    public static void setMapper(ObjectMapper mapper) {
        ExceptionHandler.mapper = mapper;
    }
}
