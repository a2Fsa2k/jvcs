package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.objects.Blob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HashObjectCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        if (ctx.args().isEmpty()) {
            ctx.err().println("usage: " + usage());
            return;
        }
        var path = Path.of(ctx.args().get(0));
        var content = Files.readAllBytes(path);
        var blob = new Blob(content);
        var hash = ctx.repository().objects().store(blob);
        ctx.out().println(hash.value());
    }

    @Override
    public String description() {
        return "Compute object hash and optionally create a blob";
    }

    @Override
    public String usage() {
        return "hash-object <file>";
    }
}
