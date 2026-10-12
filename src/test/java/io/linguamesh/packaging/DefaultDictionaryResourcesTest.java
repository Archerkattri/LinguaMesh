package io.linguamesh.packaging;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DefaultDictionaryResourcesTest {
    @Test
    public void defaultConfigurationAndDictionariesArePackagedAsClasspathResources() throws IOException {
        ClassLoader loader = getClass().getClassLoader();
        for (String resource : Arrays.asList(
                "ansj_library.properties",
                "library/default.dic",
                "library/ambiguity.dic",
                "library/synonyms.dic",
                "library/stop.dic")) {
            try (InputStream input = loader.getResourceAsStream(resource)) {
                assertNotNull(resource, input);
                assertTrue(resource, input.read() >= 0);
            }
        }

    }
}
