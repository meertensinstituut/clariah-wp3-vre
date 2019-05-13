package nl.knaw.meertens.clariah.vre.recognizer.semantics;

import net.sf.saxon.s9api.SaxonApiException;
import nl.mpi.tla.util.Saxon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.mpi.tla.util.Saxon.xpath2boolean;

public class FoLiA implements SemanticTypePlugin {

  private static final Map<String, String> NAMESPACES = new HashMap<>();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  static {
    NAMESPACES.put("folia", "http://ilk.uvt.nl/folia");
  }

  @Override
  public List<String> detect(Path object) {
    var types = new ArrayList<String>();
    try {
      var folia = Saxon.buildDocument(new StreamSource(object.toFile()));
      if (xpath2boolean(folia, "exists(/folia:FoLiA/folia:metadata/folia:annotations/folia:token-annotation)", null,
        NAMESPACES)) {
        types.add("folia.token");
      }
      if (xpath2boolean(folia,
        "/folia:FoLiA/folia:metadata/folia:annotations/folia:pos-annotation/@set='https://raw.githubusercontent" +
          ".com/proycon/folia/master/setdefinitions/frog-mbpos-cgn'",
        null, NAMESPACES)) {
        types.add("folia.pos");
        types.add("folia.pos.cgn");
      }
    } catch (SaxonApiException ex) {
      logger.error("Could not detect folia semantics", ex);
    }
    return types;
  }

}
