package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.hash.Hash;

import java.io.IOException;

public class CatFileCommand implements Command {

    @Override
    public void execute(CommandContext ctx) {
        var args = ctx.args();
        if (args.size() < 2 || !args.get(0).equals("-p")) {
            ctx.err().println("usage: " + usage());
            return;
        }
        var hash = new Hash(args.get(1));
        var blob = ctx.repository().objects().loadBlob(hash);
        ctx.out().write(blob.content(), 0, blob.content().length);
    }

    @Override
    public String description() {
        return "Display contents of a blob object";
    }

    @Override
    public String usage() {
        return "cat-file -p <hash>";
    }
}
