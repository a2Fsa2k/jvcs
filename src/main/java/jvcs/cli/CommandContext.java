package jvcs.cli;

import jvcs.repository.Repository;

import java.io.PrintStream;
import java.util.List;

public class CommandContext {
    private final List<String> args;
    private final Repository repository;
    private final PrintStream out;
    private final PrintStream err;

    CommandContext(List<String> args, Repository repository, PrintStream out, PrintStream err) {
        this.args = List.copyOf(args);
        this.repository = repository;
        this.out = out;
        this.err = err;
    }

    public List<String> args() {
        return args;
    }

    public Repository repository() {
        if (repository == null)
            throw new IllegalStateException("No repository available");
        return repository;
    }

    public boolean hasRepository() {
        return repository != null;
    }

    public PrintStream out() {
        return out;
    }

    public PrintStream err() {
        return err;
    }
}
