package jvcs.objects;

import java.util.Objects;

public record FileMode(String value) {

    public static final FileMode REGULAR_FILE = new FileMode("100644");
    public static final FileMode EXECUTABLE_FILE = new FileMode("100755");
    public static final FileMode DIRECTORY = new FileMode("40000");

    private static final String MODE_PATTERN = "[0-7]{5,6}";

    public FileMode {
        Objects.requireNonNull(value, "File mode must not be null");
        if (!value.matches(MODE_PATTERN))
            throw new IllegalArgumentException("Invalid file mode: " + value);
    }

    public boolean isTree() {
        return this.equals(DIRECTORY);
    }
}
