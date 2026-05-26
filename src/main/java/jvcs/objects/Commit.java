package jvcs.objects;

import jvcs.hash.Hash;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Commit(Hash tree, List<Hash> parents, Signature author, Signature committer, String message) {

    public Commit {
        Objects.requireNonNull(tree, "Tree hash must not be null");
        Objects.requireNonNull(parents, "Parents must not be null");
        Objects.requireNonNull(author, "Author must not be null");
        Objects.requireNonNull(committer, "Committer must not be null");
        Objects.requireNonNull(message, "Message must not be null");
        parents = List.copyOf(parents);
    }

    public byte[] serialize() {
        var sb = new StringBuilder();
        sb.append("tree ").append(tree.value()).append('\n');
        for (var parent : parents)
            sb.append("parent ").append(parent.value()).append('\n');
        sb.append("author ").append(author.serialize()).append('\n');
        sb.append("committer ").append(committer.serialize()).append('\n');
        sb.append('\n');
        sb.append(message).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static Commit deserialize(byte[] data) {
        var text = new String(data, StandardCharsets.UTF_8);

        int blankLine = text.indexOf("\n\n");
        if (blankLine < 0)
            throw new IllegalArgumentException("Malformed commit: no blank line");

        var headerLines = text.substring(0, blankLine).split("\n");
        var message = text.substring(blankLine + 2);
        // Strip trailing newline (part of format, not content)
        if (message.endsWith("\n"))
            message = message.substring(0, message.length() - 1);

        Hash treeHash = null;
        var parentHashes = new ArrayList<Hash>();
        Signature author = null;
        Signature committer = null;

        for (var line : headerLines) {
            if (line.startsWith("tree "))
                treeHash = new Hash(line.substring(5));
            else if (line.startsWith("parent "))
                parentHashes.add(new Hash(line.substring(7)));
            else if (line.startsWith("author "))
                author = Signature.deserialize(line.substring(7));
            else if (line.startsWith("committer "))
                committer = Signature.deserialize(line.substring(9));
        }

        if (treeHash == null || author == null || committer == null)
            throw new IllegalArgumentException("Malformed commit: missing required fields");

        return new Commit(treeHash, parentHashes, author, committer, message);
    }
}
