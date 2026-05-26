package jvcs.index;

import jvcs.hash.Hash;
import jvcs.objects.Blob;
import jvcs.objects.FileMode;
import jvcs.store.ObjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IndexTest {

    @TempDir
    Path tempDir;

    final Hash HASH_A = new Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

    @Test
    void addAndGetEntry() {
        var index = new Index(tempDir.resolve("index"));
        var entry = new IndexEntry(FileMode.REGULAR_FILE, "test.txt", HASH_A);
        index.add(entry);
        assertEquals(entry, index.get("test.txt"));
    }

    @Test
    void removeEntry() {
        var index = new Index(tempDir.resolve("index"));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "f.txt", HASH_A));
        index.remove("f.txt");
        assertNull(index.get("f.txt"));
    }

    @Test
    void isEmptyOnCreation() {
        var index = new Index(tempDir.resolve("index"));
        assertTrue(index.isEmpty());
    }

    @Test
    void saveAndLoad() {
        var indexFile = tempDir.resolve("index");
        var index = new Index(indexFile);
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "a.txt", HASH_A));
        index.save();

        var loaded = Index.load(indexFile);
        assertEquals(1, loaded.size());
        assertNotNull(loaded.get("a.txt"));
    }

    @Test
    void saveAndLoadMultipleEntries() {
        var indexFile = tempDir.resolve("index");
        var index = new Index(indexFile);
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "b.txt", HASH_A));
        index.add(new IndexEntry(FileMode.DIRECTORY, "src", new Hash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")));
        index.save();

        var loaded = Index.load(indexFile);
        assertEquals(2, loaded.size());
    }

    @Test
    void loadNonExistentReturnsEmpty() {
        var index = Index.load(tempDir.resolve("nonexistent"));
        assertTrue(index.isEmpty());
    }

    @Test
    void addReplacesExistingEntry() {
        var index = new Index(tempDir.resolve("index"));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "f.txt", HASH_A));
        var newHash = new Hash("cccccccccccccccccccccccccccccccccccccccc");
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "f.txt", newHash));
        assertEquals(newHash, index.get("f.txt").hash());
    }

    @Test
    void buildTreeFromFlatEntries(@TempDir Path storeDir) {
        var store = new ObjectStore(storeDir);
        var index = new Index(tempDir.resolve("index"));

        var blobHash = store.store(new Blob("hello".getBytes(StandardCharsets.UTF_8)));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "readme.md", blobHash));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "main.java", blobHash));

        var treeHash = index.buildTree(store);
        var tree = store.loadTree(treeHash);
        assertEquals(2, tree.entries().size());
    }

    @Test
    void buildTreeWithDirectories(@TempDir Path storeDir) {
        var store = new ObjectStore(storeDir);
        var index = new Index(tempDir.resolve("index"));

        var blobHash = store.store(new Blob("content".getBytes(StandardCharsets.UTF_8)));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "src/main/Foo.java", blobHash));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "src/main/Bar.java", blobHash));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "README.md", blobHash));

        var treeHash = index.buildTree(store);
        var root = store.loadTree(treeHash);
        assertEquals(2, root.entries().size());
    }

    @Test
    void indexEntrySerialization() {
        var entry = new IndexEntry(FileMode.EXECUTABLE_FILE, "script.sh", HASH_A);
        var serialized = entry.serialize();
        var parsed = IndexEntry.deserialize(serialized);
        assertEquals(entry, parsed);
    }

    @Test
    void entriesReturnsImmutableCopy() {
        var index = new Index(tempDir.resolve("index"));
        index.add(new IndexEntry(FileMode.REGULAR_FILE, "f.txt", HASH_A));
        assertThrows(UnsupportedOperationException.class, () -> index.entries().clear());
        assertEquals(1, index.size());
    }
}
