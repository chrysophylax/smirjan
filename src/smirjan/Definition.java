package smirjan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Everything a .def file describes, fully resolved. */
final class Definition {
    final Settings settings = new Settings();
    final Map<Character, PhonemeClass> classes = new LinkedHashMap<>();
    final Set<Character> nuclei = new LinkedHashSet<>();

    Weighted<Template> syllable;
    Weighted<Template> syllableInitial;
    Weighted<Template> syllableMedial;
    Weighted<Template> syllableFinal;
    Weighted<Template> syllableMono;

    /** Word length in syllables; null when only morae are constrained. */
    Weighted<Integer> wordSyllables;
    /** Word length in morae; null for no mora constraint. */
    Weighted<Integer> wordMorae;

    final int[] roleMorae = {0, 1, 1}; // onset, nucleus, coda
    final Map<Character, Integer> classMorae = new HashMap<>();
    final Map<String, Integer> phonemeMorae = new HashMap<>();
    int heavyMorae = 2;

    Stress stress = Stress.NONE;
    Stress stressFallback;
    String stressMark = "ˈ";
    String secondaryMark = "ˌ";
    boolean secondaryStress;
    boolean markMonosyllables;
    String syllableSeparator = "";

    /** Tone markers, "" meaning an unmarked tone; null for no tones. */
    Weighted<String> tones;
    /** Each tone marker mapped back to how the .def wrote it, for TSV output. */
    final Map<String, String> toneLabels = new HashMap<>();
    ToneBearing toneBearing = ToneBearing.SYLLABLE;
    TonePosition tonePosition;
    final Map<String, String> toneContours = new HashMap<>();

    final ClusterTable clusters = new ClusterTable();
    final List<Filter> filters = new ArrayList<>();
    final List<Pattern> rejects = new ArrayList<>();

    enum Stress {
        NONE, INITIAL, SECOND, FINAL, PENULT, ANTEPENULT,
        /** Penult if heavy, else antepenult (Latin). */
        HEAVY_PENULT,
        /** Final if heavy, else penult. */
        HEAVY_FINAL,
        LEFTMOST_HEAVY, RIGHTMOST_HEAVY,
        /** Lexical: any syllable, pseudorandomly. */
        FREE;

        static Stress parse(String s) {
            return switch (s.toLowerCase()) {
                case "none" -> NONE;
                case "initial", "first" -> INITIAL;
                case "second", "peninitial" -> SECOND;
                case "final", "last", "ultimate" -> FINAL;
                case "penult", "penultimate" -> PENULT;
                case "antepenult", "antepenultimate" -> ANTEPENULT;
                case "heavy-penult", "latin" -> HEAVY_PENULT;
                case "heavy-final" -> HEAVY_FINAL;
                case "leftmost-heavy" -> LEFTMOST_HEAVY;
                case "rightmost-heavy" -> RIGHTMOST_HEAVY;
                case "free", "random", "lexical" -> FREE;
                default -> throw new IllegalArgumentException("unknown stress rule '" + s + "'");
            };
        }
    }

    enum ToneBearing { SYLLABLE, MORA, STRESSED }

    enum TonePosition { NUCLEUS, AFTER, BEFORE }

    /** Onsets always weigh the onset value; phoneme and class overrides apply to nuclei and codas. */
    int morae(Seg s) {
        if (s.role() == Seg.Role.ONSET) {
            return roleMorae[0];
        }
        Integer w = phonemeMorae.get(s.text());
        if (w == null && s.cls() != null) {
            w = classMorae.get(s.cls());
        }
        return w != null ? w : roleMorae[s.role().ordinal()];
    }
}
