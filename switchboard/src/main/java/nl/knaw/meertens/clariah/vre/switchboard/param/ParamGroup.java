package nl.knaw.meertens.clariah.vre.switchboard.param;

import java.util.ArrayList;
import java.util.List;

/**
 * A ParamGroup is a Param that contains a list of Params
 */
public class ParamGroup extends Param {
  public List<Param> params = new ArrayList<>();
}
