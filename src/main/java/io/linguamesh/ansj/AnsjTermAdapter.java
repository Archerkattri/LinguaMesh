package io.linguamesh.ansj;

import io.linguamesh.api.TextToken;
import org.ansj.domain.Term;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AnsjTermAdapter {
    private AnsjTermAdapter() {
    }

    static List<TextToken> toTextTokens(String source, Iterable<Term> terms) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(terms, "terms");
        List<TextToken> tokens = new ArrayList<TextToken>();
        for (Term term : terms) {
            String surface = term.getRealName();
            if (surface == null || surface.isEmpty()) {
                surface = term.getName();
            }
            int start = term.getOffe();
            int end = start + surface.length();
            if (start < 0 || end > source.length()
                    || !source.regionMatches(start, surface, 0, surface.length())) {
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
