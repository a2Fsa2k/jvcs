import jvcs.cli.CliParser;
import jvcs.commands.*;

public class Main {
    public static void main(String[] args) {
        var cli = new CliParser();
        registerCommands(cli);
        System.exit(cli.execute(args, System.out, System.err));
    }

    private static void registerCommands(CliParser cli) {
        cli.register("init", new InitCommand());
        cli.register("hash-object", new HashObjectCommand());
        cli.register("cat-file", new CatFileCommand());
        cli.register("write-tree", new WriteTreeCommand());
        cli.register("commit-tree", new CommitTreeCommand());
        cli.register("add", new AddCommand());
        cli.register("status", new StatusCommand());
        cli.register("commit", new CommitCommand());
        cli.register("log", new LogCommand());
        cli.register("branch", new BranchCommand());
        cli.register("checkout", new CheckoutCommand());
        cli.register("diff", new DiffCommand());
    }
}
