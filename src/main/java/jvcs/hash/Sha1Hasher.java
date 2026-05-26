package jvcs.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1Hasher {
    public Hash hash(byte[] data) {
        try {
            var md = MessageDigest.getInstance("SHA-1");
            return Hash.fromRawBytes(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }
}
