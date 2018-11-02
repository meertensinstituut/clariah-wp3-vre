package nl.knaw.meertens.clariah.vre.tagger;

import java.util.ArrayList;
import java.util.List;

public class ObjectTagDto {
    public List<NameValueDto> params = new ArrayList<>();

    public ObjectTagDto(String _owner, Long _tag, Long _object) {
        params.add(new NameValueDto("_tag", _tag));
        params.add(new NameValueDto("_object", _object));
        params.add(new NameValueDto("_owner", _owner));
    }

}
