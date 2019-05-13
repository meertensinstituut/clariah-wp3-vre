/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.clariah.vre.recognizer.semantics;

import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author menzowi
 */
public interface SemanticTypePlugin {
    
    List<String> detect(Path object);
    
}
