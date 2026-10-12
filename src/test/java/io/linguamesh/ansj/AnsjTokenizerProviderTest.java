package io.linguamesh.ansj;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AnsjTokenizerProviderTest {
    @Test
    public void preservesSourceOffsetsAndPartOfSpeechForChineseTokens() {
        String text = "自然语言处理很有用";
        TextTokenizer tokenizer = new AnsjTokenizerProvider().create("zh-Hans");
        List<TextToken> tokens = tokenizer.tokenize(text);

        assertFalse(tokens.isEmpty());
        for (TextToken token : tokens) {
            assertEquals(token.getText(), text.substring(token.getStartOffset(), token.getEndOffset()));
        }
    }
}
