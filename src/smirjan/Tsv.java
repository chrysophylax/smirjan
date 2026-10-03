package smirjan;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;

/** Tab-separated output: a header row, then one row per word. */
final class Tsv {
    /**
     * A word column.
     *
     * @param perPart for a compound written as separate words, which has no
     *                single stress or syllable string: list each part's value,
     *                joined with " + ", instead of the whole word's
     */
    private record Column(String name, boolean perPart, BiFunction<Generator.Word, Boolean, String> value) {}

    /**
     * <ul>
     * <li>word: as printed normally</li>
     * <li>phonemes: space-separated, without stress or tone marks</li>
     * <li>syllables: plain syllables joined with '.'</li>
     * <li>morae: total weight; weights: per syllable, joined with '.'</li>
     * <li>stress: 1-based index of the stressed syllable, empty if none</li>
     * <li>tones: per syllable as written in the .def, joined with '.'; empty without tones</li>
     * </ul>
     */
    private static final List<Column> COLUMNS = List.of(
            new Column("word", false, (w, t) -> w.text()),
            new Column("phonemes", true, (w, t) -> nfc(join(w, " ", " "))),
            new Column("syllables", true, (w, t) -> nfc(join(w, "", "."))),
            new Column("syllable_count", false, (w, t) -> Integer.toString(w.syllables().size())),
            new Column("morae", false, (w, t) -> Integer.toString(Arrays.stream(w.morae()).sum())),
            new Column("weights", true, (w, t) -> String.join(".",
                    Arrays.stream(w.morae()).mapToObj(Integer::toString).toList())),
            new Column("stress", true, (w, t) -> w.primary() >= 0 ? Integer.toString(w.primary() + 1) : ""),
            new Column("tones", true, (w, hasTones) -> hasTones ? String.join(".", w.tones()) : ""));

    static final String HEADER = String.join("\t", COLUMNS.stream().map(Column::name).toList());

    private final boolean hasTones;
    private final boolean assigned;
    private final boolean compounds;
    private final boolean shifts;

    /**
     * Output for {@code def}; {@code assigned} adds the --assign meaning
     * columns, and {@code shifts} the --shift columns.
     */
    Tsv(Definition def, boolean assigned, boolean shifts) {
        this.hasTones = def.tones != null;
        this.assigned = assigned;
        this.compounds = def.compounds;
        this.shifts = shifts;
    }

    String header() {
        return HEADER + (assigned ? "\tmeaning_number\tmeaning" : "") + (compounds ? "\tcompound\tcomponents" : "")
                + (shifts ? "\tshift\tshift_kind" : "");
    }

    /**
     * A full row: the word columns, then with --assign the meaning's number and
     * gloss, then with compounds: yes the compound kind and its components,
     * then with --shift the new meaning and how the shift is realised.
     */
    String row(Generator.Word w, Integer number, String gloss, List<String> components, SemanticShifts.Shift shift) {
        StringBuilder b = new StringBuilder(row(w, hasTones));
        if (assigned) {
            b.append('\t').append(number == null ? "" : number).append('\t').append(clean(gloss));
        }
        if (compounds) {
            b.append('\t').append(w.kind()).append('\t').append(clean(String.join(" + ", components)));
        }
        if (shifts) {
            b.append('\t').append(shift == null ? "" : clean(shift.target()))
                    .append('\t').append(shift == null ? "" : shift.kind().label());
        }
        return b.toString();
    }

    /** The word columns alone. */
    static String row(Generator.Word w, boolean hasTones) {
        List<String> out = new ArrayList<>();
        for (Column c : COLUMNS) {
            out.add(clean(value(c, w, hasTones)));
        }
        return String.join("\t", out);
    }

    private static String value(Column c, Generator.Word w, boolean hasTones) {
        Generator.Compound cp = w.compound();
        if (!c.perPart() || cp == null || cp.separator().isEmpty()) {
            return c.value().apply(w, hasTones);
        }
        List<String> values = cp.parts().stream().map(p -> value(c, p, hasTones)).toList();
        return values.stream().allMatch(String::isEmpty) ? "" : String.join(" + ", values);
    }

    /** Every phoneme, joined within a syllable by {@code inner} and between syllables by {@code outer}. */
    private static String join(Generator.Word w, String inner, String outer) {
        StringJoiner syllables = new StringJoiner(outer);
        for (List<Seg> syl : w.syllables()) {
            StringJoiner s = new StringJoiner(inner);
            for (Seg seg : syl) {
                s.add(seg.text());
            }
            syllables.add(s.toString());
        }
        return syllables.toString();
    }

    private static String nfc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    /** TSV fields cannot hold tabs or line breaks. */
    private static String clean(String s) {
        return s.replaceAll("[\\t\\r\\n]", " ");
    }
}
