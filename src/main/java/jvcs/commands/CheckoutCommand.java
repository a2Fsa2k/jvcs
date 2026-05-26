package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.objects.Blob;
import jvcs.objects.Commit;
import jvcs.objects.Tree;
import jvcs.objects.TreeEntry;
import jvcs.refs.Refs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CheckoutCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var args = ctx.args();

        if (args.isEmpty()) {
            ctx.err().println("usage: " + usage());
            return;
        }

        var repo = ctx.repository();
        var refs = new Refs(repo.jvcsPath());

        if (args.get(0).equals("-b")) {
            if (args.size() < 2) {
                ctx.err().println("usage: " + usage());
                return;
            }
            var branchName = args.get(1);
            var headHash = refs.resolveHead();
            if (headHash == null) {
                ctx.err().println("fatal: no commits yet");
                return;
            }
            refs.writeRef(branchName, headHash);
            refs.setHeadRef("refs/heads/" + branchName);
            ctx.out().println("Switched to a new branch '" + branchName + "'");
            return;
        }

        // Checkout existing branch
        var branchName = args.get(0);
        var branchHash = refs.readRef(branchName);
        if (branchHash == null) {
            ctx.err().println("error: pathspec '" + branchName + "' did not match");
            return;
        }

        var currentRef = refs.readHeadRef();
        if (branchName.equals(currentRef)) {
            ctx.out().println("Already on '" + branchName + "'");
            return;
        }

        // Load the commit's tree and write it to disk
        try {
            var commit = repo.objects().loadCommit(branchHash);
            writeTreeToDisk(commit.tree(), repo.root(), repo);
        } catch (Exception e) {
            ctx.err().println("fatal: failed to checkout: " + e.getMessage());
            return;
        }

        refs.setHeadRef("refs/heads/" + branchName);
        ctx.out().println("Switched to branch '" + branchName + "'");
    }

    private void writeTreeToDisk(Hash treeHash, Path dir, jvcs.repository.Repository repo) throws IOException {
        var tree = repo.objects().loadTree(treeHash);
        // Track which paths exist in this tree so we can clean up stale files
        var expectedPaths = new java.util.HashSet<String>();

        // First pass: collect expected paths
        collectPaths(tree, "", repo, expectedPaths);

        // Remove files that are tracked but not in the target tree
        // (skip .jvcs directory)
        cleanUntracked(dir, expectedPaths, repo);

        // Write tree contents
        writeTree(tree, dir, repo);
    }

    private void collectPaths(Tree tree, String prefix, jvcs.repository.Repository repo,
                              java.util.HashSet<String> paths) {
        for (var entry : tree.entries()) {
            var path = prefix.isEmpty() ? entry.name() : prefix + "/" + entry.name();
            if (entry.mode().isTree()) {
                try {
                    var subTree = repo.objects().loadTree(entry.hash());
                    paths.add(path + "/");
                    collectPaths(subTree, path, repo, paths);
                } catch (Exception ignored) {}
            } else {
                paths.add(path);
            }
        }
    }

    private void cleanUntracked(Path dir, java.util.HashSet<String> expected,
                                jvcs.repository.Repository repo) throws IOException {
        try (var files = Files.list(dir)) {
            for (var path : files.toList()) {
                var name = path.getFileName().toString();
                if (name.equals(".jvcs")) continue;

                var relative = repo.root().relativize(path).toString();
                // Check if this file or its contents are in the expected set
                if (expected.contains(relative) || expected.contains(relative + "/"))
                    continue;

                if (Files.isDirectory(path)) {
                    cleanUntracked(path, expected, repo);
                    // Remove empty directories that aren't expected
                    try (var remaining = Files.list(path)) {
                        if (remaining.findAny().isEmpty())
                            Files.deleteIfExists(path);
                    }
                } else if (!expected.contains(relative)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private void writeTree(Tree tree, Path dir, jvcs.repository.Repository repo) throws IOException {
        for (var entry : tree.entries()) {
            var entryPath = dir.resolve(entry.name());
            if (entry.mode().isTree()) {
                Files.createDirectories(entryPath);
                var subTree = repo.objects().loadTree(entry.hash());
                writeTree(subTree, entryPath, repo);
            } else {
                Files.createDirectories(entryPath.getParent());
                var blob = repo.objects().loadBlob(entry.hash());
                Files.write(entryPath, blob.content());
            }
        }
    }

    @Override
    public String description() {
        return "Switch branches";
    }

    @Override
    public String usage() {
        return "checkout [-b] <branch>";
    }
}
