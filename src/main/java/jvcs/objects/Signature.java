package jvcs.objects;

import java.time.Instant;
import java.util.Objects;

public record Signature(String name, String email, Instant timestamp) {

    public Signature {
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(email, "Email must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        if (name.isEmpty())
            throw new IllegalArgumentException("Name must not be empty");
    }

    public String serialize() {
        return name + " <" + email + "> " + timestamp.getEpochSecond() + " +0000";
    }

    public static Signature deserialize(String line) {
        int emailStart = line.indexOf('<');
        int emailEnd = line.indexOf('>');
        if (emailStart < 0 || emailEnd < 0)
            throw new IllegalArgumentException("Malformed signature: " + line);

        var name = line.substring(0, emailStart).trim();
        var email = line.substring(emailStart + 1, emailEnd);

        var rest = line.substring(emailEnd + 1).trim();
        int spacePos = rest.indexOf(' ');
        if (spacePos < 0)
            throw new IllegalArgumentException("Malformed signature timestamp: " + line);

        var epoch = Long.parseLong(rest.substring(0, spacePos));
        return new Signature(name, email, Instant.ofEpochSecond(epoch));
    }
}
