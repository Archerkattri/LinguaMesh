package io.linguamesh.sudachi;

import com.worksap.nlp.sudachi.Config;
import com.worksap.nlp.sudachi.Dictionary;
import com.worksap.nlp.sudachi.DictionaryFactory;
import com.worksap.nlp.sudachi.Morpheme;
import com.worksap.nlp.sudachi.MorphemeList;
import com.worksap.nlp.sudachi.Tokenizer;
import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Creates Japanese tokenizers backed by a caller-managed Sudachi dictionary. */
public final class SudachiTokenizerProvider implements TextTokenizerProvider {
    private static final Set<String> LANGUAGE_TAGS = Collections.unmodifiableSet(
            new TreeSet<String>(Collections.singleton("ja")));

    private volatile Dictionary dictionary;
    private final Tokenizer.SplitMode splitMode;

    public SudachiTokenizerProvider() {
        this.dictionary = null;
        this.splitMode = Tokenizer.SplitMode.C;
    }

    public SudachiTokenizerProvider(Dictionary dictionary) {
        this(dictionary, Tokenizer.SplitMode.C);
    }

    public SudachiTokenizerProvider(Dictionary dictionary, Tokenizer.SplitMode splitMode) {
        this.dictionary = Objects.requireNonNull(dictionary, "dictionary");
        this.splitMode = Objects.requireNonNull(splitMode, "splitMode");
    }

    @Override
    public String getId() {
        return "sudachi";
    }

    @Override
    public Set<String> getLanguageTags() {
        return LANGUAGE_TAGS;
    }

    @Override
    public TextTokenizer create(String languageTag) {
        String normalized = TextTokenizerRegistry.normalizeLanguageTag(languageTag);
        if (!"ja".equalsIgnoreCase(java.util.Locale.forLanguageTag(normalized).getLanguage())) {
            throw new IllegalArgumentException("sudachi does not support " + normalized);
        }
        return new SudachiTokenizer(normalized, dictionary().create(), splitMode);
    }

    private Dictionary dictionary() {
        Dictionary current = dictionary;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = dictionary;
            if (current == null) {
                try {
                    current = new DictionaryFactory().create(Config.defaultConfig());
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Configure a Sudachi dictionary or pass one to SudachiTokenizerProvider", exception);
                }
                dictionary = current;
            }
        }
        return current;
    }

    private static final class SudachiTokenizer implements TextTokenizer {
        private final String languageTag;
        private final Tokenizer tokenizer;
        private final Tokenizer.SplitMode splitMode;

        private SudachiTokenizer(String languageTag, Tokenizer tokenizer, Tokenizer.SplitMode splitMode) {
            this.languageTag = languageTag;
            this.tokenizer = tokenizer;
            this.splitMode = splitMode;
        }

        @Override
        public String getProviderId() {
            return "sudachi";
        }

        @Override
        public String getLanguageTag() {
            return languageTag;
        }

        @Override
        public synchronized List<TextToken> tokenize(String text) {
            Objects.requireNonNull(text, "text");
            if (text.isEmpty()) {
                return Collections.emptyList();
            }
            MorphemeList morphemes = tokenizer.tokenize(splitMode, text);
            List<TextToken> tokens = new ArrayList<TextToken>(morphemes.size());
            for (int i = 0; i < morphemes.size(); i++) {
                Morpheme morpheme = morphemes.get(i);
                int start = morpheme.begin();
                int end = morpheme.end();
                if (start < 0 || end <= start || end > text.length()) {
                    throw new IllegalStateException("Sudachi returned offsets outside the source text");
                }
                String surface = morpheme.surface();
                if (surface.isEmpty()) {
                    continue;
                }
                Map<String, String> attributes = new LinkedHashMap<String, String>();
                attributes.put("analysisEngine", "Sudachi");
                attributes.put("partOfSpeech", String.join(",", morpheme.partOfSpeech()));
                attributes.put("dictionaryForm", morpheme.dictionaryForm());
                attributes.put("normalizedForm", morpheme.normalizedForm());
                attributes.put("readingForm", morpheme.readingForm());
                attributes.put("outOfVocabulary", Boolean.toString(morpheme.isOOV()));
                tokens.add(new TextToken(surface, start, end, attributes));
            }
            return Collections.unmodifiableList(tokens);
        }
    }
}
