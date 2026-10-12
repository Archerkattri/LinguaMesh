package io.linguamesh.packaging;

import org.ansj.domain.Result;
import org.ansj.library.AmbiguityLibrary;
import org.ansj.library.DicLibrary;
import org.ansj.library.StopLibrary;
import org.ansj.library.SynonymsLibrary;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.ansj.util.MyStaticValue;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DefaultDictionaryResourcesTest {
    @Test
    public void defaultsLoadFromClasspathWithoutWorkingDirectoryFiles() throws IOException {
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

        assertEquals("jar://library/default.dic", MyStaticValue.ENV.get(DicLibrary.DEFAULT));
        assertTrue(DicLibrary.get().size() > 0);
        assertNotNull(AmbiguityLibrary.get());
        assertNotNull(StopLibrary.get());
        assertNotNull(SynonymsLibrary.get());

        Result result = ToAnalysis.parse("自然语言处理支持中文分词");
        assertTrue(result.size() > 0);
    }
}
