package jvcs.repository;

import jvcs.exceptions.RepositoryNotFoundException;
import jvcs.objects.Blob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void initCreatesJvcsDirectory() {
        Repository.init(tempDir);
        assertTrue(Files.isDirectory(tempDir.resolve(".jvcs")));
    }

    @Test
    void initCreatesObjectsDirectory() {
        Repository.init(tempDir);
        assertTrue(Files.isDirectory(tempDir.resolve(".jvcs/objects")));
    }

    @Test
    void initCreatesRefsHeadsDirectory() {
        Repository.init(tempDir);
        assertTrue(Files.isDirectory(tempDir.resolve(".jvcs/refs/heads")));
    }

    @Test
    void initCreatesHEAD() {
        Repository.init(tempDir);
        assertTrue(Files.exists(tempDir.resolve(".jvcs/HEAD")));
    }

    @Test
    void initHeadPointsToMain() throws Exception {
        Repository.init(tempDir);
        var headContent = Files.readString(tempDir.resolve(".jvcs/HEAD"));
        assertEquals("ref: refs/heads/main\n", headContent);
    }

    @Test
    void initReturnsRepositoryWithCorrectPaths() {
        var repo = Repository.init(tempDir);
        assertEquals(tempDir.toAbsolutePath().normalize(), repo.root());
        assertEquals(tempDir.resolve(".jvcs").normalize(), repo.jvcsPath());
    }

    @Test
    void openFindsRepositoryInSubdirectory() {
        Repository.init(tempDir);
        var subdir = tempDir.resolve("src/main");
        subdir.toFile().mkdirs();

        var repo = Repository.open(subdir);
        assertEquals(tempDir.toAbsolutePath().normalize(), repo.root());
    }

    @Test
    void openReturnsRepositoryAtExactPath() {
        Repository.init(tempDir);
        var repo = Repository.open(tempDir);
        assertNotNull(repo);
    }

    @Test
    void openThrowsWhenNoRepository() {
        assertThrows(RepositoryNotFoundException.class,
            () -> Repository.open(tempDir));
    }

    @Test
    void repositoryProvidesObjectStore() {
        var repo = Repository.init(tempDir);
        var store = repo.objects();

        var hash = store.store(new Blob("stored via repo".getBytes(StandardCharsets.UTF_8)));
        var loaded = store.loadBlob(hash);

        assertArrayEquals("stored via repo".getBytes(StandardCharsets.UTF_8), loaded.content());
    }

    @Test
    void objectStorePersistsAcrossOpen() {
        var repo1 = Repository.init(tempDir);
        var blob = new Blob("persistent data".getBytes(StandardCharsets.UTF_8));
        var hash = repo1.objects().store(blob);

        var repo2 = Repository.open(tempDir);
        var loaded = repo2.objects().loadBlob(hash);
        assertArrayEquals("persistent data".getBytes(StandardCharsets.UTF_8), loaded.content());
    }
}
