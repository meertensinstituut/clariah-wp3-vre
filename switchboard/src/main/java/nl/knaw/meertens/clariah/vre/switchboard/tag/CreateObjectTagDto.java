package nl.knaw.meertens.clariah.vre.switchboard.tag;

import java.util.ArrayList;
import java.util.List;

public class CreateObjectTagDto {
  public List<NameValueDto> params = new ArrayList<NameValueDto>();

  public CreateObjectTagDto(String owner, Long tag, Long object) {
    params.add(new NameValueDto("_tag", tag));
    params.add(new NameValueDto("_object", object));
    params.add(new NameValueDto("_owner", owner));
  }
}
