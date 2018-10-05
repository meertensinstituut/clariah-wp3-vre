package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/tags")
public class TagController extends AbstractController {

    @Inject
    TagService tagService;

    @Inject
    ObjectMapper mapper;

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createTag(String body) throws IOException {
        TagDto tag = mapper.readValue(body, TagDto.class);
        TagDto result = new TagDto();
        result.id = tagService.createTag(tag);
        return createResponse(result);
    }

}