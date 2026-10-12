package io.linguamesh.ansj;

import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import io.linguamesh.api.TextToken;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            List<TextToken> tokens = new ArrayList<TextToken>();
            for (Term term : ToAnalysis.parse(text)) {
                String surface = term.getRealName();
                if (surface == null || surface.isEmpty()) {
                    surface = term.getName();
                }
                int start = term.getOffe();
                int end = start + surface.length();
                if (start < 0 || end > text.length() || !text.regionMatches(start, surface, 0, surface.length())) {
                    throw new IllegalStateException("Ansj returned token offsets that do not match the source text");
                }
                Map<String, String> attributes = new LinkedHashMap<String, String>();
                String partOfSpeech = term.getNatureStr();
                if (partOfSpeech != null && !"null".equals(partOfSpeech)) {
                    attributes.put("partOfSpeech", partOfSpeech);
                }
                tokens.add(new TextToken(surface, start, end, attributes));
            }
            return Collections.unmodifiableList(tokens);
        }
    }
}
