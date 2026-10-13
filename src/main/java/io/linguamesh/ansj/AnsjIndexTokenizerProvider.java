package io.linguamesh.ansj;

import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.IndexAnalysis;
import org.nlpcn.commons.lang.tire.domain.Forest;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Provides Ansj's overlapping, retrieval-oriented Chinese index terms. */
public final class AnsjIndexTokenizerProvider implements TextTokenizerProvider {
    private static final Set<String> LANGUAGE_TAGS = Collections.singleton("zh");

    private final Forest[] forests;

    public AnsjIndexTokenizerProvider() {
        this.forests = null;
    }

    public AnsjIndexTokenizerProvider(Forest... forests) {
        Objects.requireNonNull(forests, "forests");
        if (forests.length == 0) {
            throw new IllegalArgumentException("At least one dictionary forest is required");
        }
        this.forests = forests.clone();
        for (Forest forest : this.forests) {
            Objects.requireNonNull(forest, "forest");
        }
    }

    @Override
    public String getId() {
        return "ansj-index";
    }

    @Override
    public Set<String> getLanguageTags() {
        return LANGUAGE_TAGS;
    }

    @Override
    public TextTokenizer create(String languageTag) {
        String normalized = TextTokenizerRegistry.normalizeLanguageTag(languageTag);
        if (!"zh".equalsIgnoreCase(Locale.forLanguageTag(normalized).getLanguage())) {
            throw new IllegalArgumentException("ansj-index does not support " + normalized);
        }
        return new AnsjIndexTokenizer(normalized, forests);
    }

    private static final class AnsjIndexTokenizer implements TextTokenizer {
        private final String languageTag;
        private final Forest[] forests;

        private AnsjIndexTokenizer(String languageTag, Forest[] forests) {
            this.languageTag = languageTag;
            this.forests = forests;
        }

        @Override
        public String getProviderId() {
            return "ansj-index";
        }

        @Override
        public String getLanguageTag() {
            return languageTag;
        }

        @Override
        public List<TextToken> tokenize(String text) {
            Objects.requireNonNull(text, "text");
            if (text.isEmpty()) {
                return Collections.emptyList();
            }
            Result result = forests == null ? IndexAnalysis.parse(text) : IndexAnalysis.parse(text, forests);
            List<Term> terms = result.getTerms();
            return AnsjTermAdapter.toTextTokens(text, terms);
        }
    }
}
