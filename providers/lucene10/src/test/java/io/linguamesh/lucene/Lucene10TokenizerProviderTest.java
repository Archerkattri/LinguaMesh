package io.linguamesh.lucene;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Lucene10TokenizerProviderTest {
    @Test
    public void selectsLanguageSpecificAnalysisAndKeepsValidSourceOffsets() {
        assertTokenRanges("en", "The quick runners are running.");
        assertTokenRanges("zh", "自然语言处理支持中文分词");
    }

    @Test
    public void supportsAUnicodeFallbackForLanguagesWithoutDedicatedAnalyzers() {
        TextTokenizer tokenizer = new Lucene10TokenizerProvider().create("am");
        List<TextToken> tokens = tokenizer.tokenize("ሰላም ዓለም");
        assertFalse(tokens.isEmpty());
        assertTrue(tokens.get(0).getStartOffset() >= 0);
    }

    private static void assertTokenRanges(String languageTag, String text) {
        List<TextToken> tokens = new Lucene10TokenizerProvider().create(languageTag).tokenize(text);
        assertFalse(tokens.isEmpty());
        for (TextToken token : tokens) {
            assertTrue(token.getStartOffset() >= 0);
            assertTrue(token.getEndOffset() > token.getStartOffset());
            assertTrue(token.getEndOffset() <= text.length());
        }
    }
}
