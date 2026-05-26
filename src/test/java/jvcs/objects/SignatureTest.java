package jvcs.objects;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SignatureTest {

    @Test
    void serializeFormat() {
        var sig = new Signature("John Doe", "john@example.com", Instant.ofEpochSecond(1748260000));
        assertEquals("John Doe <john@example.com> 1748260000 +0000", sig.serialize());
    }

    @Test
    void deserializeRoundTrip() {
        var original = new Signature("Jane Doe", "jane@test.com", Instant.ofEpochSecond(1000000000));
        var parsed = Signature.deserialize(original.serialize());
        assertEquals(original, parsed);
    }

    @Test
    void deserializeParsesTimestamp() {
        var sig = Signature.deserialize("Author Name <author@example.com> 1234567890 +0000");
        assertEquals(Instant.ofEpochSecond(1234567890), sig.timestamp());
    }

    @Test
    void deserializeHandlesMultipleSpacesInName() {
        var sig = Signature.deserialize("Very Long Name <email@e.com> 999999999 +0000");
        assertEquals("Very Long Name", sig.name());
    }

    @Test
    void rejectsMalformedSignature() {
        assertThrows(IllegalArgumentException.class,
            () -> Signature.deserialize("no angle brackets"));
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class,
            () -> new Signature("", "e@e.com", Instant.now()));
    }
}
