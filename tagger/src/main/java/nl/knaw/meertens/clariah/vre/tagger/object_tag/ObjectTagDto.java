package nl.knaw.meertens.clariah.vre.tagger.object_tag;

import java.util.HashMap;

public class ObjectTagDto {
    public HashMap<String, Object> params = new HashMap<>();

    public ObjectTagDto(String _owner, Long _tag, Long _object) {
        params.put("_tag", _tag);
        params.put("_object", _object);
        params.put("_owner", _owner);
    }

}
