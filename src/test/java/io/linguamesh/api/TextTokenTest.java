package io.linguamesh.api;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TextTokenTest {
    @Test
    public void snapshotsAndProtectsAttributes() {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("partOfSpeech", "n");
        TextToken token = new TextToken("词", 2, 3, attributes);
        attributes.put("partOfSpeech", "v");

        assertEquals("n", token.getAttributes().get("partOfSpeech"));
        try {
            token.getAttributes().put("partOfSpeech", "v");
            fail("Token attributes must be immutable");
        } catch (UnsupportedOperationException expected) {
            assertEquals("n", token.getAttributes().get("partOfSpeech"));
        }
    }

    @Test
    public void rejectsEmptyTokensAndInvalidOffsets() {
        assertThrows(IllegalArgumentException.class,
                () -> new TextToken("", 0, 0, Collections.<String, String>emptyMap()));
        assertThrows(IllegalArgumentException.class,
                () -> new TextToken("词", 2, 2, Collections.<String, String>emptyMap()));
        assertThrows(IllegalArgumentException.class,
                () -> new TextToken("词", -1, 0, Collections.<String, String>emptyMap()));
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            fail("Expected " + type.getName());
        } catch (Throwable exception) {
            if (!type.isInstance(exception)) {
                throw new AssertionError("Unexpected exception", exception);
            }
        }
    }
}
