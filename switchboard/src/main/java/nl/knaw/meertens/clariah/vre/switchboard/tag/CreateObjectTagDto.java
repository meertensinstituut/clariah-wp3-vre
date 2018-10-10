package nl.knaw.meertens.clariah.vre.switchboard.tag;

import java.util.ArrayList;
import java.util.List;

public class CreateObjectTagDto {
    public List<NameValueDto> params = new ArrayList<>();

    public CreateObjectTagDto(String _owner, Long _tag, Long _object) {
        params.add(new NameValueDto("_tag", _tag));
        params.add(new NameValueDto("_object", _object));
        params.add(new NameValueDto("_owner", _owner));
    }
}
