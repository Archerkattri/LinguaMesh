package io.linguamesh.icu4j;

import com.ibm.icu.text.BreakIterator;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import io.linguamesh.api.TextToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Icu4jTokenizerProvider implements TextTokenizerProvider {
    private static final Set<String> LANGUAGE_TAGS = Collections.singleton("*");

    @Override
    public String getId() {
        return "icu4j";
    }

    @Override
    public Set<String> getLanguageTags() {
        return LANGUAGE_TAGS;
    }

    @Override
    public TextTokenizer create(String languageTag) {
        return new Icu4jTokenizer(TextTokenizerRegistry.normalizeLanguageTag(languageTag));
    }

    private static final class Icu4jTokenizer implements TextTokenizer {
        private final String languageTag;

        private Icu4jTokenizer(String languageTag) {
            this.languageTag = languageTag;
        }

        @Override
        public String getProviderId() {
            return "icu4j";
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
            BreakIterator iterator = BreakIterator.getWordInstance(Locale.forLanguageTag(languageTag));
            iterator.setText(text);
            List<TextToken> tokens = new ArrayList<TextToken>();
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                if (containsWordCharacter(text, start, end)) {
                    Map<String, String> attributes = new LinkedHashMap<String, String>();
                    attributes.put("boundaryEngine", "icu4j-word-break");
                    tokens.add(new TextToken(text.substring(start, end), start, end, attributes));
                }
            }
            return Collections.unmodifiableList(tokens);
        }

        private static boolean containsWordCharacter(String text, int start, int end) {
            for (int offset = start; offset < end; ) {
                int codePoint = text.codePointAt(offset);
                int type = Character.getType(codePoint);
                if (Character.isLetterOrDigit(codePoint)
                        || type == Character.NON_SPACING_MARK
                        || type == Character.COMBINING_SPACING_MARK
                        || type == Character.ENCLOSING_MARK) {
                    return true;
                }
                offset += Character.charCount(codePoint);
            }
            return false;
        }
    }
}
