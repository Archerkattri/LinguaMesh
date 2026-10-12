package io.linguamesh.opennlp;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerRegistry;
import opennlp.tools.tokenize.TokenizerModel;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenNlpTokenizerProviderTest {
    @Test
    public void registersWithServiceLoader() {
        assertTrue(TextTokenizerRegistry.discover().getProviderIds().contains("opennlp"));
    }

    @Test
    public void usesAnApplicationSuppliedModelAndPreservesSourceOffsets() throws IOException {
        TokenizerModel model;
        try (InputStream input = getClass().getResourceAsStream(
                "/opennlp-en-ud-ewt-tokens-1.3-2.5.4.bin")) {
            assertNotNull(input);
            model = new TokenizerModel(input);
        }
        TextTokenizer tokenizer = new OpenNlpTokenizerProvider(Collections.singletonMap("en", model))
                .create("en-US");
        String text = "😀 OpenNLP tokenizes text while preserving source spans.";
        List<TextToken> tokens = tokenizer.tokenize(text);

        assertFalse(tokens.isEmpty());
        boolean foundTokenAfterSupplementaryCharacter = false;
        for (TextToken token : tokens) {
            assertEquals(token.getText(), text.substring(token.getStartOffset(), token.getEndOffset()));
            foundTokenAfterSupplementaryCharacter |= token.getStartOffset() >= 3;
        }
        assertTrue(foundTokenAfterSupplementaryCharacter);
    }
}
