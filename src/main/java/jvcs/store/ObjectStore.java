package jvcs.store;

import jvcs.exceptions.CorruptObjectException;
import jvcs.exceptions.ObjectNotFoundException;
import jvcs.hash.Hash;
import jvcs.hash.Sha1Hasher;
import jvcs.objects.Blob;
import jvcs.objects.Commit;
import jvcs.objects.Tree;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ObjectStore {
    private final Path objectsDir;
    private final Sha1Hasher hasher = new Sha1Hasher();

    public ObjectStore(Path objectsDir) {
        this.objectsDir = objectsDir;
    }

    // -- Public API --

    public Hash store(Blob blob) {
        return storeInternal("blob", blob.content());
    }

    public Hash store(Tree tree) {
        return storeInternal("tree", tree.serialize());
    }

    public Blob loadBlob(Hash hash) {
        var record = loadInternal(hash);
        if (!record.type.equals("blob"))
            throw new CorruptObjectException("Expected blob, found " + record.type);
        return new Blob(record.content);
    }

    public Hash store(Commit commit) {
        return storeInternal("commit", commit.serialize());
    }

    public Tree loadTree(Hash hash) {
        var record = loadInternal(hash);
        if (!record.type.equals("tree"))
            throw new CorruptObjectException("Expected tree, found " + record.type);
        return Tree.deserialize(record.content);
    }

    public Commit loadCommit(Hash hash) {
        var record = loadInternal(hash);
        if (!record.type.equals("commit"))
            throw new CorruptObjectException("Expected commit, found " + record.type);
        return Commit.deserialize(record.content);
    }

    public boolean exists(Hash hash) {
        return objectPath(hash).toFile().exists();
    }

    // -- Storage internals --

    private Hash storeInternal(String type, byte[] content) {
        var envelope = buildEnvelope(type, content);
        var hash = hasher.hash(envelope);
        var path = objectPath(hash);

        if (path.toFile().exists())
            return hash;

        try {
            Files.createDirectories(path.getParent());
            try (var out = new DeflaterOutputStream(new FileOutputStream(path.toFile()))) {
                out.write(envelope);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store " + type + " object", e);
        }

        return hash;
    }

    private ObjectRecord loadInternal(Hash hash) {
        var path = objectPath(hash);

        if (!path.toFile().exists())
            throw new ObjectNotFoundException(hash);

        byte[] envelope;
        try (var in = new InflaterInputStream(new FileInputStream(path.toFile()))) {
            envelope = in.readAllBytes();
        } catch (IOException e) {
            throw new CorruptObjectException("Failed to read object " + hash.value(), e);
        }

        return parseEnvelope(envelope);
    }

    // -- Envelope format: "type size\0content" --

    private byte[] buildEnvelope(String type, byte[] content) {
        var header = (type + " " + content.length + "\0").getBytes(StandardCharsets.US_ASCII);
        var envelope = new byte[header.length + content.length];
        System.arraycopy(header, 0, envelope, 0, header.length);
        System.arraycopy(content, 0, envelope, header.length, content.length);
        return envelope;
    }

    private ObjectRecord parseEnvelope(byte[] envelope) {
        int nullPos = 0;
        while (nullPos < envelope.length && envelope[nullPos] != 0)
            nullPos++;

        if (nullPos >= envelope.length)
            throw new CorruptObjectException("Missing null terminator in object header");

        var header = new String(envelope, 0, nullPos, StandardCharsets.US_ASCII);
        int spacePos = header.indexOf(' ');
        if (spacePos < 0)
            throw new CorruptObjectException("Malformed object header: " + header);

        var type = header.substring(0, spacePos);
        var content = Arrays.copyOfRange(envelope, nullPos + 1, envelope.length);
        return new ObjectRecord(type, content);
    }

    private Path objectPath(Hash hash) {
        var value = hash.value();
        return objectsDir.resolve(value.substring(0, 2)).resolve(value.substring(2));
    }

    private record ObjectRecord(String type, byte[] content) {}
}
