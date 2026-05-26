package jvcs.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileModeTest {

    @Test
    void regularFile() {
        var mode = new FileMode("100644");
        assertFalse(mode.isTree());
    }

    @Test
    void executableFile() {
        var mode = new FileMode("100755");
        assertFalse(mode.isTree());
    }

    @Test
    void directory() {
        var mode = new FileMode("40000");
        assertTrue(mode.isTree());
    }

    @Test
    void constantsMatch() {
        assertEquals("100644", FileMode.REGULAR_FILE.value());
        assertEquals("100755", FileMode.EXECUTABLE_FILE.value());
        assertEquals("40000", FileMode.DIRECTORY.value());
    }

    @Test
    void rejectsInvalidMode() {
        assertThrows(IllegalArgumentException.class, () -> new FileMode("foo"));
        assertThrows(IllegalArgumentException.class, () -> new FileMode("777"));
        assertThrows(IllegalArgumentException.class, () -> new FileMode("100644 "));
        assertThrows(NullPointerException.class, () -> new FileMode(null));
    }
}
