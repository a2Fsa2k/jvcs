package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.index.Index;
import jvcs.objects.Commit;
import jvcs.objects.Signature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

public class CommitCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var args = ctx.args();
        String message = null;
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals("-m") && i + 1 < args.size()) {
                message = args.get(i + 1);
                break;
            }
        }

        if (message == null) {
            ctx.err().println("usage: " + usage());
            return;
        }

        var repo = ctx.repository();
        var index = Index.load(repo.jvcsPath().resolve("index"));

        if (index.isEmpty()) {
            ctx.err().println("nothing to commit");
            return;
        }

        // Build tree from index
        var treeHash = index.buildTree(repo.objects());

        // Determine parent commit from HEAD
        List<Hash> parents;
        var headRef = readHead(repo);
        if (headRef != null) {
            parents = List.of(headRef);
        } else {
            parents = List.of();
        }

        var sig = new Signature("jvcs User", "user@jvcs", Instant.now());
        var commit = new Commit(treeHash, parents, sig, sig, message);
        var commitHash = repo.objects().store(commit);

        // Update HEAD ref
        updateHead(repo, commitHash);

        // Clear index
        index.save(); // Already saved state; clear for next commit
        // Actually, Git doesn't clear the index after commit - it keeps it
        // The index reflects the working tree state

        ctx.out().println("[" + commitHash.value().substring(0, 7) + "] " + message);
    }

    private Hash readHead(jvcs.repository.Repository repo) throws IOException {
        var headFile = repo.jvcsPath().resolve("HEAD");
        if (!Files.exists(headFile))
            return null;

        var content = Files.readString(headFile, StandardCharsets.UTF_8).trim();
        if (!content.startsWith("ref: "))
            return null;

        var refPath = repo.jvcsPath().resolve(content.substring(5));
        if (!Files.exists(refPath))
            return null;

        return new Hash(Files.readString(refPath, StandardCharsets.UTF_8).trim());
    }

    private void updateHead(jvcs.repository.Repository repo, Hash commitHash) throws IOException {
        var headFile = repo.jvcsPath().resolve("HEAD");
        var content = Files.readString(headFile, StandardCharsets.UTF_8).trim();

        if (!content.startsWith("ref: "))
            throw new IllegalStateException("Detached HEAD not supported");

        var refPath = repo.jvcsPath().resolve(content.substring(5));
        Files.writeString(refPath, commitHash.value() + "\n", StandardCharsets.UTF_8);
    }

    @Override
    public String description() {
        return "Record changes to the repository";
    }

    @Override
    public String usage() {
        return "commit -m <message>";
    }
}
