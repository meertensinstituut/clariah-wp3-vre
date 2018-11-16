package nl.knaw.meertens.clariah.vre.tagger.objecttag;

import java.util.HashMap;

public class UdateObjectTagDto {
  public HashMap<String, Object> params = new HashMap<>();

  public UdateObjectTagDto(Long object, Long newTag, String type, String owner) {
    params.put("_object", object);
    params.put("_new_tag", newTag);
    params.put("_type", type);
    params.put("_owner", owner);
  }

}
