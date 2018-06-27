package nl.knaw.meertens.clariah.vre.switchboard.param;

import java.util.ArrayList;
import java.util.List;

/**
 * A ParamGroup is a Param that contains a list of Params
 */
public class ParamGroupDto extends ParamDto {
    public List<ParamDto> params = new ArrayList<>();
}
