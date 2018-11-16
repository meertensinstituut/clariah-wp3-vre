package nl.knaw.meertens.clariah.vre.tagger.objecttag;

import java.util.HashMap;

public class ObjectTagDto {
  public HashMap<String, Object> params = new HashMap<>();

  public ObjectTagDto(String owner, Long tag, Long object) {
    params.put("_tag", tag);
    params.put("_object", object);
    params.put("_owner", owner);
  }

}
