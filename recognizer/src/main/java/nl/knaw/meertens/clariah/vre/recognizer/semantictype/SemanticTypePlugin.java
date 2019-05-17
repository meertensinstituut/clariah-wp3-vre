package nl.knaw.meertens.clariah.vre.recognizer.semantictype;

import java.nio.file.Path;
import java.util.List;

public interface SemanticTypePlugin {

  List<String> detect(Path object);

}
