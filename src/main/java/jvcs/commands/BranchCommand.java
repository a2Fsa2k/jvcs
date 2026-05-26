package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.refs.Refs;

import java.io.IOException;

public class BranchCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var args = ctx.args();
        var repo = ctx.repository();
        var refs = new Refs(repo.jvcsPath());
        var currentRef = refs.readHeadRef();
        var currentBranch = currentRef != null && currentRef.startsWith("refs/heads/")
            ? currentRef.substring("refs/heads/".length()) : currentRef;

        if (args.isEmpty()) {
            // List branches
            var heads = refs.listHeads();
            if (heads.isEmpty()) {
                ctx.out().println("No branches yet.");
                return;
            }
            for (var entry : heads.entrySet()) {
                var marker = entry.getKey().equals(currentBranch) ? "* " : "  ";
                ctx.out().println(marker + entry.getKey());
            }
            return;
        }

        if (args.size() == 1) {
            // Create branch
            var branchName = args.get(0);
            if (refs.readRef(branchName) != null) {
                ctx.err().println("fatal: a branch named '" + branchName + "' already exists");
                return;
            }
            var headHash = refs.resolveHead();
            if (headHash == null) {
                ctx.err().println("fatal: no commits yet");
                return;
            }
            refs.writeRef(branchName, headHash);
            return;
        }

        if (args.size() >= 2 && args.get(0).equals("-d")) {
            var branchName = args.get(1);
            var existing = refs.readRef(branchName);
            if (existing == null) {
                ctx.err().println("error: branch '" + branchName + "' not found");
                return;
            }
            if (branchName.equals(currentBranch)) {
                ctx.err().println("error: cannot delete branch '" + branchName + "' checked out");
                return;
            }
            refs.deleteRef(branchName);
            return;
        }

        ctx.err().println("usage: " + usage());
    }

    @Override
    public String description() {
        return "List, create, or delete branches";
    }

    @Override
    public String usage() {
        return "branch [-d <name>] [<name>]";
    }
}
