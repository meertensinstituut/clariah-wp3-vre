package nl.knaw.meertens.clariah.vre.tagger.object_tag;

import java.util.HashMap;

public class UdateObjectTagDto {
    public HashMap<String, Object> params = new HashMap<>();

    public UdateObjectTagDto(Long _object, Long _new_tag, String _type, String _owner) {
        params.put("_object", _object);
        params.put("_new_tag", _new_tag);
        params.put("_type", _type);
        params.put("_owner", _owner);
    }

}
