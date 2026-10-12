package org.ansj.recognition.impl;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ThousandsSeparatorRecognitionTest {

    @Test
    public void recognizesGroupedIntegersAndDecimals() {
        Result result = ToAnalysis.parse("访问量为10,234,567.89元").recognition(new ThousandsSeparatorRecognition());

        assertNature(result, "10,234,567.89", "thousands_separator");
    }

    @Test
    public void supportsAConfiguredSeparator() {
        Result result = ToAnalysis.parse("10，234，567").recognition(new ThousandsSeparatorRecognition("，"));

        assertNature(result, "10，234，567", "thousands_separator");
    }

    @Test
    public void leavesMalformedGroupingUnchanged() {
        Result result = ToAnalysis.parse("10,23,567").recognition(new ThousandsSeparatorRecognition());

        assertNull(findTerm(result, "10,23,567"));
        for (Term term : result) {
            if ("thousands_separator".equals(term.getNatureStr())) {
                throw new AssertionError("Malformed grouping was classified as a number.");
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnInvalidSeparator() {
        new ThousandsSeparatorRecognition("12");
    }

    private static void assertNature(Result result, String name, String nature) {
        Term term = findTerm(result, name);
        assertNotNull("Expected token: " + name, term);
        assertEquals(nature, term.getNatureStr());
    }

    private static Term findTerm(Result result, String name) {
        for (Term term : result) {
            if (name.equals(term.getName())) {
                return term;
            }
        }
        return null;
    }
}
