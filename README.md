# LinguaMesh

**Java text tokenization with selectable language engines.** LinguaMesh keeps the Ansj Chinese segmenter and adds a provider API so applications can choose an engine appropriate to their language, runtime, and use case.

[![Java CI](https://github.com/Archerkattri/LinguaMesh/actions/workflows/ci.yml/badge.svg)](https://github.com/Archerkattri/LinguaMesh/actions/workflows/ci.yml)
[![Apache-2.0 license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE.txt)

This repository imports the history and code of [Ansj Seg](https://github.com/NLPchina/ansj_seg), an Apache-2.0 Chinese text-analysis library. Existing `org.ansj` APIs remain available. New LinguaMesh APIs and providers are maintained separately; this project is independent and is not an official NLPchina release.

## Providers

| Provider | Coverage | Notes |
|---|---|---|
| `ansj` | Chinese | Existing dictionaries and analysis behavior; retains the `org.ansj` compatibility API. |
| `icu4j` | Unicode word boundaries for locale-tagged text | General segmentation fallback, not a substitute for language-specific morphology. ICU supplies dictionary word breaking for Chinese, Japanese, Thai, Lao, Khmer, and Burmese. |
| `lucene10` | Arabic, Armenian, Basque, Bengali, Brazilian Portuguese, Bulgarian, Catalan, Chinese/Japanese/Korean, Czech, Danish, Dutch, English, Estonian, Finnish, French, Galician, German, Greek, Hindi, Hungarian, Indonesian, Irish, Italian, Latvian, Lithuanian, Nepali, Norwegian, Persian, Portuguese, Romanian, Russian, Serbian, Sorani Kurdish, Spanish, Swedish, Tamil, Telugu, Thai, and Turkish; generic Unicode fallback for other tags | Uses Lucene language analyzers for the listed languages and `StandardAnalyzer` otherwise. Some analyzers normalize, remove stop words, or stem; offsets still refer to the input. Requires Java 21. |

Providers are optional Maven dependencies discovered with Java `ServiceLoader`. Tokens include UTF-16 offsets into the source and engine-specific attributes; token text may be normalized. Multiple engines can support the same language, so select the provider explicitly for predictable behavior.

## Install

The current coordinates are development snapshots and are not published to Maven Central:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-core</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Add the Lucene 10 provider for language-specific search analysis:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-provider-lucene10</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Add the ICU4J provider when Unicode boundary tokenization is needed:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-provider-icu4j</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Use

```java
import io.linguamesh.api.TextToken;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.api.TextTokenizerRegistry;

TextTokenizerRegistry registry = TextTokenizerRegistry.discover();
TextTokenizer tokenizer = registry.create("icu4j", "th");
for (TextToken token : tokenizer.tokenize("ภาษาไทย")) {
    System.out.println(token.getText() + " " + token.getStartOffset() + ":" + token.getEndOffset());
}
```

For direct access to the original Ansj API, use `ToAnalysis.parse(text)` and the `org.ansj` classes as before. Token offsets are zero-based, end-exclusive UTF-16 indices into the input string. A provider may return normalized token text, so use offsets to locate the original text rather than assuming the token equals its source substring.

## Build and test

Requires JDK 8 or newer and Maven 3.6.3 or newer.

```sh
mvn -B -ntp clean verify
mvn -B -ntp install
mvn -B -ntp -f providers/icu4j/pom.xml clean verify
mvn -B -ntp -f providers/lucene10/pom.xml clean verify
```

The core targets Java 8. ICU4J 78.3 requires Java 11, and the Lucene 10 provider requires Java 21.

The historical integrations under `plugin/` are retained from Ansj for reference and compatibility; the new provider API is the extension point for current integrations.

## Credit and license

LinguaMesh retains the Ansj Seg Apache License 2.0 and notices. The imported source history retains its recorded authorship; new provider code is maintained in this repository. Third-party engines and language models keep their own licenses and notices. See [LICENSE.txt](LICENSE.txt) and [NOTICE.txt](NOTICE.txt).
