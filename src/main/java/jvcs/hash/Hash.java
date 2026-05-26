package jvcs.hash;

import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public record Hash(String value) implements Comparable<Hash> {

    private static final int HEX_LENGTH = 40;

    public Hash {
        Objects.requireNonNull(value, "Hash value must not be null");
        value = value.toLowerCase(Locale.ROOT);
        if (value.length() != HEX_LENGTH || !isHex(value))
            throw new IllegalArgumentException("Invalid hash: " + value);
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                return false;
        }
        return true;
    }

    public static Hash fromRawBytes(byte[] raw) {
        Objects.requireNonNull(raw, "Raw bytes must not be null");
        if (raw.length != 20)
            throw new IllegalArgumentException("Raw hash must be 20 bytes, got " + raw.length);
        return new Hash(HexFormat.of().formatHex(raw));
    }

    public byte[] toRawBytes() {
        return HexFormat.of().parseHex(value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(Hash other) {
        return this.value.compareTo(other.value);
    }
}
