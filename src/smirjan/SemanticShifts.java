package smirjan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simulates semantic change on an assigned lexicon (--shift): some words take
 * on a new meaning along a shift recorded in the Database of Semantic Shifts
 * (Zalizniak et al. 2024; CC BY 4.0, via Concepticon), such as 'fire' to
 * 'electricity' or 'to shoot'.
 *
 * <p>Each word with a meaning shifts with probability {@code rate}. The new
 * meaning is drawn from the shifts recorded for the old one, weighted by the
 * number of language families attesting each, so that recurrent shifts are
 * chosen more often than ones seen in a single family. Each shift is realised
 * either by <b>polysemy</b>, the word itself gaining the new sense, or by
 * <b>derivation</b>, a word derived from it carrying the new sense, in
 * proportion to how often the database attests each.
 */
final class SemanticShifts {
    enum Kind {
        POLYSEMY, DERIVATION;

        String label() {
            return name().toLowerCase();
        }
    }

    /** A word's new meaning, and how the shift is realised. */
    record Shift(String target, Kind kind) {}

    /** A recorded shift: the number of families attesting it by polysemy and by derivation. */
    private record Recorded(String target, int polysemy, int derivation) {}

    private static Map<Integer, List<Recorded>> shifts;

    private static synchronized Map<Integer, List<Recorded>> load() {
        if (shifts == null) {
            Map<Integer, List<Recorded>> m = new HashMap<>();
            Meanings.read("shifts.tsv", c -> m.computeIfAbsent(Integer.parseInt(c[0]), k -> new ArrayList<>())
                    .add(new Recorded(c[1], Integer.parseInt(c[2]), Integer.parseInt(c[3]))));
            shifts = m;
        }
        return shifts;
    }

    private SemanticShifts() {}

    /**
     * The shift of each entry, in the same order, null where a word keeps its
     * meaning. The draws come from their own stream of the seed, so the words
     * and their meanings are those printed without --shift.
     */
    static List<Shift> simulate(List<Lexicon.Entry> entries, String seed, double rate) {
        Random r = Rng.stream(seed, "shift");
        List<Shift> out = new ArrayList<>();
        for (Lexicon.Entry e : entries) {
            out.add(e.concept() > 0 && r.nextDouble() < rate ? shift(e.concept(), r) : null);
        }
        return out;
    }

    private static Shift shift(int concept, Random r) {
        Map<Recorded, Double> pool = new LinkedHashMap<>();
        for (Recorded s : load().getOrDefault(concept, List.of())) {
            pool.put(s, (double) (s.polysemy + s.derivation));
        }
        Recorded s = Weighted.of(pool).pick(r);
        if (s == null) {
            return null; // no shift recorded from this meaning
        }
        boolean polysemy = r.nextDouble() * (s.polysemy + s.derivation) < s.polysemy;
        return new Shift(s.target, polysemy ? Kind.POLYSEMY : Kind.DERIVATION);
    }
}
