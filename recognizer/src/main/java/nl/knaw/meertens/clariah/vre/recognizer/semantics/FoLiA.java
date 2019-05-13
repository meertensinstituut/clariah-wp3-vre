/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.clariah.vre.recognizer.semantics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.Saxon;

/**
 *
 * @author menzowi
 */
public class FoLiA implements SemanticTypePlugin {
    
    private static final Map<String,String> NAMESPACES = new HashMap<>();
    
    static {
        NAMESPACES.put("folia","http://ilk.uvt.nl/folia");
    }
    
    
    @Override
    public List<String> detect(Path object) {
        List<String> types = new ArrayList<>();
        try {
            XdmNode folia = Saxon.buildDocument(new StreamSource(object.toFile()));
            if (Saxon.xpath2boolean(folia, "exists(/folia:FoLiA/folia:metadata/folia:annotations/folia:token-annotation)", null, NAMESPACES)) {
                types.add("folia.token");
            }
            if (Saxon.xpath2boolean(folia, "/folia:FoLiA/folia:metadata/folia:annotations/folia:pos-annotation/@set='https://raw.githubusercontent.com/proycon/folia/master/setdefinitions/frog-mbpos-cgn'", null, NAMESPACES)) {
                types.add("folia.pos");
                types.add("folia.pos.cgn");
            }
        } catch (SaxonApiException ex) {
            Logger.getLogger(FoLiA.class.getName()).log(Level.SEVERE, null, ex);
        }
        return types;
    }
    
}
