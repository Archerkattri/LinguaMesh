package io.linguamesh.icu4j;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerRegistry;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Icu4jTokenizerProviderTest {
    @Test
    public void registersWithServiceLoader() {
        assertTrue(TextTokenizerRegistry.discover().getProviderIds().contains("icu4j"));
    }

    @Test
    public void tokenOffsetsReferToOriginalTextForMixedScripts() {
        String text = "Café 中文ภาษาไทย مرحبا";
        TextTokenizer tokenizer = new Icu4jTokenizerProvider().create("und");
        List<TextToken> tokens = tokenizer.tokenize(text);

        assertFalse(tokens.isEmpty());
        for (TextToken token : tokens) {
            assertEquals(token.getText(), text.substring(token.getStartOffset(), token.getEndOffset()));
        }
    }

    @Test
    public void emptyTextReturnsNoTokens() {
        assertEquals(0, new Icu4jTokenizerProvider().create("en").tokenize("").size());
    }

    @Test
    public void sourceOffsetsRemainUtf16IndicesAfterSupplementaryCharacters() {
        String text = "😀 Café 中文";
        List<TextToken> tokens = new Icu4jTokenizerProvider().create("en").tokenize(text);

        assertFalse(tokens.isEmpty());
        for (TextToken token : tokens) {
            assertEquals(token.getText(), text.substring(token.getStartOffset(), token.getEndOffset()));
        }
        assertEquals(3, tokens.get(0).getStartOffset());
    }
}
