package smirjan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The release number, baked into the jar and native image from the bundled resource
 * {@code resources/smirjan/version.txt}. The committed file says {@code dev}; CI overwrites it
 * with the release number before building a release.
 */
final class Version {
    private Version() {}

    static String get() {
        try (InputStream in = Version.class.getResourceAsStream("version.txt")) {
            if (in != null) {
                String v = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
                if (!v.isEmpty()) {
                    return v;
                }
            }
        } catch (IOException e) {
            // Fall through: an unreadable version is no reason to fail.
        }
        return "dev";
    }
}
