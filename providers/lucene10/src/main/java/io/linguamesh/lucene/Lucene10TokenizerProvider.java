package io.linguamesh.lucene;

import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerProvider;
import io.linguamesh.api.TextTokenizerRegistry;
import io.linguamesh.api.TextToken;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.bn.BengaliAnalyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.ckb.SoraniAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.et.EstonianAnalyzer;
import org.apache.lucene.analysis.eu.BasqueAnalyzer;
import org.apache.lucene.analysis.fa.PersianAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.ga.IrishAnalyzer;
import org.apache.lucene.analysis.gl.GalicianAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.hy.ArmenianAnalyzer;
import org.apache.lucene.analysis.id.IndonesianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.lt.LithuanianAnalyzer;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.analysis.ne.NepaliAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.sr.SerbianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.ta.TamilAnalyzer;
import org.apache.lucene.analysis.te.TeluguAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
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
                    "ar", "bg", "bn", "ca", "ckb", "cs", "da", "de", "el", "en", "es", "et", "eu",
                    "fa", "fi", "fr", "ga", "gl", "hi", "hu", "hy", "id", "it", "lt", "lv", "ne",
                    "nl", "no", "pt", "pt-BR", "ro", "ru", "sr", "sv", "ta", "te", "th", "tr",
                    "zh", "ja", "ko", "*")));

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
            try (Analyzer analyzer = analyzerFor(language, languageTag);
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

        private static Analyzer analyzerFor(String language, String languageTag) {
            switch (language) {
                case "ar":
                    return new ArabicAnalyzer();
                case "bg":
                    return new BulgarianAnalyzer();
                case "bn":
                    return new BengaliAnalyzer();
                case "ca":
                    return new CatalanAnalyzer();
                case "ckb":
                    return new SoraniAnalyzer();
                case "cs":
                    return new CzechAnalyzer();
                case "da":
                    return new DanishAnalyzer();
                case "de":
                    return new GermanAnalyzer();
                case "el":
                    return new GreekAnalyzer();
                case "en":
                    return new EnglishAnalyzer();
                case "es":
                    return new SpanishAnalyzer();
                case "et":
                    return new EstonianAnalyzer();
                case "eu":
                    return new BasqueAnalyzer();
                case "fa":
                    return new PersianAnalyzer();
                case "fi":
                    return new FinnishAnalyzer();
                case "fr":
                    return new FrenchAnalyzer();
                case "ga":
                    return new IrishAnalyzer();
                case "gl":
                    return new GalicianAnalyzer();
                case "hi":
                    return new HindiAnalyzer();
                case "hu":
                    return new HungarianAnalyzer();
                case "hy":
                    return new ArmenianAnalyzer();
                case "id":
                    return new IndonesianAnalyzer();
                case "it":
                    return new ItalianAnalyzer();
                case "lt":
                    return new LithuanianAnalyzer();
                case "lv":
                    return new LatvianAnalyzer();
                case "ne":
                    return new NepaliAnalyzer();
                case "nl":
                    return new DutchAnalyzer();
                case "no":
                    return new NorwegianAnalyzer();
                case "pt":
                    return "BR".equalsIgnoreCase(Locale.forLanguageTag(languageTag).getCountry())
                            ? new BrazilianAnalyzer() : new PortugueseAnalyzer();
                case "ro":
                    return new RomanianAnalyzer();
                case "ru":
                    return new RussianAnalyzer();
                case "sr":
                    return new SerbianAnalyzer();
                case "sv":
                    return new SwedishAnalyzer();
                case "ta":
                    return new TamilAnalyzer();
                case "te":
                    return new TeluguAnalyzer();
                case "th":
                    return new ThaiAnalyzer();
                case "tr":
                    return new TurkishAnalyzer();
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
