package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.hash.Sha1Hasher;
import jvcs.objects.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WriteTreeCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var hash = writeTree(Path.of("."), ctx);
        ctx.out().println(hash.value());
    }

    private Hash writeTree(Path dir, CommandContext ctx) throws IOException {
        var entries = new ArrayList<TreeEntry>();

        try (var files = Files.list(dir)) {
            for (var path : files.toList()) {
                var name = path.getFileName().toString();
                if (name.equals(".jvcs")) continue;

                if (Files.isDirectory(path)) {
                    var treeHash = writeTree(path, ctx);
                    entries.add(new TreeEntry(FileMode.DIRECTORY, name, treeHash));
                } else {
                    var content = Files.readAllBytes(path);
                    var blobHash = ctx.repository().objects().store(new Blob(content));
                    entries.add(new TreeEntry(FileMode.REGULAR_FILE, name, blobHash));
                }
            }
        }

        var tree = new Tree(entries);
        return ctx.repository().objects().store(tree);
    }

    @Override
    public String description() {
        return "Create a tree object from the current working directory";
    }

    @Override
    public String usage() {
        return "write-tree";
    }
}
