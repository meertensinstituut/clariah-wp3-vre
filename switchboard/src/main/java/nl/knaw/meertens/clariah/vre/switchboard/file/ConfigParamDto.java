package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.param.Param;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamType;

import java.util.ArrayList;
import java.util.List;

public class ConfigParamDto {
  public String name;
  public ParamType type;
  public String value;
  public List<Param> params = new ArrayList<>();
}
