package jvcs.repository;

import jvcs.exceptions.RepositoryNotFoundException;
import jvcs.store.ObjectStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Repository {
    private static final String JVCS_DIR = ".jvcs";
    private static final String OBJECTS_DIR = "objects";
    private static final String REFS_DIR = "refs";
    private static final String HEADS_DIR = "heads";
    private static final String HEAD_FILE = "HEAD";
    private static final byte[] HEAD_INITIAL = "ref: refs/heads/main\n".getBytes(StandardCharsets.UTF_8);

    private final Path rootPath;
    private final Path jvcsPath;
    private final ObjectStore objectStore;

    private Repository(Path rootPath) {
        this.rootPath = rootPath.toAbsolutePath().normalize();
        this.jvcsPath = this.rootPath.resolve(JVCS_DIR);
        this.objectStore = new ObjectStore(this.jvcsPath.resolve(OBJECTS_DIR));
    }

    public static Repository init(Path path) {
        var repo = new Repository(path);
        try {
            Files.createDirectories(repo.jvcsPath.resolve(OBJECTS_DIR));
            Files.createDirectories(repo.jvcsPath.resolve(REFS_DIR).resolve(HEADS_DIR));
            Files.write(repo.jvcsPath.resolve(HEAD_FILE), HEAD_INITIAL);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize repository at " + path, e);
        }
        return repo;
    }

    public static Repository open(Path path) {
        var root = findJvcsRoot(path);
        if (root == null)
            throw new RepositoryNotFoundException(path);
        return new Repository(root);
    }

    private static Path findJvcsRoot(Path start) {
        var current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (current.resolve(JVCS_DIR).toFile().isDirectory())
                return current;
            current = current.getParent();
        }
        return null;
    }

    public Path root() {
        return rootPath;
    }

    public Path jvcsPath() {
        return jvcsPath;
    }

    public ObjectStore objects() {
        return objectStore;
    }
}
