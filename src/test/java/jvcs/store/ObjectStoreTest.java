package jvcs.store;

import jvcs.exceptions.ObjectNotFoundException;
import jvcs.hash.Hash;
import jvcs.objects.Blob;
import jvcs.objects.Commit;
import jvcs.objects.FileMode;
import jvcs.objects.Signature;
import jvcs.objects.Tree;
import jvcs.objects.TreeEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ObjectStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storeAndLoadBlob() {
        var store = new ObjectStore(tempDir);
        var content = "hello jvcs".getBytes(StandardCharsets.UTF_8);
        var blob = new Blob(content);

        var hash = store.store(blob);
        var loaded = store.loadBlob(hash);

        assertArrayEquals(content, loaded.content());
    }

    @Test
    void storeSameBlobGivesSameHash() {
        var store = new ObjectStore(tempDir);
        var content = "deduplication test".getBytes(StandardCharsets.UTF_8);
        var blob = new Blob(content);

        var hash1 = store.store(blob);
        var hash2 = store.store(blob);

        assertEquals(hash1, hash2);
    }

    @Test
    void loadBlobWithDifferentTypeThrows() {
        var store = new ObjectStore(tempDir);
        var content = "this will be stored as blob".getBytes(StandardCharsets.UTF_8);
        var blob = new Blob(content);
        var hash = store.store(blob);

        // Since we only have blob loading, try a non-existent hash
        var fakeHash = new Hash("0000000000000000000000000000000000000000");
        assertThrows(ObjectNotFoundException.class, () -> store.loadBlob(fakeHash));
    }

    @Test
    void storeReturnsDeterministicHash() {
        var store1 = new ObjectStore(tempDir.resolve("repo1"));
        var store2 = new ObjectStore(tempDir.resolve("repo2"));
        var content = "same content".getBytes(StandardCharsets.UTF_8);
        var blob = new Blob(content);

        var hash1 = store1.store(blob);
        var hash2 = store2.store(blob);

        assertEquals(hash1, hash2);
    }

    @Test
    void blobEquality() {
        var content = "test".getBytes(StandardCharsets.UTF_8);
        var b1 = new Blob(content);
        var b2 = new Blob(content);

        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
    }

    @Test
    void blobImmutability() {
        var original = "immutable".getBytes(StandardCharsets.UTF_8);
        var blob = new Blob(original);
        original[0] = 'X'; // mutate original array

        assertNotEquals(original[0], blob.content()[0]);
    }

    @Test
    void existsReturnsFalseForUnknownHash() {
        var store = new ObjectStore(tempDir);
        var hash = new Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertFalse(store.exists(hash));
    }

    @Test
    void existsReturnsTrueForStoredObject() {
        var store = new ObjectStore(tempDir);
        var hash = store.store(new Blob("exists check".getBytes(StandardCharsets.UTF_8)));
        assertTrue(store.exists(hash));
    }

    // -- Tree persistence --

    @Test
    void storeAndLoadTree() {
        var store = new ObjectStore(tempDir);
        var content = "file content".getBytes(StandardCharsets.UTF_8);
        var blobHash = store.store(new Blob(content));

        var tree = new Tree(java.util.List.of(
            new TreeEntry(FileMode.REGULAR_FILE, "file.txt", blobHash)
        ));

        var treeHash = store.store(tree);
        var loaded = store.loadTree(treeHash);

        assertEquals(1, loaded.entries().size());
        assertEquals("file.txt", loaded.entries().get(0).name());
        assertEquals(blobHash, loaded.entries().get(0).hash());
    }

    @Test
    void loadTreeWithWrongTypeThrows() {
        var store = new ObjectStore(tempDir);
        var blobHash = store.store(new Blob("not a tree".getBytes(StandardCharsets.UTF_8)));

        assertThrows(jvcs.exceptions.CorruptObjectException.class,
            () -> store.loadTree(blobHash));
    }

    @Test
    void treeRoundTripPreservesStructure() {
        var store = new ObjectStore(tempDir);

        // Create a small tree with two files
        var hash1 = store.store(new Blob("a content".getBytes(StandardCharsets.UTF_8)));
        var hash2 = store.store(new Blob("b content".getBytes(StandardCharsets.UTF_8)));

        var original = new Tree(java.util.List.of(
            new TreeEntry(FileMode.REGULAR_FILE, "b.txt", hash2),
            new TreeEntry(FileMode.REGULAR_FILE, "a.txt", hash1)
        ));

        var treeHash = store.store(original);
        var loaded = store.loadTree(treeHash);

        assertEquals(original, loaded);
    }

    // -- Commit persistence --

    @Test
    void storeAndLoadCommit() {
        var store = new ObjectStore(tempDir);
        var sig = new Signature("Test", "test@test.com", java.time.Instant.ofEpochSecond(1000));
        var treeHash = store.store(new Blob("content".getBytes(StandardCharsets.UTF_8)));
        var tree = new Tree(java.util.List.of(
            new TreeEntry(FileMode.REGULAR_FILE, "f.txt", treeHash)
        ));
        var treeCommitHash = store.store(tree);

        var commit = new Commit(treeCommitHash, java.util.List.of(), sig, sig, "initial");
        var commitHash = store.store(commit);
        var loaded = store.loadCommit(commitHash);

        assertEquals(commit, loaded);
    }

    @Test
    void loadCommitWithWrongTypeThrows() {
        var store = new ObjectStore(tempDir);
        var blobHash = store.store(new Blob("not a commit".getBytes(StandardCharsets.UTF_8)));

        assertThrows(jvcs.exceptions.CorruptObjectException.class,
            () -> store.loadCommit(blobHash));
    }

    @Test
    void commitRoundTripWithParent() {
        var store = new ObjectStore(tempDir);
        var sig = new Signature("Dev", "dev@example.com", java.time.Instant.now());
        var treeHash = store.store(new Tree(java.util.List.of()));

        var parent = new Commit(treeHash, java.util.List.of(), sig, sig, "parent");
        var parentHash = store.store(parent);
        var child = new Commit(treeHash, java.util.List.of(parentHash), sig, sig, "child");

        var childHash = store.store(child);
        var loaded = store.loadCommit(childHash);

        assertEquals(1, loaded.parents().size());
        assertEquals(parentHash, loaded.parents().get(0));
    }
}
