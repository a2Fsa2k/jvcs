package jvcs.objects;

import jvcs.hash.Hash;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record Tree(List<TreeEntry> entries) {

    public Tree {
        Objects.requireNonNull(entries, "Entries must not be null");
        var sorted = new ArrayList<>(entries);
        sorted.sort(Tree::compareEntries);
        checkDuplicates(sorted);
        entries = List.copyOf(sorted);
    }

    private static int compareEntries(TreeEntry a, TreeEntry b) {
        return a.name().compareTo(b.name());
    }

    private static void checkDuplicates(List<TreeEntry> sorted) {
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).name().equals(sorted.get(i - 1).name()))
                throw new IllegalArgumentException(
                    "Duplicate entry: " + sorted.get(i).name());
        }
    }

    public byte[] serialize() {
        var out = new ByteArrayOutputStream();
        for (var entry : entries) {
            var modeBytes = entry.mode().value().getBytes(StandardCharsets.US_ASCII);
            var nameBytes = entry.name().getBytes(StandardCharsets.UTF_8);
            var rawHash = entry.hash().toRawBytes();

            out.writeBytes(modeBytes);
            out.write(' ');
            out.writeBytes(nameBytes);
            out.write(0);
            out.writeBytes(rawHash);
        }
        return out.toByteArray();
    }

    public static Tree deserialize(byte[] data) {
        var entries = new ArrayList<TreeEntry>();
        var offset = 0;

        while (offset < data.length) {
            // Read mode (up to space)
            var modeStart = offset;
            while (offset < data.length && data[offset] != ' ') offset++;
            if (offset >= data.length) throw formatError();
            var modeStr = new String(data, modeStart, offset - modeStart, StandardCharsets.US_ASCII);
            offset++; // skip space

            // Read name (up to null byte)
            var nameStart = offset;
            while (offset < data.length && data[offset] != 0) offset++;
            if (offset >= data.length) throw formatError();
            var name = new String(data, nameStart, offset - nameStart, StandardCharsets.UTF_8);
            offset++; // skip null

            // Read 20-byte SHA-1 hash
            if (offset + 20 > data.length) throw formatError();
            var rawHash = Arrays.copyOfRange(data, offset, offset + 20);
            offset += 20;

            entries.add(new TreeEntry(new FileMode(modeStr), name, Hash.fromRawBytes(rawHash)));
        }

        return new Tree(entries);
    }

    private static RuntimeException formatError() {
        return new IllegalArgumentException("Malformed tree data");
    }
}
