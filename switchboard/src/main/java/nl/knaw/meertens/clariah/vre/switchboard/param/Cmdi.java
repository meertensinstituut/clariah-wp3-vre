package nl.knaw.meertens.clariah.vre.switchboard.param;

import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind;

import java.util.ArrayList;
import java.util.List;

public class Cmdi {

    public Long id;
    public String name;
    public ServiceKind kind;
    public List<Param> params = new ArrayList<>();

}
