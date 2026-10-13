package io.linguamesh.ansj;

import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import io.linguamesh.api.TextToken;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AnsjTokenizerProvider implements TextTokenizerProvider {
    private static final Set<String> LANGUAGE_TAGS = Collections.singleton("zh");

    @Override
    public String getId() {
        return "ansj";
    }

    @Override
    public Set<String> getLanguageTags() {
        return LANGUAGE_TAGS;
    }

    @Override
    public TextTokenizer create(String languageTag) {
        return new AnsjTokenizer(TextTokenizerRegistry.normalizeLanguageTag(languageTag));
    }

    private static final class AnsjTokenizer implements TextTokenizer {
        private final String languageTag;

        private AnsjTokenizer(String languageTag) {
            this.languageTag = languageTag;
        }

        @Override
        public String getProviderId() {
            return "ansj";
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
            return AnsjTermAdapter.toTextTokens(text, ToAnalysis.parse(text));
        }
    }
}
