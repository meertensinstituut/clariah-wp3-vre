/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.knaw.meertens.deployment.lib;

import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.configuration.ConfigurationException;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Logger;

/**
 * @author vic
 */
public interface RecipePlugin {
  Boolean finished();

  String execute(String key, Logger logger);

  //get internal status of deployment
  JSONObject getStatus(String pid) throws IOException, JDOMException, MalformedURLException;

  // get status of remote service
  // JSONObject getProjectStatus(String pid) throws IOException, MalformedURLException, JDOMException;
  public void init(String wd, Service serviceObj) throws JDOMException, IOException, SaxonApiException;

  public JSONObject parseUserConfig(String key) throws IOException, ParseException, ConfigurationException;

  public JSONObject parseSymantics(String symantics) throws JDOMException, IOException, SaxonApiException;

}
