package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.index.Index;
import jvcs.index.IndexEntry;
import jvcs.objects.Blob;
import jvcs.objects.Tree;
import jvcs.objects.TreeEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StatusCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var repo = ctx.repository();
        var index = Index.load(repo.jvcsPath().resolve("index"));

        // Get HEAD tree entries
        var headEntries = loadHeadEntries(ctx);

        // Get working directory state
        var workingSet = new HashSet<String>();
        collectWorkingFiles(Path.of("."), "", workingSet);

        // Compute staged changes (index vs HEAD)
        var stagedAdds = new ArrayList<String>();
        var stagedMods = new ArrayList<String>();
        var stagedDels = new ArrayList<String>();

        for (var entry : index.entries()) {
            var headHash = headEntries.get(entry.path());
            if (headHash == null)
                stagedAdds.add(entry.path());
            else if (!headHash.equals(entry.hash()))
                stagedMods.add(entry.path());
        }
        for (var headPath : headEntries.keySet()) {
            if (!index.has(headPath))
                stagedDels.add(headPath);
        }

        // Compute unstaged changes (working tree vs index)
        var unstagedMods = new ArrayList<String>();
        var unstagedDels = new ArrayList<String>();
        var untracked = new ArrayList<String>();

        for (var entry : index.entries()) {
            var path = Path.of(entry.path());
            if (!Files.exists(path)) {
                unstagedDels.add(entry.path());
            } else if (isModified(path, entry.hash(), repo)) {
                unstagedMods.add(entry.path());
            }
        }

        for (var file : workingSet) {
            if (!index.has(file))
                untracked.add(file);
        }

        // -- Output --
        boolean hasChanges = false;

        if (!stagedAdds.isEmpty() || !stagedMods.isEmpty() || !stagedDels.isEmpty()) {
            hasChanges = true;
            ctx.out().println("Changes to be committed:");
            for (var f : stagedAdds) ctx.out().println("   new:     " + f);
            for (var f : stagedMods) ctx.out().println("   modified: " + f);
            for (var f : stagedDels) ctx.out().println("   deleted:  " + f);
        }

        if (!unstagedMods.isEmpty() || !unstagedDels.isEmpty()) {
            hasChanges = true;
            ctx.out().println();
            ctx.out().println("Changes not staged for commit:");
            for (var f : unstagedMods) ctx.out().println("   modified: " + f);
            for (var f : unstagedDels) ctx.out().println("   deleted:  " + f);
        }

        if (!untracked.isEmpty()) {
            hasChanges = true;
            ctx.out().println();
            ctx.out().println("Untracked files:");
            for (var f : untracked) ctx.out().println("   " + f);
        }

        if (!hasChanges)
            ctx.out().println("nothing to commit, working tree clean");
    }

    private Map<String, Hash> loadHeadEntries(CommandContext ctx) {
        var headFile = ctx.repository().jvcsPath().resolve("HEAD");
        try {
            var headContent = Files.readString(headFile).trim();
            if (!headContent.startsWith("ref: "))
                return Map.of();

            var refPath = ctx.repository().jvcsPath().resolve(headContent.substring(5));
            if (!Files.exists(refPath))
                return Map.of();

            var commitHash = new Hash(Files.readString(refPath).trim());
            var commit = ctx.repository().objects().loadCommit(commitHash);
            var tree = ctx.repository().objects().loadTree(commit.tree());
            return flattenTree(tree, "", ctx);
        } catch (IOException e) {
            return Map.of();
        }
    }

    private Map<String, Hash> flattenTree(Tree tree, String prefix, CommandContext ctx) {
        var result = new HashMap<String, Hash>();
        for (var entry : tree.entries()) {
            var path = prefix.isEmpty() ? entry.name() : prefix + "/" + entry.name();
            if (entry.mode().isTree()) {
                try {
                    var subTree = ctx.repository().objects().loadTree(entry.hash());
                    result.putAll(flattenTree(subTree, path, ctx));
                } catch (Exception ignored) {}
            } else {
                result.put(path, entry.hash());
            }
        }
        return result;
    }

    private void collectWorkingFiles(Path dir, String prefix, Set<String> result) throws IOException {
        try (var files = Files.list(dir)) {
            for (var path : files.toList()) {
                var name = path.getFileName().toString();
                if (name.equals(".jvcs"))
                    continue;
                var relative = prefix.isEmpty() ? name : prefix + "/" + name;
                if (Files.isDirectory(path)) {
                    collectWorkingFiles(path, relative, result);
                } else {
                    result.add(relative);
                }
            }
        }
    }

    private boolean isModified(Path path, Hash expectedHash, jvcs.repository.Repository repo) throws IOException {
        var content = Files.readAllBytes(path);
        var blob = new Blob(content);
        var actualHash = repo.objects().store(blob);
        return !actualHash.equals(expectedHash);
    }

    @Override
    public String description() {
        return "Show the working tree status";
    }

    @Override
    public String usage() {
        return "status";
    }
}
