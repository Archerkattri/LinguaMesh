# LinguaMesh

**Java text tokenization with selectable language engines.** LinguaMesh keeps the Ansj Chinese segmenter and adds a provider API so applications can choose an engine appropriate to their language, runtime, and use case.

[![Java CI](https://github.com/Archerkattri/LinguaMesh/actions/workflows/ci.yml/badge.svg)](https://github.com/Archerkattri/LinguaMesh/actions/workflows/ci.yml)
[![Apache-2.0 license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE.txt)

This repository imports the history and code of [Ansj Seg](https://github.com/NLPchina/ansj_seg), an Apache-2.0 Chinese text-analysis library. Existing `org.ansj` APIs remain available. New LinguaMesh APIs and providers are maintained separately; this project is independent and is not an official NLPchina release.

## Providers

The planned Maven Central release contains the provider-neutral API and ICU4J, Lucene, OpenNLP, and Sudachi providers. The Ansj compatibility implementation remains in this source repository but is not packaged for Central while the inherited data assets' redistribution rights are unresolved.

| Provider | Coverage | Notes |
|---|---|---|
| `ansj` | Chinese | Source-tree compatibility provider; excluded from Central pending data provenance review. |
| `ansj-index` | Chinese retrieval analysis | Source-tree compatibility provider based on Ansj `IndexAnalysis`; excluded from Central. |
| `icu4j` | Unicode word boundaries for locale-tagged text | General segmentation fallback, not a substitute for language-specific morphology. ICU supplies dictionary word breaking for Chinese, Japanese, Thai, Lao, Khmer, and Burmese. |
| `lucene10` | Arabic, Armenian, Basque, Bengali, Brazilian Portuguese, Bulgarian, Catalan, Chinese, Czech, Danish, Dutch, English, Estonian, Finnish, French, Galician, German, Greek, Hindi, Hungarian, Indonesian, Irish, Italian, Japanese, Korean, Latvian, Lithuanian, Nepali, Norwegian, Persian, Portuguese, Romanian, Russian, Serbian, Sorani Kurdish, Spanish, Swedish, Tamil, Telugu, Thai, and Turkish; generic Unicode fallback for other tags | Uses Lucene language analyzers, Kuromoji for Japanese morphology, Nori for Korean morphology, and `CJKAnalyzer` for Chinese; `StandardAnalyzer` is the generic fallback. Some analyzers normalize, remove stop words, or stem; offsets still refer to the input. Requires Java 21. |
| `sudachi` | Japanese | Morphological analysis with configurable A/B/C segmentation, POS, reading, dictionary-form, normalized-form, and OOV attributes. Uses Sudachi 0.8.2 and requires a caller-provided V1 dictionary; no dictionary is bundled. User dictionaries must be rebuilt against the exact system dictionary used at runtime. SudachiDict includes UniDic and NEologd data, so follow the dictionary's separate notices when redistributing it. Requires Java 8. |
| `opennlp` | Languages for which the application supplies a compatible OpenNLP tokenizer model | The model is application-provided and not bundled. Apache lists 36 language-specific model sets across several NLP tasks; coverage is task-specific. Requires Java 17. |

Providers are optional Maven dependencies discovered with Java `ServiceLoader`. Tokens include UTF-16 offsets into the source and engine-specific attributes; token text may be normalized. Multiple engines can support the same language, so select the provider explicitly for predictable behavior.

## Install

The coordinates below are for the prepared `1.0.0` Central release. They become installable after publication completes. Add this provider-neutral API before choosing one or more engines:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-api</artifactId>
  <version>1.0.0</version>
</dependency>
```

Add the Lucene 10 provider for language-specific search analysis:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-provider-lucene10</artifactId>
  <version>1.0.0</version>
</dependency>
```

Add the OpenNLP provider when an application supplies tokenizer models:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-provider-opennlp</artifactId>
  <version>1.0.0</version>
</dependency>
```

Load each model under an explicit BCP 47 language tag. LinguaMesh does not download or redistribute model files:

```java
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.opennlp.OpenNlpTokenizerProvider;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

TokenizerModel model;
try (InputStream input = Files.newInputStream(Path.of("models/en-token.bin"))) {
    model = new TokenizerModel(input);
}
TextTokenizer tokenizer = new OpenNlpTokenizerProvider(Collections.singletonMap("en", model))
        .create("en-US");
```

Add the Sudachi provider for Japanese morphological analysis. It does not bundle or download a dictionary; load a Sudachi V1 dictionary compatible with 0.8.2 and keep the `Dictionary` instance alive while using its tokenizers. User dictionaries must be rebuilt against the exact system dictionary used at runtime. The `ServiceLoader` provider uses Sudachi's `Config.defaultConfig()`; the explicit constructor below lets applications select a split mode:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-provider-sudachi</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
import com.worksap.nlp.sudachi.Config;
import com.worksap.nlp.sudachi.Dictionary;
import com.worksap.nlp.sudachi.Tokenizer;
import io.linguamesh.api.TextTokenizer;
import io.linguamesh.sudachi.SudachiTokenizerProvider;

import java.io.IOException;
import java.nio.file.Paths;

Config config = Config.defaultConfig();
config.systemDictionary(Paths.get("models/system_core.dic"));
Dictionary dictionary = Dictionary.load(config);
SudachiTokenizerProvider provider = new SudachiTokenizerProvider(dictionary, Tokenizer.SplitMode.B);
TextTokenizer tokenizer = provider.create("ja-JP");
```

Add the ICU4J provider when Unicode boundary tokenization is needed:

```xml
<dependency>
  <groupId>io.github.archerkattri</groupId>
  <artifactId>linguamesh-provider-icu4j</artifactId>
  <version>1.0.0</version>
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

The Central API/provider artifacts exclude the imported Ansj lexicons, matrices, and CRF model. For Chinese tokenization from Maven Central, use ICU4J or Lucene; the legacy `org.ansj` implementation remains in the source repository but is not part of these release artifacts.

## Build and integration checks

Requires JDK 8 or newer and Maven 3.6.3 or newer.

```sh
mvn -B -ntp clean verify
mvn -B -ntp -f api/pom.xml clean verify
mvn -B -ntp -f api/pom.xml -Dgpg.skip=true install
mvn -B -ntp -f providers/icu4j/pom.xml clean verify
mvn -B -ntp -f providers/lucene10/pom.xml clean verify
mvn -B -ntp -f providers/opennlp/pom.xml clean verify
mvn -B -ntp -f providers/sudachi/pom.xml clean verify
```

The legacy unit-test suites have been removed. Provider and packaging integration checks remain. Sudachi checks require `SUDACHI_DICTIONARY_PATH`; CI downloads a checksum-pinned dictionary fixture that is not included in the artifact. The core and Sudachi provider target Java 8. ICU4J 78.3 requires Java 11, OpenNLP 2.5.12 requires Java 17, and Lucene 10 requires Java 21.

The historical integrations under `plugin/` are retained from Ansj for reference and compatibility; the new provider API is the extension point for current integrations.

## Maven Central release

The `central-publishing` profile signs and uploads the API/provider artifacts to the Central Portal for validation; the final publish action stays manual. The proposed release set is `linguamesh-api` plus the ICU4J, Lucene 10, OpenNLP, and Sudachi providers. The Ansj compatibility implementation is excluded until its bundled-data provenance is resolved. Publication requires a verified `io.github.archerkattri` namespace, a GPG signing key, and a `central` user-token entry in the developer's Maven `settings.xml`. Never commit those credentials.

```sh
mvn -f api/pom.xml -Pcentral-publishing clean deploy
mvn -f providers/icu4j/pom.xml -Pcentral-publishing clean deploy
mvn -f providers/lucene10/pom.xml -Pcentral-publishing clean deploy
mvn -f providers/opennlp/pom.xml -Pcentral-publishing clean deploy
mvn -f providers/sudachi/pom.xml -Pcentral-publishing clean deploy
```

## Credit and license

LinguaMesh retains the Ansj Seg Apache License 2.0 and notices. The imported source history retains its recorded authorship; new provider code is maintained in this repository. Third-party engines and language models keep their own licenses and notices. See [LICENSE.txt](LICENSE.txt) and [NOTICE.txt](NOTICE.txt).

The upstream Ansj repository does not identify the source or redistribution terms for its bundled dictionaries and CRF model. Those resources are kept out of the Central artifacts; their rights review remains open for any future release of the Ansj-compatible provider.
