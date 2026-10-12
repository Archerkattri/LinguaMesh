package io.linguamesh.api;

import java.util.List;

public interface TextTokenizer {
    String getProviderId();

    String getLanguageTag();

    List<TextToken> tokenize(String text);
}
