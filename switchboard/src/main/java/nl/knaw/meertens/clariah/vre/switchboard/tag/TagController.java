package nl.knaw.meertens.clariah.vre.switchboard.tag;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/tags")
public class TagController extends AbstractController {

    @Inject
    TagService tagService;

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createTag(String body) {
        return null;
    }

}