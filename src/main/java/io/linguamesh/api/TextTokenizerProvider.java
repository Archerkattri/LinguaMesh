package io.linguamesh.api;

import java.util.Set;

public interface TextTokenizerProvider {
    String getId();

    Set<String> getLanguageTags();

    TextTokenizer create(String languageTag);
}
