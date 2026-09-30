package smirjan;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A named, ordered set of phonemes, e.g. {@code D: n þ s t d}. */
final class PhonemeClass {
    final char name;
    final Weighted<String> phonemes;
    final Set<String> members;

    PhonemeClass(char name, Weighted<String> phonemes) {
        this.name = name;
        this.phonemes = phonemes;
        this.members = new LinkedHashSet<>(phonemes.items());
    }

    List<String> list() {
        return phonemes.items();
    }
}
