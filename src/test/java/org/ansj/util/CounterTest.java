package org.ansj.util;

import org.ansj.domain.Result;
import org.ansj.splitWord.Analysis;
import org.ansj.splitWord.analysis.BaseAnalysis;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CounterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void countsTextFilesInNestedDirectories() throws Exception {
        Path root = temporaryFolder.getRoot().toPath();
        Path nested = Files.createDirectories(root.resolve("nested/level"));
        String rootText = "rootword";
        String nestedText = "nestedword";
        Files.write(root.resolve("root.txt"), rootText.getBytes(StandardCharsets.UTF_8));
        Files.write(nested.resolve("nested.txt"), nestedText.getBytes(StandardCharsets.UTF_8));
        Files.write(nested.resolve("ignored.bin"), "ignored".getBytes(StandardCharsets.UTF_8));

        Analysis analysis = new BaseAnalysis();
        Result result = Counter.count(root.toFile(), "UTF-8", analysis, 20);

        assertEquals(analysis.parseStr(rootText).size() + analysis.parseStr(nestedText).size(), totalCount(result));
    }

    private static int totalCount(Counter.Result result) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : result) {
            total += entry.getValue();
        }
        return total;
    }
}
