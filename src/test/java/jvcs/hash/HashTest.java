package jvcs.hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashTest {

    @Test
    void fromHexString() {
        var hex = "a9993e364706816aba3e25717850c26c9cd0d89d";
        var hash = new Hash(hex);
        assertEquals(hex, hash.value());
    }

    @Test
    void normalizesUppercase() {
        var upper = "A9993E364706816ABA3E25717850C26C9CD0D89D";
        var lower = "a9993e364706816aba3e25717850c26c9cd0d89d";
        assertEquals(new Hash(lower), new Hash(upper));
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new Hash("abc"));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> new Hash(null));
    }

    @Test
    void rejectsNonHexCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new Hash("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
    }

    @Test
    void roundTripToRawBytes() {
        var original = new Hash("a9993e364706816aba3e25717850c26c9cd0d89d");
        var raw = original.toRawBytes();
        var restored = Hash.fromRawBytes(raw);
        assertEquals(original, restored);
    }

    @Test
    void comparableOrdersByHex() {
        var a = new Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        var b = new Hash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    void fromRawBytesRequires20Bytes() {
        assertThrows(IllegalArgumentException.class, () -> Hash.fromRawBytes(new byte[19]));
        assertThrows(IllegalArgumentException.class, () -> Hash.fromRawBytes(new byte[21]));
    }

    @Test
    void toStringReturnsValue() {
        var hex = "a9993e364706816aba3e25717850c26c9cd0d89d";
        var hash = new Hash(hex);
        assertEquals(hex, hash.toString());
    }
}
