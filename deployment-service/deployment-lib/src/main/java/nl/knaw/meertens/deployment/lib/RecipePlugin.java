/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import java.io.IOException;
import java.net.MalformedURLException;
import net.sf.saxon.s9api.SaxonApiException;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;

/**
 *
 * @author vic
 */
public interface RecipePlugin {
    Boolean finished();
    String execute(String key);
    JSONObject getStatus(String key) throws IOException, JDOMException, MalformedURLException;

    public void init(String wd, Service serviceObj) throws JDOMException, IOException, SaxonApiException;
}
