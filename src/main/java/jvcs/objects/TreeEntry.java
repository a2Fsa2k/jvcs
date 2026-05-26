package jvcs.objects;

import jvcs.hash.Hash;

import java.util.Objects;

public record TreeEntry(FileMode mode, String name, Hash hash) {

    public TreeEntry {
        Objects.requireNonNull(mode, "Entry mode must not be null");
        Objects.requireNonNull(name, "Entry name must not be null");
        Objects.requireNonNull(hash, "Entry hash must not be null");
        if (name.isEmpty())
            throw new IllegalArgumentException("Entry name must not be empty");
        if (name.contains("/") || name.contains("\0"))
            throw new IllegalArgumentException("Entry name contains invalid characters: " + name);
    }
}
