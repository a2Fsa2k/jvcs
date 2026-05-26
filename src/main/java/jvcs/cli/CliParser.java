package jvcs.cli;

import jvcs.commands.Command;
import jvcs.exceptions.JvcsException;
import jvcs.exceptions.RepositoryNotFoundException;
import jvcs.repository.Repository;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;

public class CliParser {
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_ERROR = 1;

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private String programName = "jvcs";

    public void register(String name, Command command) {
        commands.put(name, command);
    }

    public void setProgramName(String name) {
        this.programName = name;
    }

    public int execute(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || args[0].equals("--help") || args[0].equals("help")) {
            if (args.length >= 2 && !args[0].equals("--help"))
                showCommandHelp(args[1], out);
            else
                showGlobalHelp(out);
            return EXIT_SUCCESS;
        }

        var commandName = args[0];
        var command = commands.get(commandName);
        if (command == null) {
            err.println("jvcs: '" + commandName + "' is not a jvcs command.");
            err.println("See 'jvcs --help' for available commands.");
            return EXIT_ERROR;
        }

        var commandArgs = Arrays.copyOfRange(args, 1, args.length);

        if (commandArgs.length > 0 && commandArgs[0].equals("--help")) {
            showCommandHelp(command, out);
            return EXIT_SUCCESS;
        }

        Repository repository = resolveRepository(commandName, err);
        var ctx = new CommandContext(
            List.of(commandArgs),
            repository,
            out,
            err
        );

        try {
            command.execute(ctx);
            return EXIT_SUCCESS;
        } catch (JvcsException e) {
            err.println("error: " + e.getMessage());
            return EXIT_ERROR;
        } catch (Exception e) {
            err.println("fatal: " + e.getMessage());
            return EXIT_ERROR;
        }
    }

    private Repository resolveRepository(String commandName, PrintStream err) {
        if (commandName.equals("init"))
            return null;
        try {
            return Repository.open(Path.of("."));
        } catch (RepositoryNotFoundException e) {
            err.println("fatal: not a jvcs repository (or any parent)");
            return null;
        }
    }

    private void showGlobalHelp(PrintStream out) {
        out.println("usage: " + programName + " <command> [<args>]");
        out.println();
        out.println("Available commands:");
        var width = commands.keySet().stream().mapToInt(String::length).max().orElse(8) + 2;
        for (var entry : commands.entrySet()) {
            out.println("   " + padRight(entry.getKey(), width) + entry.getValue().description());
        }
        out.println();
        out.println("See '" + programName + " <command> --help' for command-specific help.");
    }

    private void showCommandHelp(String name, PrintStream out) {
        var command = commands.get(name);
        if (command == null) {
            out.println("jvcs: unknown command '" + name + "'");
            return;
        }
        showCommandHelp(command, out);
    }

    private void showCommandHelp(Command command, PrintStream out) {
        out.println("usage: " + programName + " " + command.usage());
        out.println();
        out.println(command.description());
    }

    private static String padRight(String s, int width) {
        return s + " ".repeat(Math.max(0, width - s.length()));
    }
}
