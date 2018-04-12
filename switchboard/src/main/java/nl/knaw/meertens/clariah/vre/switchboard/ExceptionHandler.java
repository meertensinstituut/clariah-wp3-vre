package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;

public class ExceptionHandler {

    private static final ObjectMapper mapper = getMapper();

    private ExceptionHandler() {}

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    public static void handleException(Throwable e, String template, String... args) {
        String msg = String.format(template, Arrays.stream(args).toArray());
        throw new RuntimeException(msg, e);
    }

    public static void handleException(String template, String... args) {
        String msg = String.format(template, Arrays.stream(args).toArray());
        throw new RuntimeException(msg);
    }

    public static Response handleInternalServerError(Exception e) throws JsonProcessingException {
        logger.error(e.getMessage(), e);
        return Response
                .status(500)
                .entity(mapper.writeValueAsString(new SwitchboardMsg(
                        e.getMessage()
                ))).build();
    }

}
