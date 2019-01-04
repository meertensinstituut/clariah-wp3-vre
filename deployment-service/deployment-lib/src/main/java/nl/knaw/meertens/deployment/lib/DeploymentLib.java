package nl.knaw.meertens.deployment.lib;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.Saxon;

import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.USER_CONF_FILE;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class DeploymentLib {

  private static JsonNodeFactory jsonFactory = new JsonNodeFactory(false);
  private static ObjectMapper parser = new ObjectMapper();

  /**
   * @throws RecipePluginException when work dir does not exist
   */
  public static void workDirExists(String workDir) throws RecipePluginException {
    File file = Paths.get(ROOT_WORK_DIR, workDir).toFile();
    if (!file.exists()) {
      throw new RecipePluginException("work dir does not exist");
    }
  }

  public Service getServiceByName(String serviceName) throws IOException {
    String dbApiKey = System.getenv("APP_KEY_SERVICES");
    String servicesDatabase = "http://dreamfactory:80/api/v2/services";
    String urlString = servicesDatabase + "/_table/service/?filter=name%3D" + serviceName;

    HttpResponse<String> result;
    try {
      result = Unirest
        .get(urlString)
        .header("Content-Type", "application/json")
        .header("accept", "application/json")
        .header("X-DreamFactory-Api-Key", dbApiKey)
        .asString();
    } catch (UnirestException e) {
      throw new RuntimeException(String.format("Could not get service by name [%s]", serviceName));
    }

    JsonNode json = parser.readTree(result.getBody());
    JsonNode resource = json.get("resource");

    if (isNull(resource) || resource.size() == 0) {
      throw new IllegalStateException("No such service");
    }

    JsonNode record = resource.get(0);
    String serviceRecipe = record.get("recipe").asText();
    String serviceSemantics = record.get("semantics").asText();
    String serviceId = record.get("id").asText();
    return new Service(serviceId, serviceName, serviceRecipe, serviceSemantics, "");

  }

  public static ObjectNode parseSemantics(String symantics) throws RecipePluginException {
    try {
      ObjectNode parametersJson = jsonFactory.objectNode();

      Map<String, String> nameSpace = new LinkedHashMap<>();
      nameSpace.put("cmd", "http://www.clarin.eu/cmd/1");
      nameSpace.put("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011");

      StringReader reader = new StringReader(symantics);
      XdmNode service = Saxon.buildDocument(new StreamSource(reader));


      int counter = 0;
      for (XdmItem param : Saxon
        .xpath(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:Parameter", null, nameSpace)) {

        ObjectNode json = jsonFactory.objectNode();
        String parameterName = Saxon.xpath2string(param, "cmdp:Name", null, nameSpace);
        String parameterDescription = Saxon.xpath2string(param, "cmdp:Description", null, nameSpace);
        String parameterType = Saxon.xpath2string(param, "cmdp:MIMEType", null, nameSpace);

        json.put("parameterName", parameterName);
        json.put("parameterDescription", parameterDescription);
        json.put("parameterType", parameterType);

        int valueCounter = 0;
        for (XdmItem paramValue : Saxon
          .xpath(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:Parameter/cmdp:ParameterValue",
            null, nameSpace)) {

          ObjectNode parameterValueJson = jsonFactory.objectNode();

          String parameterValueValue = Saxon.xpath2string(paramValue, "cmdp:Value", null, nameSpace);
          String parameterValueDescription = Saxon.xpath2string(paramValue, "cmdp:Description", null, nameSpace);

          parameterValueJson.put("parameterValueValue", parameterValueValue);
          parameterValueJson.put("parameterValueDescription", parameterValueDescription);

          json.set("value" + Integer.toString(valueCounter), parameterValueJson);

          valueCounter++;
        }

        parametersJson.set("parameter" + Integer.toString(counter), json);

        counter++;
      }

      ObjectNode json = jsonFactory.objectNode();
      String serviceName = Saxon.xpath2string(service, "cmdp:Name", null, nameSpace);
      String serviceDescription = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Description", null, nameSpace);
      String serviceLocation = Saxon.xpath2string(
        service, "//cmdp:ServiceDescriptionLocation/cmdp:Location", null, nameSpace);

      json.put("serviceName", serviceName);
      json.put("serviceDescription", serviceDescription);
      json.put("serviceLocation", serviceLocation);
      json.put("counter", 0);
      json.set("parameters", parametersJson);
      return json;
    } catch (SaxonApiException ex) {
      throw new RecipePluginException("Invalid semantics xml", ex);
    }
  }

  public static ObjectNode parseUserConfig(String workDir) throws RecipePluginException {
    ObjectMapper parser = new ObjectMapper();
    if (isEmpty(workDir)) {
      throw new RuntimeException("working directory is empty");
    }

    String path = Paths
      .get(ROOT_WORK_DIR, workDir, USER_CONF_FILE)
      .normalize().toString();

    try {
      return (ObjectNode) parser.readTree(new FileReader(path));
    } catch (IOException e) {
      throw new RecipePluginException(String.format("could not read config file [%s]", path), e);
    }
  }
}
