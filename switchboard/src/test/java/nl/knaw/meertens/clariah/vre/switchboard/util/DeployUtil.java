package nl.knaw.meertens.clariah.vre.switchboard.util;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.param.Param;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static org.assertj.core.util.Lists.newArrayList;

public class DeployUtil {

  private static Logger logger = LoggerFactory.getLogger(DeployUtil.class);

  public static DeploymentRequestDto getDeploymentRequestDto(String id, String longName) {
    var deploymentRequest = new DeploymentRequestDto();
    var paramGroup = new ParamGroup();
    paramGroup.name = "untokinput";
    paramGroup.type = FILE;
    paramGroup.value = id;
    var param = new Param();
    param.name = "language";
    param.value = "eng";
    var param2 = new Param();
    param2.name = "author";
    param2.value = longName;
    paramGroup.params = newArrayList(param, param2);
    deploymentRequest.params.add(paramGroup);
    return deploymentRequest;
  }

  public static DeploymentRequestDto getViewerDeploymentRequest(String id) {
    var deploymentRequestDto = new DeploymentRequestDto();
    var paramDto = new ParamGroup();
    paramDto.name = "input";
    paramDto.type = FILE;
    paramDto.value = id;
    deploymentRequestDto.params.add(paramDto);
    return deploymentRequestDto;
  }

  /**
   * Wait until switchboard endpoint of {request} returns {deploymentStatus}
   * (Max 10 seconds)
   */
  public static String waitUntil(Invocation.Builder request, DeploymentStatus deploymentStatus)
    throws InterruptedException, AssertionError {

    var timeout = 500;
    var iterations = 20;

    logger.info(format("Wait until status [%s]", deploymentStatus));
    var httpStatus = 0;
    var json = "";
    for (var i = 0; i < iterations; i++) {
      var response = request.get();
      httpStatus = response.getStatus();
      json = response.readEntity(String.class);
      String status = JsonPath.parse(json).read("$.status");
      if (status.equals(deploymentStatus.toString())) {
        logger.info(format("Status is [%s]", deploymentStatus));
        return json;
      }
      TimeUnit.MILLISECONDS.sleep(timeout);
    }
    throw new AssertionError(format("Status [%s] not found in [%d][%s]", deploymentStatus, httpStatus, json));
  }
}
