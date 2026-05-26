package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;
import jvcs.objects.Commit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LogCommand implements Command {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z")
            .withLocale(Locale.US)
            .withZone(ZoneId.of("UTC"));

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var repo = ctx.repository();
        var currentHash = resolveHead(repo);

        if (currentHash == null) {
            ctx.err().println("fatal: no commits yet");
            return;
        }

        while (currentHash != null) {
            try {
                var commit = repo.objects().loadCommit(currentHash);
                printCommit(ctx, currentHash, commit);
                currentHash = commit.parents().isEmpty() ? null : commit.parents().get(0);
                if (currentHash != null)
                    ctx.out().println();
            } catch (Exception e) {
                break;
            }
        }
    }

    private Hash resolveHead(jvcs.repository.Repository repo) throws IOException {
        var headFile = repo.jvcsPath().resolve("HEAD");
        if (!Files.exists(headFile))
            return null;

        var content = Files.readString(headFile, StandardCharsets.UTF_8).trim();
        if (content.startsWith("ref: ")) {
            var refPath = repo.jvcsPath().resolve(content.substring(5));
            if (!Files.exists(refPath))
                return null;
            return new Hash(Files.readString(refPath, StandardCharsets.UTF_8).trim());
        }

        return new Hash(content);
    }

    private void printCommit(CommandContext ctx, Hash hash, Commit commit) {
        ctx.out().println("commit " + hash.value());
        ctx.out().println("Author: " + commit.author().name() + " <" + commit.author().email() + ">");
        ctx.out().println("Date:   " + FORMATTER.format(commit.author().timestamp()));
        ctx.out().println();
        ctx.out().println("    " + commit.message().replace("\n", "\n    "));
        ctx.out().println();
    }

    @Override
    public String description() {
        return "Show commit logs";
    }

    @Override
    public String usage() {
        return "log";
    }
}
