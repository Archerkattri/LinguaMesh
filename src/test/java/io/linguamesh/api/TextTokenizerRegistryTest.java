package io.linguamesh.api;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TextTokenizerRegistryTest {
    @Test
    public void findsExactPrimaryLanguageAndWildcardProvidersInStableOrder() {
        TextTokenizerRegistry registry = new TextTokenizerRegistry(Arrays.asList(
                new TestProvider("fallback", Collections.singleton("*")),
                new TestProvider("chinese", Collections.singleton("zh")),
                new TestProvider("english", Collections.singleton("en"))));

        List<TextTokenizerProvider> matches = registry.providersFor("zh-Hans-CN");
        assertEquals(2, matches.size());
        assertEquals("chinese", matches.get(0).getId());
        assertEquals("fallback", matches.get(1).getId());
        assertEquals("zh-Hans-CN", registry.create("chinese", "zh-hans-cn").getLanguageTag());
    }

    @Test
    public void rejectsUnknownProvidersAndUnsupportedLanguages() {
        TextTokenizerRegistry registry = new TextTokenizerRegistry(
                Collections.<TextTokenizerProvider>singletonList(
                        new TestProvider("english", Collections.singleton("en"))));

        assertThrows(IllegalArgumentException.class, () -> registry.create("missing", "en"));
        assertThrows(IllegalArgumentException.class, () -> registry.create("english", "fr"));
        assertThrows(IllegalArgumentException.class, () -> registry.providersFor("not a tag"));
    }

    @Test
    public void discoversBundledAnsjProvider() {
        TextTokenizerRegistry registry = TextTokenizerRegistry.discover();
        assertTrue(registry.getProviderIds().contains("ansj"));
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            fail("Expected " + type.getName());
        } catch (Throwable exception) {
            if (!type.isInstance(exception)) {
                throw new AssertionError("Unexpected exception", exception);
            }
        }
    }

    private static final class TestProvider implements TextTokenizerProvider {
        private final String id;
        private final Set<String> languageTags;

        private TestProvider(String id, Set<String> languageTags) {
            this.id = id;
            this.languageTags = languageTags;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Set<String> getLanguageTags() {
            return languageTags;
        }

        @Override
        public TextTokenizer create(String languageTag) {
            return new TextTokenizer() {
                @Override
                public String getProviderId() {
                    return id;
                }

                @Override
                public String getLanguageTag() {
                    return languageTag;
                }

                @Override
                public List<TextToken> tokenize(String text) {
                    return Collections.emptyList();
                }
            };
        }
    }
}
