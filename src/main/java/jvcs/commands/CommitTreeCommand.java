package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.objects.Commit;
import jvcs.objects.Signature;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommitTreeCommand implements Command {

    @Override
    public void execute(CommandContext ctx) {
        var args = ctx.args();
        if (args.isEmpty()) {
            ctx.err().println("usage: " + usage());
            return;
        }

        String treeSha = null;
        String parentSha = null;
        String message = null;

        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i)) {
                case "-p" -> {
                    if (i + 1 < args.size())
                        parentSha = args.get(++i);
                }
                case "-m" -> {
                    if (i + 1 < args.size())
                        message = args.get(++i);
                }
                default -> {
                    if (treeSha == null)
                        treeSha = args.get(i);
                }
            }
        }

        if (treeSha == null || message == null) {
            ctx.err().println("usage: " + usage());
            return;
        }

        var parents = parentSha != null
            ? List.of(new Hash(parentSha))
            : List.<Hash>of();

        var sig = new Signature("jvcs User", "user@jvcs", Instant.now());
        var commit = new Commit(new Hash(treeSha), parents, sig, sig, message);
        var commitHash = ctx.repository().objects().store(commit);
        ctx.out().println(commitHash.value());
    }

    @Override
    public String description() {
        return "Create a commit object from a tree hash";
    }

    @Override
    public String usage() {
        return "commit-tree <tree_sha> -m <message> [-p <parent_sha>]";
    }
}
