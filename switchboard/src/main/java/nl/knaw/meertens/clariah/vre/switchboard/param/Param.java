package nl.knaw.meertens.clariah.vre.switchboard.param;

import java.util.List;

public class Param {
  public String name;
  public String label;
  public String description;
  public ParamType type;
  public String minimumCardinality;
  public String maximumCardinality;
  public ParamType valuesType;
  public List<ParamValueDto> values;
  public String value;
}
