package jvcs.objects;

import jvcs.hash.Hash;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommitTest {

    static final Hash TREE_HASH = new Hash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    static final Hash PARENT_HASH = new Hash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    static final Signature SIG = new Signature("Test User", "test@example.com", Instant.ofEpochSecond(1000000));

    @Test
    void serializeDeserializeRootCommit() {
        var commit = new Commit(TREE_HASH, List.of(), SIG, SIG, "initial commit");
        var parsed = Commit.deserialize(commit.serialize());
        assertEquals(commit, parsed);
    }

    @Test
    void serializeDeserializeWithParent() {
        var commit = new Commit(TREE_HASH, List.of(PARENT_HASH), SIG, SIG, "second commit");
        var parsed = Commit.deserialize(commit.serialize());
        assertEquals(commit, parsed);
        assertEquals(1, parsed.parents().size());
        assertEquals(PARENT_HASH, parsed.parents().get(0));
    }

    @Test
    void serializeDeserializeWithMultipleParents() {
        var p1 = new Hash("1111111111111111111111111111111111111111");
        var p2 = new Hash("2222222222222222222222222222222222222222");
        var commit = new Commit(TREE_HASH, List.of(p1, p2), SIG, SIG, "merge commit");
        var parsed = Commit.deserialize(commit.serialize());
        assertEquals(2, parsed.parents().size());
    }

    @Test
    void serializeDeserializeMultilineMessage() {
        var commit = new Commit(TREE_HASH, List.of(), SIG, SIG, "line one\nline two\nline three");
        var parsed = Commit.deserialize(commit.serialize());
        assertEquals("line one\nline two\nline three", parsed.message());
    }

    @Test
    void emptyMessageIsAllowed() {
        var commit = new Commit(TREE_HASH, List.of(), SIG, SIG, "");
        var parsed = Commit.deserialize(commit.serialize());
        assertEquals("", parsed.message());
    }

    @Test
    void authorAndCommitterCanDiffer() {
        var author = new Signature("Author", "author@e.com", Instant.ofEpochSecond(1));
        var committer = new Signature("Committer", "committer@e.com", Instant.ofEpochSecond(2));
        var commit = new Commit(TREE_HASH, List.of(), author, committer, "msg");
        var parsed = Commit.deserialize(commit.serialize());
        assertEquals("Author", parsed.author().name());
        assertEquals("Committer", parsed.committer().name());
    }

    @Test
    void rejectsMissingRequiredFields() {
        var badBytes = "tree aaaaa\n\na message\n".getBytes();
        assertThrows(IllegalArgumentException.class, () -> Commit.deserialize(badBytes));
    }

    @Test
    void parentsAreImmutable() {
        var mutable = new java.util.ArrayList<Hash>();
        mutable.add(PARENT_HASH);
        var commit = new Commit(TREE_HASH, mutable, SIG, SIG, "msg");
        mutable.add(new Hash("cccccccccccccccccccccccccccccccccccccccc"));
        assertEquals(1, commit.parents().size());
    }
}
