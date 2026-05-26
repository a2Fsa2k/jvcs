package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.repository.Repository;

import java.nio.file.Path;

public class InitCommand implements Command {

    @Override
    public void execute(CommandContext ctx) {
        var path = ctx.args().isEmpty() ? Path.of(".") : Path.of(ctx.args().get(0));
        var repo = Repository.init(path);
        ctx.out().println("Initialized empty jvcs repository at " + repo.jvcsPath());
    }

    @Override
    public String description() {
        return "Initialize a new jvcs repository";
    }

    @Override
    public String usage() {
        return "init [directory]";
    }
}
