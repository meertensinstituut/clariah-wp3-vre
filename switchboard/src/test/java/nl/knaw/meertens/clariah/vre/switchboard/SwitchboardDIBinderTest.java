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
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SwitchboardDIBinderTest extends AbstractTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testAllControllersAreIncluded() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("nl.knaw.meertens.clariah.vre.switchboard"))
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
        );
        Set<Class<?>> allControllers = reflections.getTypesAnnotatedWith(Path.class);

        logger.info("All controllers (annotated with Path): "
                + Arrays.toString(allControllers.toArray()));
        logger.info("Controllers defined in SwitchboardDIBinder: "
                + Arrays.toString(SwitchboardDIBinder.getControllerClasses().toArray()));

        boolean binderContainsAllControlles = SwitchboardDIBinder
                .getControllerClasses()
                .containsAll(allControllers);
        assertThat(SwitchboardDIBinder.getControllerClasses().size()).isEqualTo(allControllers.size());
        assertThat(binderContainsAllControlles).isTrue();
    }

}
