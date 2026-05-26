package jvcs.cli;

import jvcs.commands.Command;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliParserTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outContent);
    private final PrintStream err = new PrintStream(errContent);

    private CliParser parserWith(Command command) {
        var p = new CliParser();
        p.register("testcmd", command);
        return p;
    }

    @Test
    void noArgsShowsHelp() {
        var parser = parserWith(nullCommand());
        var code = parser.execute(new String[0], out, err);
        assertEquals(0, code);
        assertTrue(outContent.toString().contains("usage:"));
    }

    @Test
    void helpFlagShowsHelp() {
        var parser = parserWith(nullCommand());
        var code = parser.execute(new String[]{"--help"}, out, err);
        assertEquals(0, code);
        assertTrue(outContent.toString().contains("usage:"));
    }

    @Test
    void helpSubcommandShowsHelp() {
        var parser = parserWith(nullCommand());
        var code = parser.execute(new String[]{"help"}, out, err);
        assertEquals(0, code);
        assertTrue(outContent.toString().contains("usage:"));
    }

    @Test
    void commandSpecificHelp() {
        var parser = parserWith(new TestCommand());
        var code = parser.execute(new String[]{"testcmd", "--help"}, out, err);
        assertEquals(0, code);
        var output = outContent.toString();
        assertTrue(output.contains("usage:"));
        assertTrue(output.contains("test command"));
    }

    @Test
    void helpForSpecificCommand() {
        var parser = parserWith(new TestCommand());
        var code = parser.execute(new String[]{"help", "testcmd"}, out, err);
        assertEquals(0, code);
        assertTrue(outContent.toString().contains("test command"));
    }

    @Test
    void unknownCommandReturnsError() {
        var parser = parserWith(nullCommand());
        var code = parser.execute(new String[]{"unknown"}, out, err);
        assertEquals(1, code);
        assertTrue(errContent.toString().contains("unknown"));
    }

    @Test
    void commandReceivesArgs() {
        var cmd = new TestCommand();
        var parser = parserWith(cmd);
        parser.execute(new String[]{"testcmd", "--flag", "value"}, out, err);
        assertEquals(2, cmd.lastArgs.size());
        assertEquals("--flag", cmd.lastArgs.get(0));
        assertEquals("value", cmd.lastArgs.get(1));
    }

    @Test
    void commandReceivesStreams() {
        var cmd = new TestCommand();
        var parser = parserWith(cmd);
        parser.execute(new String[]{"testcmd"}, out, err);
        assertEquals("executed\n", outContent.toString());
    }

    @Test
    void commandExceptionShowsError() {
        var failing = new Command() {
            @Override
            public void execute(CommandContext ctx) {
                throw new jvcs.exceptions.JvcsException("something broke");
            }
            @Override
            public String description() { return ""; }
            @Override
            public String usage() { return "fail"; }
        };
        var parser = parserWith(failing);
        var code = parser.execute(new String[]{"testcmd"}, out, err);
        assertEquals(1, code);
        assertTrue(errContent.toString().contains("something broke"));
    }

    @Test
    void registeredCommandDispatches() {
        var cmd = new TestCommand();
        var parser = parserWith(cmd);
        parser.execute(new String[]{"testcmd"}, out, err);
        assertTrue(cmd.executed);
    }

    // -- Helpers --

    private static Command nullCommand() {
        return new Command() {
            @Override
            public void execute(CommandContext ctx) {}

            @Override
            public String description() { return ""; }

            @Override
            public String usage() { return "testcmd"; }
        };
    }

    private static class TestCommand implements Command {
        List<String> lastArgs;
        CommandContext lastCtx;
        boolean executed;

        @Override
        public void execute(CommandContext ctx) {
            executed = true;
            lastArgs = ctx.args();
            lastCtx = ctx;
            ctx.out().println("executed");
        }

        @Override
        public String description() { return "test command"; }

        @Override
        public String usage() { return "testcmd [options]"; }
    }
}
