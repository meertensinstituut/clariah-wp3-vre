package nl.knaw.meertens.clariah.vre.switchboard;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SwitchboardDiBinderTest extends AbstractTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Test if SwitchboardDiBinder binds all magical jersey classes
   * (those annotated with @Path or @Provider)
   */
  @Test
  public void testAllControllersAreIncluded() {
    var reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage("nl.knaw.meertens.clariah.vre.switchboard"))
      .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
    );

    Set<Class<?>> magicalClasses = new HashSet<>();
    magicalClasses.addAll(reflections.getTypesAnnotatedWith(Path.class));
    magicalClasses.addAll(reflections.getTypesAnnotatedWith(Provider.class));

    logger.info("Classes annotated with Path or Provider: "
      + Arrays.toString(magicalClasses.toArray()));
    logger.info("Controllers defined in SwitchboardDiBinder: "
      + Arrays.toString(SwitchboardDiBinder.getControllerClasses().toArray()));

    var binderContainsAllControlles = SwitchboardDiBinder
      .getControllerClasses()
      .containsAll(magicalClasses);
    assertThat(SwitchboardDiBinder.getControllerClasses().size()).isEqualTo(magicalClasses.size());
    assertThat(binderContainsAllControlles).isTrue();
  }

}
