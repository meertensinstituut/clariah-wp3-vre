package nl.knaw.meertens.deployment.api;

import java.io.IOException;
import java.net.MalformedURLException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.Service;

import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * exposed at "service" path
 */
@Path("/service")
public class WebServiceClass {
    
    
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "json" media type.
     *
     * @return String that will be returned as a json response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllServices() {
        JSONArray json = new JSONArray();
        json.add("1. List all available services: \n");
        json.add("http://localhost/deployment-service/a/service/");
        json.add("2. get service by ID");
        json.add("http://localhost/deployment-service/a/service/<CLAM>");
        return json.toString();
    }
    
    @GET
    @Path("/{service}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getService(@PathParam("service") String service) throws ConfigurationException, IOException, MalformedURLException, ParseException {
        DeploymentLib dplib = new DeploymentLib();
        Service rp = dplib.getServiceByName(service);
        JSONObject json = new JSONObject();
        json.put("serviceName", rp.getName());
        json.put("serviceId", rp.getServiceId());
        json.put("serviceSymantics", rp.getServiceSymantics());
        json.put("serviceTechInfo", rp.getServiceTechInfo());
        return json.toString();
    }

}
