package nl.knaw.meertens.clariah.vre.tagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static TaggerService taggerService = new TaggerService();

    public static void main(String[] args) {
        taggerService.consumeRecognizer();
    }

}