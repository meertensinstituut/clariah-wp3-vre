/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import java.io.IOException;
import java.net.MalformedURLException;
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
}
