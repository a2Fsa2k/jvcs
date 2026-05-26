package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.index.Index;
import jvcs.index.IndexEntry;
import jvcs.objects.Blob;
import jvcs.objects.FileMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        if (ctx.args().isEmpty()) {
            ctx.err().println("usage: " + usage());
            return;
        }

        var repo = ctx.repository();
        var index = Index.load(repo.jvcsPath().resolve("index"));
        var workDir = repo.root();

        for (var arg : ctx.args()) {
            var path = workDir.resolve(arg).normalize();
            if (!Files.exists(path)) {
                ctx.err().println("fatal: path '" + arg + "' does not exist");
                continue;
            }
            addPath(workDir, path, index, repo);
        }

        index.save();
    }

    private void addPath(Path workDir, Path path, Index index, jvcs.repository.Repository repo) throws IOException {
        if (Files.isDirectory(path)) {
            try (var files = Files.list(path)) {
                for (var child : files.toList()) {
                    addPath(workDir, child, index, repo);
                }
            }
        } else {
            var content = Files.readAllBytes(path);
            var blobHash = repo.objects().store(new Blob(content));
            var relativePath = workDir.relativize(path).toString();
            if (File.separatorChar != '/')
                relativePath = relativePath.replace(File.separatorChar, '/');
            var entry = new IndexEntry(FileMode.REGULAR_FILE, relativePath, blobHash);
            index.add(entry);
        }
    }

    @Override
    public String description() {
        return "Add file contents to the index (staging area)";
    }

    @Override
    public String usage() {
        return "add <file>...";
    }
}
