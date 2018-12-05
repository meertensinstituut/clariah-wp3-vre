package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamGroup;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentRequestDto {

  public List<ParamGroup> params = new ArrayList<>();

}
