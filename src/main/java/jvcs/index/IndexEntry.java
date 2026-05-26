package jvcs.index;

import jvcs.hash.Hash;
import jvcs.objects.FileMode;

import java.util.Objects;

public record IndexEntry(FileMode mode, String path, Hash hash) {

    public IndexEntry {
        Objects.requireNonNull(mode, "Entry mode must not be null");
        Objects.requireNonNull(path, "Entry path must not be null");
        Objects.requireNonNull(hash, "Entry hash must not be null");
        if (path.isEmpty())
            throw new IllegalArgumentException("Entry path must not be empty");
    }

    String serialize() {
        return mode.value() + "\t" + hash.value() + "\t" + path;
    }

    static IndexEntry deserialize(String line) {
        var parts = line.split("\t", 3);
        if (parts.length != 3)
            throw new IllegalArgumentException("Malformed index entry: " + line);
        return new IndexEntry(new FileMode(parts[0]), parts[2], new Hash(parts[1]));
    }
}
