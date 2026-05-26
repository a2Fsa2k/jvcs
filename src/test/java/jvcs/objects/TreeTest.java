package jvcs.objects;

import jvcs.hash.Hash;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeTest {

    static final Hash HASH_A = new Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    static final Hash HASH_B = new Hash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    static final Hash HASH_C = new Hash("cccccccccccccccccccccccccccccccccccccccc");

    @Test
    void treeSortsEntriesByName() {
        var entryB = new TreeEntry(FileMode.REGULAR_FILE, "b", HASH_B);
        var entryA = new TreeEntry(FileMode.REGULAR_FILE, "a", HASH_A);

        var tree = new Tree(List.of(entryB, entryA));
        assertEquals("a", tree.entries().get(0).name());
        assertEquals("b", tree.entries().get(1).name());
    }

    @Test
    void treeRejectsDuplicates() {
        var entry1 = new TreeEntry(FileMode.REGULAR_FILE, "dup", HASH_A);
        var entry2 = new TreeEntry(FileMode.REGULAR_FILE, "dup", HASH_B);

        assertThrows(IllegalArgumentException.class, () -> new Tree(List.of(entry1, entry2)));
    }

    @Test
    void treeRejectsNullEntries() {
        assertThrows(NullPointerException.class, () -> new Tree(null));
    }

    @Test
    void emptyTree() {
        var tree = new Tree(List.of());
        assertTrue(tree.entries().isEmpty());
        assertArrayEquals(new byte[0], tree.serialize());
    }

    @Test
    void serializeDeserializeRoundTrip() {
        var entries = List.of(
            new TreeEntry(FileMode.REGULAR_FILE, "readme.md", HASH_A),
            new TreeEntry(FileMode.DIRECTORY, "src", HASH_B),
            new TreeEntry(FileMode.REGULAR_FILE, "test.txt", HASH_C)
        );

        var original = new Tree(entries);
        var deserialized = Tree.deserialize(original.serialize());

        assertEquals(original, deserialized);
    }

    @Test
    void deserializeFromKnownBytes() {
        // Build a tree with one entry manually
        var entry = new TreeEntry(FileMode.REGULAR_FILE, "hello.txt", HASH_A);
        var tree = new Tree(List.of(entry));
        var serialized = tree.serialize();

        var parsed = Tree.deserialize(serialized);
        assertEquals(1, parsed.entries().size());
        var parsedEntry = parsed.entries().get(0);
        assertEquals("100644", parsedEntry.mode().value());
        assertEquals("hello.txt", parsedEntry.name());
        assertEquals(HASH_A, parsedEntry.hash());
    }

    @Test
    void treeEntriesAreImmutable() {
        var mutable = new java.util.ArrayList<TreeEntry>();
        mutable.add(new TreeEntry(FileMode.REGULAR_FILE, "a", HASH_A));
        var tree = new Tree(mutable);
        mutable.add(new TreeEntry(FileMode.REGULAR_FILE, "b", HASH_B));

        assertEquals(1, tree.entries().size());
    }

    @Test
    void deserializeRejectsInvalidData() {
        assertThrows(IllegalArgumentException.class,
            () -> Tree.deserialize(new byte[] { (byte) 0xff }));
    }
}
