package jvcs.objects;

import java.util.Arrays;
import java.util.Objects;

public record Blob(byte[] content) {

    public Blob {
        Objects.requireNonNull(content, "Content must not be null");
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Blob other && Arrays.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "Blob[" + content.length + " bytes]";
    }
}
