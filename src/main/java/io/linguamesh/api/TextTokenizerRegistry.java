package io.linguamesh.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

public final class TextTokenizerRegistry {
    private final Map<String, TextTokenizerProvider> providers;

    public TextTokenizerRegistry(Collection<? extends TextTokenizerProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        Map<String, TextTokenizerProvider> indexed = new LinkedHashMap<String, TextTokenizerProvider>();
        for (TextTokenizerProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            String id = requireId(provider.getId());
            if (indexed.put(id, provider) != null) {
                throw new IllegalArgumentException("Duplicate tokenizer provider id: " + id);
            }
        }
        this.providers = Collections.unmodifiableMap(indexed);
    }

    public static TextTokenizerRegistry discover() {
        List<TextTokenizerProvider> discovered = new ArrayList<TextTokenizerProvider>();
        for (TextTokenizerProvider provider : ServiceLoader.load(TextTokenizerProvider.class)) {
            discovered.add(provider);
        }
        return new TextTokenizerRegistry(discovered);
    }

    public List<TextTokenizerProvider> providersFor(String languageTag) {
        String normalized = normalizeLanguageTag(languageTag);
        String primaryLanguage = Locale.forLanguageTag(normalized).getLanguage().toLowerCase(Locale.ROOT);
        List<TextTokenizerProvider> matches = new ArrayList<TextTokenizerProvider>();
        for (TextTokenizerProvider provider : providers.values()) {
            for (String supportedTag : provider.getLanguageTags()) {
                if ("*".equals(supportedTag)
                        || normalized.equalsIgnoreCase(supportedTag)
                        || primaryLanguage.equalsIgnoreCase(Locale.forLanguageTag(supportedTag).getLanguage())) {
                    matches.add(provider);
                    break;
                }
            }
        }
        Collections.sort(matches, Comparator.comparing(TextTokenizerProvider::getId));
        return Collections.unmodifiableList(matches);
    }

    public TextTokenizer create(String providerId, String languageTag) {
        TextTokenizerProvider provider = providers.get(requireId(providerId));
        if (provider == null) {
            throw new IllegalArgumentException("Unknown tokenizer provider: " + providerId);
        }
        String normalized = normalizeLanguageTag(languageTag);
        boolean supported = false;
        String primaryLanguage = Locale.forLanguageTag(normalized).getLanguage();
        for (String supportedTag : provider.getLanguageTags()) {
            if ("*".equals(supportedTag)
                    || normalized.equalsIgnoreCase(supportedTag)
                    || primaryLanguage.equalsIgnoreCase(Locale.forLanguageTag(supportedTag).getLanguage())) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            throw new IllegalArgumentException(provider.getId() + " does not support " + normalized);
        }
        return Objects.requireNonNull(provider.create(normalized), "provider tokenizer");
    }

    public Set<String> getProviderIds() {
        return Collections.unmodifiableSet(new TreeSet<String>(providers.keySet()));
    }

    public static String normalizeLanguageTag(String languageTag) {
        Objects.requireNonNull(languageTag, "languageTag");
        String trimmed = languageTag.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            throw new IllegalArgumentException("A concrete BCP 47 language tag is required");
        }
        try {
            String normalized = new Locale.Builder().setLanguageTag(trimmed).build().toLanguageTag();
            if ("und".equals(normalized) && !"und".equalsIgnoreCase(trimmed)) {
                throw new IllegalArgumentException("Invalid BCP 47 language tag: " + languageTag);
            }
            return normalized;
        } catch (java.util.IllformedLocaleException exception) {
            throw new IllegalArgumentException("Invalid BCP 47 language tag: " + languageTag, exception);
        }
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "provider id");
        String normalized = id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Provider id must not be empty");
        }
        return normalized;
    }
}
