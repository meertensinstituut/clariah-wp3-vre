/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import java.io.FileNotFoundException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
//import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.parser.ParseException;

/**
 *
 * @author vic
 */
public class Test implements RecipePlugin {
    protected int counter = 0;
    protected Boolean isFinished = false;
    
    @Override
    public Boolean finished() {
        return isFinished;
    }
    
    @Override
    public String execute(String key, Logger logger) {
        logger.info(String.format("Create TEST deployment with workDir [%s]", key));
        try {
            logger.info("Start 15 second run...");
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            logger.info("Test service was interrupted.");
            logger.info(e.getLocalizedMessage());
        }
        Path outputFile = Paths.get("/tmp/wd/" + key + "/output/result.txt");
        outputFile.toFile().getParentFile().mkdirs();
        logger.info(String.format("Creating outputFile [%s]", outputFile.toString()));
        try {
            String sentence = "Insanity: doing the same thing over and over again and expecting different results.";
            FileUtils.write(outputFile.toFile(), sentence, Charsets.UTF_8);
            logger.info(String.format("Created outputFile [%s]", outputFile.toString()));
        } catch (IOException e) {
            logger.info(String.format("Could not generate output for [%s]", key));
            logger.info(e.getLocalizedMessage());
        }
        this.isFinished = true;
        return "OK";
    }

    /**
     *
     * @param key
     * @return
     * @throws IOException
     * @throws MalformedURLException
     * @throws JDOMException
     */
    @Override
    public JSONObject getStatus(String key) throws IOException, MalformedURLException, JDOMException {
        JSONObject json = new JSONObject();
        json.put("key", key);
        json.put("finished", isFinished);
        json.put("success", isFinished);
        if(isFinished) {
            json.put("completion", 100L);
        } else {
            json.put("completion", 80L);
        }
        return json;
    }

    @Override
    public void init(String wd, Service serviceObj) throws JDOMException, IOException, SaxonApiException {
        System.out.println("init plugin");
    }

    @Override
    public JSONObject parseUserConfig(String key) throws FileNotFoundException, IOException, ParseException, ConfigurationException {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return new JSONObject();
    }

    @Override
    public JSONObject parseSymantics(String symantics) throws JDOMException, IOException, SaxonApiException {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return new JSONObject();
    }

}
