package io.linguamesh.lucene;

import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import io.linguamesh.api.TextToken;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Lucene10TokenizerProvider implements TextTokenizerProvider {
    private static final Set<String> LANGUAGE_TAGS = Collections.unmodifiableSet(
            new java.util.LinkedHashSet<String>(Arrays.asList(
                    "ar", "de", "en", "es", "fr", "hi", "it", "ru", "th", "zh", "ja", "ko", "*")));

    @Override
    public String getId() {
        return "lucene10";
    }

    @Override
    public Set<String> getLanguageTags() {
        return LANGUAGE_TAGS;
    }

    @Override
    public TextTokenizer create(String languageTag) {
        return new LuceneTokenizer(TextTokenizerRegistry.normalizeLanguageTag(languageTag));
    }

    private static final class LuceneTokenizer implements TextTokenizer {
        private final String languageTag;

        private LuceneTokenizer(String languageTag) {
            this.languageTag = languageTag;
        }

        @Override
        public String getProviderId() {
            return "lucene10";
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
            String language = Locale.forLanguageTag(languageTag).getLanguage();
            try (Analyzer analyzer = analyzerFor(language);
                 TokenStream stream = analyzer.tokenStream("text", text)) {
                CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
                OffsetAttribute offsets = stream.addAttribute(OffsetAttribute.class);
                List<TextToken> tokens = new ArrayList<TextToken>();
                stream.reset();
                while (stream.incrementToken()) {
                    int start = offsets.startOffset();
                    int end = offsets.endOffset();
                    if (start < 0 || end <= start || end > text.length()) {
                        throw new IllegalStateException("Lucene returned offsets outside the source text");
                    }
                    Map<String, String> attributes = new LinkedHashMap<String, String>();
                    attributes.put("analysisEngine", "Apache Lucene 10");
                    tokens.add(new TextToken(term.toString(), start, end, attributes));
                }
                stream.end();
                return Collections.unmodifiableList(tokens);
            } catch (IOException exception) {
                throw new IllegalStateException("Lucene could not tokenize the input", exception);
            }
        }

        private static Analyzer analyzerFor(String language) {
            switch (language) {
                case "ar":
                    return new ArabicAnalyzer();
                case "de":
                    return new GermanAnalyzer();
                case "en":
                    return new EnglishAnalyzer();
                case "es":
                    return new SpanishAnalyzer();
                case "fr":
                    return new FrenchAnalyzer();
                case "hi":
                    return new HindiAnalyzer();
                case "it":
                    return new ItalianAnalyzer();
                case "ru":
                    return new RussianAnalyzer();
                case "th":
                    return new ThaiAnalyzer();
                case "zh":
                case "ja":
                case "ko":
                    return new CJKAnalyzer();
                default:
                    return new StandardAnalyzer();
            }
        }
    }
}
