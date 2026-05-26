package jvcs.commands;

import jvcs.cli.CommandContext;
import jvcs.index.Index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiffCommand implements Command {

    @Override
    public void execute(CommandContext ctx) throws IOException {
        var repo = ctx.repository();
        var index = Index.load(repo.jvcsPath().resolve("index"));
        boolean anyDiff = false;

        for (var entry : index.entries()) {
            var path = repo.root().resolve(entry.path());
            if (!Files.exists(path)) continue;

            var oldBytes = repo.objects().loadBlob(entry.hash()).content();
            var newBytes = Files.readAllBytes(path);

            if (Arrays.equals(oldBytes, newBytes)) continue;

            var oldLines = toLines(oldBytes);
            var newLines = toLines(newBytes);

            printFileDiff(ctx, entry.path(), oldLines, newLines);
            anyDiff = true;
        }

        if (!anyDiff)
            ctx.out().println("no changes");
    }

    private void printFileDiff(CommandContext ctx, String path, List<String> oldLines, List<String> newLines) {
        ctx.out().println("--- " + path);
        ctx.out().println("+++ " + path);

        var lcs = lcsTable(oldLines, newLines);
        var diff = backtrack(lcs, oldLines, newLines);

        for (var line : diff)
            ctx.out().println(line.prefix + line.text);
    }

    // -- LCS-based diff --

    private static int[][] lcsTable(List<String> a, List<String> b) {
        int m = a.size(), n = b.size();
        var dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.get(i - 1).equals(b.get(j - 1))
                    ? dp[i - 1][j - 1] + 1
                    : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp;
    }

    private static List<DiffLine> backtrack(int[][] dp, List<String> oldLines, List<String> newLines) {
        var result = new ArrayList<DiffLine>();
        int i = oldLines.size(), j = newLines.size();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines.get(i - 1).equals(newLines.get(j - 1))) {
                result.add(new DiffLine(" ", oldLines.get(i - 1)));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                result.add(new DiffLine("+", newLines.get(j - 1)));
                j--;
            } else {
                result.add(new DiffLine("-", oldLines.get(i - 1)));
                i--;
            }
        }
        var reversed = new ArrayList<DiffLine>();
        for (int k = result.size() - 1; k >= 0; k--)
            reversed.add(result.get(k));
        return reversed;
    }

    private record DiffLine(String prefix, String text) {}

    // -- Line splitting —

    private static List<String> toLines(byte[] bytes) {
        var text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        // Split on newlines, preserve trailing newline behavior
        var parts = text.split("\n", -1);
        // Remove trailing empty element that split adds for trailing \n
        var lines = new ArrayList<String>();
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].isEmpty() && text.endsWith("\n"))
                continue; // skip trailing empty from split
            lines.add(parts[i]);
        }
        return lines;
    }

    @Override
    public String description() {
        return "Show changes between working tree and index";
    }

    @Override
    public String usage() {
        return "diff";
    }
}
