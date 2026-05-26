package jvcs.refs;

import jvcs.hash.Hash;
import jvcs.objects.Blob;
import jvcs.repository.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RefsTest {

    @TempDir
    Path tempDir;

    @Test
    void headRefDefaultsToMain() throws Exception {
        var repo = Repository.init(tempDir);
        var refs = new Refs(repo.jvcsPath());
        assertEquals("refs/heads/main", refs.readHeadRef());
    }

    @Test
    void resolveHeadReturnsNullOnEmptyRepo() throws Exception {
        var repo = Repository.init(tempDir);
        var refs = new Refs(repo.jvcsPath());
        assertNull(refs.resolveHead());
    }

    @Test
    void writeAndReadRef() throws Exception {
        var repo = Repository.init(tempDir);
        var refs = new Refs(repo.jvcsPath());
        var hash = new Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        refs.writeRef("feature", hash);
        assertEquals(hash, refs.readRef("feature"));
    }

    @Test
    void deleteRef() throws Exception {
        var repo = Repository.init(tempDir);
        var refs = new Refs(repo.jvcsPath());
        refs.writeRef("feature", new Hash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));
        refs.deleteRef("feature");
        assertNull(refs.readRef("feature"));
    }

    @Test
    void listHeadsAfterCommit() throws Exception {
        var repo = Repository.init(tempDir);
        var store = repo.objects();
        var blobHash = store.store(new Blob("data".getBytes(StandardCharsets.UTF_8)));
        var tree = new jvcs.objects.Tree(java.util.List.of(
            new jvcs.objects.TreeEntry(jvcs.objects.FileMode.REGULAR_FILE, "f.txt", blobHash)
        ));
        var treeHash = store.store(tree);
        var sig = new jvcs.objects.Signature("T", "t@t.com", java.time.Instant.ofEpochSecond(0));
        var commit = new jvcs.objects.Commit(treeHash, java.util.List.of(), sig, sig, "msg");
        var commitHash = store.store(commit);

        var refs = new Refs(repo.jvcsPath());
        refs.writeRef("main", commitHash);

        var heads = refs.listHeads();
        assertEquals(1, heads.size());
        assertEquals(commitHash, heads.get("main"));
    }

    @Test
    void setHeadRef() throws Exception {
        var repo = Repository.init(tempDir);
        var refs = new Refs(repo.jvcsPath());
        refs.setHeadRef("refs/heads/feature");
        assertEquals("refs/heads/feature", refs.readHeadRef());
    }

    @Test
    void resolveHeadThroughRef() throws Exception {
        var repo = Repository.init(tempDir);
        var hash = new Hash("cccccccccccccccccccccccccccccccccccccccc");
        var refs = new Refs(repo.jvcsPath());
        refs.writeRef("main", hash);
        assertEquals(hash, refs.resolveHead());
    }
}
