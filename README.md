# Ansj Seg

Ansj Seg is a Java library for Chinese word segmentation. It includes several analysis modes, term and entity recognition, custom dictionaries, keyword extraction, and text summarization.

[![Java CI](https://github.com/Archerkattri/ansj_seg/actions/workflows/ci.yml/badge.svg)](https://github.com/Archerkattri/ansj_seg/actions/workflows/ci.yml)
[![Apache-2.0 license](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE.txt)

## About this repository

This repository is an English-maintained fork of [NLPchina/ansj_seg](https://github.com/NLPchina/ansj_seg). It retains the upstream Java package names and Maven coordinates, but this fork has not published a Maven release. Maven consumers should continue to use the upstream artifact until a release from this fork is available.

The project keeps its Apache License 2.0 notice and upstream attribution. It is an independent fork and is not an official NLPchina release.

## Requirements

- Java 8 or newer for the core library.
- Java 11 or newer for the Lucene 9 adapter in `plugin/ansj_lucene9_plugin`.
- Maven 3.6.3 or newer.

## Build and test

Run the core test suite and create the library, sources, and Javadoc artifacts:

```sh
mvn -B -ntp -Dgpg.skip=true clean verify
```

The Lucene 9 adapter is built separately after installing the core artifact locally:

```sh
mvn -B -ntp -Dgpg.skip=true install
mvn -B -ntp -Dgpg.skip=true -f plugin/ansj_lucene9_plugin/pom.xml clean verify
```

Other adapters under `plugin/` target older Lucene or Solr releases. They are retained for compatibility and are not covered by the current CI build.

## Basic usage

```java
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

String text = "欢迎使用中文分词";
for (Term term : ToAnalysis.parse(text)) {
    System.out.println(term.getName() + "\t" + term.getNatureStr());
}
```

`ToAnalysis` is the general-purpose entry point. The library also provides `BaseAnalysis`, `IndexAnalysis`, `DicAnalysis`, and `NlpAnalysis`; choose an analyzer based on the segmentation behavior your application needs.

## Dictionaries and models

The default dictionaries referenced by `ansj_library.properties` are included under `library/`. If you configure custom dictionaries or optional model files, make sure they are available at the paths you specify before parsing. Missing resources can cause initialization errors, especially in packaged or Android applications.

For a custom word dictionary, each line contains a word, part of speech, and frequency separated by tab characters:

```text
自然语言处理\tn\t1000
```

See the [upstream dictionary guide](https://github.com/NLPchina/ansj_seg/wiki/%E7%94%A8%E6%88%B7%E8%87%AA%E5%AE%9A%E4%B9%89%E8%AF%8D%E5%85%B8) and the [upstream documentation](https://github.com/NLPchina/ansj_seg/wiki) for dictionary and model formats. Check the resource's own license and provenance before distributing it with an application.

## License

Apache License 2.0. See [LICENSE.txt](LICENSE.txt) and [NOTICE.txt](NOTICE.txt).
