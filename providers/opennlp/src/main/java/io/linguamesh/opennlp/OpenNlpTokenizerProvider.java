package io.linguamesh.opennlp;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class OpenNlpTokenizerProvider implements TextTokenizerProvider {
    private final Map<String, TokenizerModel> models;
    private final Set<String> languageTags;

    public OpenNlpTokenizerProvider() {
        this(Collections.<String, TokenizerModel>emptyMap());
    }

    public OpenNlpTokenizerProvider(Map<String, TokenizerModel> models) {
        Objects.requireNonNull(models, "models");
        Map<String, TokenizerModel> indexed = new LinkedHashMap<String, TokenizerModel>();
        for (Map.Entry<String, TokenizerModel> entry : models.entrySet()) {
            String languageTag = TextTokenizerRegistry.normalizeLanguageTag(entry.getKey());
            if (indexed.put(languageTag, Objects.requireNonNull(entry.getValue(), "tokenizer model")) != null) {
                throw new IllegalArgumentException("Duplicate model language tag: " + languageTag);
            }
        }
        this.models = Collections.unmodifiableMap(indexed);
        this.languageTags = Collections.unmodifiableSet(new TreeSet<String>(indexed.keySet()));
    }

    @Override
    public String getId() {
        return "opennlp";
    }

    @Override
    public Set<String> getLanguageTags() {
        return languageTags;
    }

    @Override
    public TextTokenizer create(String languageTag) {
        String normalized = TextTokenizerRegistry.normalizeLanguageTag(languageTag);
        TokenizerModel model = models.get(normalized);
        if (model == null) {
            model = models.get(Locale.forLanguageTag(normalized).getLanguage());
        }
        if (model == null) {
            throw new IllegalArgumentException("No OpenNLP tokenizer model is configured for " + normalized);
        }
        return new OpenNlpTokenizer(normalized, model);
    }

    private static final class OpenNlpTokenizer implements TextTokenizer {
        private final String languageTag;
        private final TokenizerME tokenizer;

        private OpenNlpTokenizer(String languageTag, TokenizerModel model) {
            this.languageTag = languageTag;
            this.tokenizer = new TokenizerME(model);
        }

        @Override
        public String getProviderId() {
            return "opennlp";
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
            Span[] spans = tokenizer.tokenizePos(text);
            List<TextToken> tokens = new ArrayList<TextToken>(spans.length);
            for (Span span : spans) {
                int start = span.getStart();
                int end = span.getEnd();
                if (start < 0 || end <= start || end > text.length()) {
                    throw new IllegalStateException("OpenNLP returned offsets outside the source text");
                }
                Map<String, String> attributes = new LinkedHashMap<String, String>();
                attributes.put("analysisEngine", "Apache OpenNLP");
                tokens.add(new TextToken(text.substring(start, end), start, end, attributes));
            }
            return Collections.unmodifiableList(tokens);
        }
    }
}
