package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractController;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
    var tag = mapper.readValue(body, TagDto.class);
    var result = new TagDto();
    result.id = tagService.createTag(tag);
    return createResponse(result);
  }

  @POST
  @Path("/{tag}/objects")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response tagObject(
    @PathParam("tag") Long tag,
    String body
  ) {
    var object = JsonPath.parse(body).read("$.object", Long.class);
    var result = new ObjectTagDto();
    result.id = tagService.tagObject(object, tag);
    return createResponse(result);
  }

  @DELETE
  @Path("/{tag}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response deleteTag(
    @PathParam("tag") Long tag
  ) {
    var result = new TagDto();
    result.id = tagService.deleteTag(tag);
    return createResponse(result);
  }

  @DELETE
  @Path("/{tag}/objects/{object}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response untagObject(
    @PathParam("tag") Long tag,
    @PathParam("object") Long object
  ) {
    var result = new ObjectTagDto();
    result.id = tagService.untagObject(object, tag);
    return createResponse(result);
  }

}
