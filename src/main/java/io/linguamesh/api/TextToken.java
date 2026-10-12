package io.linguamesh.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TextToken {
    private final String text;
    private final int startOffset;
    private final int endOffset;
    private final Map<String, String> attributes;

    public TextToken(String text, int startOffset, int endOffset) {
        this(text, startOffset, endOffset, Collections.<String, String>emptyMap());
    }

    public TextToken(String text, int startOffset, int endOffset, Map<String, String> attributes) {
        this.text = Objects.requireNonNull(text, "text");
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Token text must not be empty");
        }
        if (startOffset < 0 || endOffset <= startOffset) {
            throw new IllegalArgumentException("Token offsets must be a non-empty range starting at zero or later");
        }
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        Map<String, String> copy = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> attribute : Objects.requireNonNull(attributes, "attributes").entrySet()) {
            copy.put(Objects.requireNonNull(attribute.getKey(), "attribute key"),
                    Objects.requireNonNull(attribute.getValue(), "attribute value"));
        }
        this.attributes = Collections.unmodifiableMap(copy);
    }

    public String getText() {
        return text;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
