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

    public static DeploymentRequestDto getViewerDeploymentRequestDto(String id) {
        var deploymentRequestDto = new DeploymentRequestDto();
        var paramDto = new ParamGroup();
        paramDto.name = "input";
        paramDto.type = FILE;
        paramDto.value = id;
        deploymentRequestDto.params.add(paramDto);
        return deploymentRequestDto;
    }

    public static String waitUntil(Invocation.Builder request, DeploymentStatus deploymentStatus) throws InterruptedException {
        logger.info(String.format("Wait until status [%s]", deploymentStatus));
        var httpStatus = 0;
        var json = "";
        for (int i = 0; i < 20; i++) {
            var response = request.get();
            httpStatus = response.getStatus();
            json = response.readEntity(String.class);
            String status = JsonPath.parse(json).read("$.status");
            if (status.equals(deploymentStatus.toString())) {
                logger.info(String.format("Status is [%s]", deploymentStatus));
                return json;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        throw new AssertionError(String.format("Deployment status [%s] not found in response [%d][%s]", deploymentStatus, httpStatus, json));
    }
}
