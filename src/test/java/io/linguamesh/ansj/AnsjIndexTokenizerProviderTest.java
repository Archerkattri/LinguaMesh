package io.linguamesh.ansj;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerRegistry;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.IndexAnalysis;
import org.junit.Test;
import org.nlpcn.commons.lang.tire.domain.Forest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnsjIndexTokenizerProviderTest {
    @Test
    public void exposesOverlappingDictionaryMatchesWithSourceOffsets() {
        String text = "隐形眼镜";
        Forest forest = new Forest();
        forest.addBranch("隐形眼镜", new String[] {"n", "1000"});
        forest.addBranch("隐形", new String[] {"n", "1000"});
        forest.addBranch("眼镜", new String[] {"n", "1000"});

        List<Term> expected = IndexAnalysis.parse(text, forest).getTerms();
        TextTokenizer tokenizer = new AnsjIndexTokenizerProvider(forest).create("zh-Hans");
        List<TextToken> actual = tokenizer.tokenize(text);

        assertEquals("ansj-index", tokenizer.getProviderId());
        assertFalse(actual.isEmpty());
        assertEquals(expected.size(), actual.size());
        boolean hasOverlappingTerms = false;
        for (int i = 0; i < expected.size(); i++) {
            Term term = expected.get(i);
            TextToken token = actual.get(i);
            String surface = term.getRealName() == null ? term.getName() : term.getRealName();
            assertEquals(surface, token.getText());
            assertEquals(term.getOffe(), token.getStartOffset());
            assertEquals(term.getOffe() + surface.length(), token.getEndOffset());
            assertEquals(surface, text.substring(token.getStartOffset(), token.getEndOffset()));
            assertTrue(token.getAttributes().containsKey("partOfSpeech"));
            for (int j = i + 1; j < actual.size(); j++) {
                TextToken other = actual.get(j);
                if (token.getStartOffset() < other.getStartOffset()
                        && other.getStartOffset() < token.getEndOffset()) {
                    hasOverlappingTerms = true;
                }
            }
        }
        assertTrue("IndexAnalysis should retain nested dictionary matches", hasOverlappingTerms);
    }

    @Test
    public void isDiscoveredAlongsideTheDefaultAnsjProvider() {
        TextTokenizerRegistry registry = TextTokenizerRegistry.discover();

        assertTrue(registry.getProviderIds().contains("ansj"));
        assertTrue(registry.getProviderIds().contains("ansj-index"));
        assertEquals(2, registry.providersFor("zh-CN").size());
    }
}
