package jvcs.index;

import jvcs.hash.Hash;
import jvcs.objects.FileMode;
import jvcs.objects.Tree;
import jvcs.objects.TreeEntry;
import jvcs.store.ObjectStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Index {
    private final Path indexFile;
    private final Map<String, IndexEntry> entries = new LinkedHashMap<>();

    public Index(Path indexFile) {
        this.indexFile = indexFile;
    }

    // -- Lifecycle --

    public static Index load(Path indexFile) {
        var index = new Index(indexFile);
        if (Files.exists(indexFile)) {
            try {
                var lines = Files.readAllLines(indexFile, StandardCharsets.UTF_8);
                for (var line : lines) {
                    if (!line.isBlank()) {
                        var entry = IndexEntry.deserialize(line);
                        index.entries.put(entry.path(), entry);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load index", e);
            }
        }
        return index;
    }

    public void save() {
        try {
            Files.createDirectories(indexFile.getParent());
            var lines = new ArrayList<String>();
            for (var entry : entries.values())
                lines.add(entry.serialize());
            Files.write(indexFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save index", e);
        }
    }

    // -- Mutators --

    public void add(IndexEntry entry) {
        entries.put(entry.path(), entry);
    }

    public void remove(String path) {
        entries.remove(path);
    }

    // -- Queries --

    public IndexEntry get(String path) {
        return entries.get(path);
    }

    public boolean has(String path) {
        return entries.containsKey(path);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public Collection<IndexEntry> entries() {
        return List.copyOf(entries.values());
    }

    public int size() {
        return entries.size();
    }

    // -- Tree building --

    public Hash buildTree(ObjectStore store) {
        return buildTree(entries.values(), store);
    }

    private Hash buildTree(Collection<IndexEntry> entries, ObjectStore store) {
        var dirs = new HashMap<String, List<IndexEntry>>();
        var files = new ArrayList<IndexEntry>();

        for (var entry : entries) {
            var path = entry.path();
            var slash = path.indexOf('/');
            if (slash < 0) {
                files.add(entry);
            } else {
                var dir = path.substring(0, slash);
                var rest = path.substring(slash + 1);
                dirs.computeIfAbsent(dir, k -> new ArrayList<>())
                    .add(new IndexEntry(entry.mode(), rest, entry.hash()));
            }
        }

        var treeEntries = new ArrayList<TreeEntry>();

        // Sort directory names for deterministic output
        var sortedDirs = new ArrayList<>(dirs.keySet());
        Collections.sort(sortedDirs);
        for (var dir : sortedDirs) {
            var dirHash = buildTree(dirs.get(dir), store);
            treeEntries.add(new TreeEntry(FileMode.DIRECTORY, dir, dirHash));
        }

        for (var entry : files) {
            treeEntries.add(new TreeEntry(entry.mode(), entry.path(), entry.hash()));
        }

        return store.store(new Tree(treeEntries));
    }
}
