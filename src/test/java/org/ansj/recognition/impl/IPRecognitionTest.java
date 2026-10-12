package org.ansj.recognition.impl;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class IPRecognitionTest {

    @Test
    public void recognizesValidIpv4Addresses() {
        Result result = ToAnalysis.parse("192.168.1.1 and 0.0.0.0").recognition(new IPRecognition());

        assertNature(result, "192.168.1.1");
        assertNature(result, "0.0.0.0");
    }

    @Test
    public void rejectsOutOfRangeAndAmbiguousOctets() {
        Result result = ToAnalysis.parse("256.1.1.1 and 192.168.01.1").recognition(new IPRecognition());

        assertNull(findTerm(result, "256.1.1.1"));
        assertNull(findTerm(result, "192.168.01.1"));
    }

    @Test
    public void rejectsValidLookingSuffixesOfLongerDottedSequences() {
        Result prefixed = ToAnalysis.parse("999.192.168.1.1").recognition(new IPRecognition());
        Result suffixed = ToAnalysis.parse("192.168.1.1.999").recognition(new IPRecognition());

        assertNull(findIp(prefixed, "192.168.1.1"));
        assertNull(findIp(suffixed, "192.168.1.1"));
    }

    private static void assertNature(Result result, String address) {
        Term term = findTerm(result, address);
        assertNotNull("Expected token: " + address, term);
        assertEquals("ip", term.getNatureStr());
    }

    private static Term findTerm(Result result, String name) {
        for (Term term : result) {
            if (name.equals(term.getName())) {
                return term;
            }
        }
        return null;
    }

    private static Term findIp(Result result, String name) {
        Term term = findTerm(result, name);
        return term != null && "ip".equals(term.getNatureStr()) ? term : null;
    }
}
