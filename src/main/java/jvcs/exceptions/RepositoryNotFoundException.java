package jvcs.exceptions;

import java.nio.file.Path;

public class RepositoryNotFoundException extends JvcsException {
    public RepositoryNotFoundException(Path path) {
        super("Not a jvcs repository: " + path.toAbsolutePath());
    }
}
