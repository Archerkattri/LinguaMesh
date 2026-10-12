package io.linguamesh.sudachi;

import com.worksap.nlp.sudachi.Config;
import com.worksap.nlp.sudachi.Dictionary;
import com.worksap.nlp.sudachi.Tokenizer;
import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerRegistry;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SudachiTokenizerProviderTest {
    private static Dictionary dictionary;

    @BeforeClass
    public static void loadDictionary() throws IOException {
        String configuredPath = System.getenv("SUDACHI_DICTIONARY_PATH");
        Assume.assumeTrue("Set SUDACHI_DICTIONARY_PATH to run Sudachi integration tests",
                configuredPath != null && !configuredPath.trim().isEmpty());
        Path dictionaryPath = Paths.get(configuredPath);
        Assume.assumeTrue("Sudachi dictionary file is unavailable", Files.isRegularFile(dictionaryPath));
        Config config = Config.defaultConfig();
        config.systemDictionary(dictionaryPath);
        dictionary = Dictionary.load(config);
    }

    @AfterClass
    public static void closeDictionary() throws IOException {
        if (dictionary != null) {
            dictionary.close();
        }
    }

    @Test
    public void registersJapaneseAndUsesTheConfiguredSplitMode() {
        SudachiTokenizerProvider provider = new SudachiTokenizerProvider(dictionary, Tokenizer.SplitMode.A);
        TextTokenizerRegistry registry = new TextTokenizerRegistry(java.util.Collections.singletonList(provider));

        assertEquals("ja", provider.getLanguageTags().iterator().next());
        assertEquals(1, registry.providersFor("ja-JP").size());
        assertTrue(TextTokenizerRegistry.discover().getProviderIds().contains("sudachi"));
        assertEquals(Arrays.asList("選挙", "管理", "委員", "会"), tokenTexts(
                registry.create("sudachi", "ja-JP").tokenize("選挙管理委員会")));
        assertEquals(Arrays.asList("選挙", "管理", "委員会"), tokenTexts(
                new SudachiTokenizerProvider(dictionary, Tokenizer.SplitMode.B)
                        .create("ja").tokenize("選挙管理委員会")));
        assertEquals(Collections.singletonList("選挙管理委員会"), tokenTexts(
                new SudachiTokenizerProvider(dictionary, Tokenizer.SplitMode.C)
                        .create("ja").tokenize("選挙管理委員会")));
    }

    @Test
    public void returnsJapaneseMetadataAndUtf16OffsetsAfterSupplementaryCharacters() {
        TextTokenizer tokenizer = new SudachiTokenizerProvider(dictionary, Tokenizer.SplitMode.A).create("ja");
        String text = "😀東京都へ行く";
        List<TextToken> tokens = tokenizer.tokenize(text);
        TextToken tokyo = tokens.stream()
                .filter(token -> "東京".equals(token.getText()))
                .findFirst()
                .orElse(null);

        assertNotNull(tokyo);
        assertEquals(2, tokyo.getStartOffset());
        assertEquals(4, tokyo.getEndOffset());
        assertEquals("東京", text.substring(tokyo.getStartOffset(), tokyo.getEndOffset()));
        assertEquals("Sudachi", tokyo.getAttributes().get("analysisEngine"));
        assertNotNull(tokyo.getAttributes().get("partOfSpeech"));
        assertNotNull(tokyo.getAttributes().get("normalizedForm"));
        assertNotNull(tokyo.getAttributes().get("readingForm"));
        assertEquals("false", tokyo.getAttributes().get("outOfVocabulary"));
    }

    @Test
    public void returnsNoTokensForEmptyInput() {
        TextTokenizer tokenizer = new SudachiTokenizerProvider(dictionary).create("ja");
        assertTrue(tokenizer.tokenize("").isEmpty());
    }

    private static List<String> tokenTexts(List<TextToken> tokens) {
        return tokens.stream().map(TextToken::getText).collect(Collectors.toList());
    }
}
