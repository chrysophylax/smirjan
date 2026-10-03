package smirjan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexifer-style cluster tables. The header row after {@code %} lists the second
 * element of a pair, the first column lists the first element. A cell of
 * {@code +} allows the pair, {@code -} rejects the word, and anything else
 * replaces the pair. Pairs not covered by any table are allowed. Tables look
 * at every pair of adjacent phonemes in the word, including across syllable
 * boundaries.
 *
 * <pre>
 * % a  i  u
 * a +  +  o
 * i -  +  uu
 * u -  -  +
 * </pre>
 */
final class ClusterTable {
    private final Map<String, Map<String, String>> cells = new HashMap<>();
    /** Phonemes of all classes, longest first, for splitting replacements. */
    private List<String> phonemes = List.of();

    boolean isEmpty() {
        return cells.isEmpty();
    }

    void put(String first, String second, String cell) {
        cells.computeIfAbsent(first, k -> new HashMap<>()).put(second, cell);
    }

    String lookup(String first, String second) {
        Map<String, String> row = cells.get(first);
        return row == null ? null : row.get(second);
    }

    /**
     * Applies the tables in place. Returns false if a forbidden pair is found.
     * A replacement of two phonemes (ŋk, uu) takes the pair's two places;
     * anything else becomes one segment in the first element's syllable. The
     * result is checked again against its new neighbours.
     */
    boolean apply(List<Seg> segs) {
        int i = 0;
        int guard = 0;
        while (i < segs.size() - 1) {
            Seg a = segs.get(i);
            Seg b = segs.get(i + 1);
            String cell = lookup(a.text(), b.text());
            if (cell == null || cell.equals("+")) {
                i++;
                continue;
            }
            if (cell.equals("-")) {
                return false;
            }
            if (++guard > 1000) {
                return false; // substitutions feeding each other forever
            }
            List<String> parts = split(cell);
            if (parts.size() == 2) {
                // Two phonemes replace two: each keeps its original place (n+k -> ŋ.k).
                segs.set(i, new Seg(parts.get(0), a.syl(), a.role(), null));
                segs.set(i + 1, new Seg(parts.get(1), b.syl(), b.role(), null));
            } else {
                Seg.Role role = a.role() == Seg.Role.NUCLEUS || b.role() == Seg.Role.NUCLEUS
                        ? Seg.Role.NUCLEUS : a.role();
                segs.set(i, new Seg(cell, a.syl(), role, null));
                segs.remove(i + 1);
            }
            i = Math.max(0, i - 1);
        }
        return true;
    }

    /** Parses one table. Labels may be phonemes or single-letter class names. */
    static void parse(List<String> rows, List<Integer> lines, Map<Character, PhonemeClass> classes,
            List<String> phonemesLongestFirst, ClusterTable into) throws DefinitionException {
        into.phonemes = phonemesLongestFirst;
        String[] header = rows.get(0).substring(1).strip().split("\\s+");
        if (header.length == 0 || header[0].isEmpty()) {
            throw DefinitionException.at(lines.get(0), "cluster table header lists no phonemes");
        }
        List<List<String>> seconds = new ArrayList<>();
        for (String h : header) {
            seconds.add(expand(h, classes));
        }
        if (rows.size() < 2) {
            throw DefinitionException.at(lines.get(0), "cluster table has no rows");
        }
        for (int r = 1; r < rows.size(); r++) {
            String[] cols = rows.get(r).strip().split("\\s+");
            if (cols.length != header.length + 1) {
                throw DefinitionException.at(lines.get(r), "cluster table row has " + (cols.length - 1)
                        + " cells but the header has " + header.length);
            }
            for (String first : expand(cols[0], classes)) {
                for (int c = 0; c < header.length; c++) {
                    for (String second : seconds.get(c)) {
                        into.put(first, second, cols[c + 1]);
                    }
                }
            }
        }
    }

    /** Splits a replacement into defined phonemes, longest first; unknown characters join the one before. */
    private List<String> split(String cell) {
        List<String> out = new ArrayList<>();
        for (Phonemes.Token t : Phonemes.tokens(cell, phonemes)) {
            out.add(t.known() || out.isEmpty() ? t.text() : out.removeLast() + t.text());
        }
        return out;
    }

    private static List<String> expand(String label, Map<Character, PhonemeClass> classes) {
        if (label.length() == 1 && classes.containsKey(label.charAt(0))) {
            return classes.get(label.charAt(0)).list();
        }
        return List.of(label);
    }
}
