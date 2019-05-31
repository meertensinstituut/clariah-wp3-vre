package nl.knaw.meertens.clariah.vre.tracker;

import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;

import static java.util.Arrays.asList;

public class ProvService {

  private static final String VRE_PREFIX = "clariah-wp3-vre";
  private static final String PROV_NAMESPACE = "https://github.com/meertensinstituut/clariah-wp3-vre";

  private final ProvFactory provFactory;
  private final Namespace namespace;

  public ProvService(ProvFactory provFactory) {
    this.provFactory = provFactory;
    namespace = new Namespace();
    namespace.addKnownNamespaces();
    namespace.register(VRE_PREFIX, PROV_NAMESPACE);
  }

  public Document createProv() {

    var a1 = provFactory.newAgent(qn("agent1"), "Agent One Full Name");
    var a2 = provFactory.newAgent(qn("agent2"), "Agent Two Full Name");

    var v1 = "LittleSemanticsWeb.html";
    var e1 = provFactory.newEntity(namespace.qualifiedName(VRE_PREFIX, v1, provFactory));

    var v2 = "A little provenance goes a long way";
    var e2 = "a-little-provenance-goes-a-long-way";

    var eqn = provFactory.newEntity(qn(e2));
    eqn.setValue(provFactory.newValue(v2, provFactory.getName().XSD_STRING));

    var wat1 = provFactory.newWasAttributedTo(null, eqn.getId(), a1.getId());

    var wat2 = provFactory.newWasAttributedTo(null, eqn.getId(), a2.getId());

    var wdf1 = provFactory.newWasDerivedFrom(eqn.getId(), e1.getId());

    var document = provFactory.newDocument();
    document
      .getStatementOrBundle()
      .addAll(asList(eqn, a1, a2, wat1, wat2, e1, wdf1));

    document.setNamespace(namespace);

    return document;
  }

  private QualifiedName qn(String local) {
    return namespace.qualifiedName(VRE_PREFIX, local, provFactory);
  }


}
