package jvcs.refs;

import jvcs.hash.Hash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Refs {
    private static final String HEAD_FILE = "HEAD";
    private static final String REFS_HEADS = "refs/heads";
    private static final String REF_PREFIX = "ref: ";

    private final Path jvcsPath;

    public Refs(Path jvcsPath) {
        this.jvcsPath = jvcsPath;
    }

    public Hash resolveHead() throws IOException {
        var content = readFile(HEAD_FILE);
        if (content == null) return null;
        return resolveRef(content.trim());
    }

    public String readHeadRef() throws IOException {
        var content = readFile(HEAD_FILE);
        if (content == null) return null;
        var trimmed = content.trim();
        if (trimmed.startsWith(REF_PREFIX))
            return trimmed.substring(REF_PREFIX.length());
        return null;
    }

    public void setHeadRef(String refName) throws IOException {
        writeFile(HEAD_FILE, REF_PREFIX + refName + "\n");
    }

    public Hash readRef(String refName) throws IOException {
        var content = readFile(REFS_HEADS + "/" + refName);
        if (content == null) return null;
        return new Hash(content.trim());
    }

    public void writeRef(String refName, Hash hash) throws IOException {
        var path = jvcsPath.resolve(REFS_HEADS).resolve(refName);
        Files.createDirectories(path.getParent());
        writeFile(REFS_HEADS + "/" + refName, hash.value() + "\n");
    }

    public void deleteRef(String refName) throws IOException {
        var path = jvcsPath.resolve(REFS_HEADS).resolve(refName);
        Files.deleteIfExists(path);
    }

    public Map<String, Hash> listHeads() throws IOException {
        var headsDir = jvcsPath.resolve(REFS_HEADS);
        if (!Files.isDirectory(headsDir))
            return Map.of();

        var result = new LinkedHashMap<String, Hash>();
        try (var files = Files.list(headsDir)) {
            for (var file : files.sorted().toList()) {
                var name = file.getFileName().toString();
                var content = Files.readString(file, StandardCharsets.UTF_8).trim();
                result.put(name, new Hash(content));
            }
        }
        return result;
    }

    private Hash resolveRef(String ref) throws IOException {
        if (ref.startsWith(REF_PREFIX)) {
            var refPath = ref.substring(REF_PREFIX.length());
            var content = readFile(refPath);
            if (content == null) return null;
            return new Hash(content.trim());
        }
        return new Hash(ref);
    }

    private String readFile(String relativePath) throws IOException {
        var path = jvcsPath.resolve(relativePath);
        if (!Files.exists(path)) return null;
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private void writeFile(String relativePath, String content) throws IOException {
        var path = jvcsPath.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
