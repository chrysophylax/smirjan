package smirjan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Seed handling. Every independent source of randomness (each class's weight
 * jitter, each optional element's rate, the generator itself) gets its own
 * stream derived from the user's seed and a label, so that adding a class or a
 * syllable shape does not reshuffle everything else.
 */
final class Rng {
    private Rng() {}

    static long derive(String seed, String label) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(seed.getBytes(StandardCharsets.UTF_8));
            sha.update((byte) 0);
            sha.update(label.getBytes(StandardCharsets.UTF_8));
            byte[] d = sha.digest();
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v = (v << 8) | (d[i] & 0xff);
            }
            return v;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** java.util.Random has a specified algorithm, so streams are stable across JVMs. */
    static Random stream(String seed, String label) {
        return new Random(derive(seed, label));
    }

    /** A multiplicative jitter factor in [1 - amount, 1 + amount]. */
    static double jitter(Random r, double amount) {
        return 1.0 + amount * (2.0 * r.nextDouble() - 1.0);
    }
}
