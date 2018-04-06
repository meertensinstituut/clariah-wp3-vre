/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    public String execute(String key) {
        System.out.println(String.format("Create TEST deployment with workDir [%s]", key));
        try {
            System.out.println("Start 5 second run...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.out.println("Test service was interrupted.");
        }
        Path outputFile = Paths.get("/tmp/wd/" + key + "/output/result.txt");
        outputFile.toFile().getParentFile().mkdirs();
        System.out.println(String.format("Created outputFile [%s]", outputFile.toString()));
        try {
            String sentence = "Insanity: doing the same thing over and over again and expecting different results.";
            FileUtils.write(outputFile.toFile(), sentence, Charsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Could not generate output for " + key);
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
        return json;
    }

}
