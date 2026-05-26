package jvcs.commands;

import jvcs.cli.CommandContext;

public interface Command {
    void execute(CommandContext ctx) throws Exception;
    String description();
    String usage();
}
