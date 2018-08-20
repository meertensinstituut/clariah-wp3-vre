package nl.knaw.meertens.clariah.vre.switchboard.param;

import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind;

import java.util.ArrayList;
import java.util.List;

public class CmdiDto {

    public Long id;
    public String name;
    public ServiceKind kind;
    public List<ParamDto> params = new ArrayList<>();

}
