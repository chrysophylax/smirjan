package smirjan;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/** Tab-separated output: a header row, then one row per word. */
final class Tsv {
    static final String HEADER = String.join("\t",
            "word", "phonemes", "syllables", "syllable_count", "morae", "weights", "stress", "tones");

    private Tsv() {}

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
    static String row(Generator.Word w, boolean hasTones) {
        StringJoiner phonemes = new StringJoiner(" ");
        StringJoiner syllables = new StringJoiner(".");
        StringJoiner weights = new StringJoiner(".");
        int total = 0;
        for (int i = 0; i < w.syllables().size(); i++) {
            StringBuilder syl = new StringBuilder();
            for (Seg s : w.syllables().get(i)) {
                phonemes.add(s.text());
                syl.append(s.text());
            }
            syllables.add(syl);
            weights.add(Integer.toString(w.morae()[i]));
            total += w.morae()[i];
        }
        List<String> cols = new ArrayList<>(List.of(
                w.text(),
                nfc(phonemes.toString()),
                nfc(syllables.toString()),
                Integer.toString(w.syllables().size()),
                Integer.toString(total),
                weights.toString(),
                w.primary() >= 0 ? Integer.toString(w.primary() + 1) : "",
                hasTones ? String.join(".", w.tones()) : ""));
        cols.replaceAll(Tsv::clean);
        return String.join("\t", cols);
    }

    private static String nfc(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    /** TSV fields cannot hold tabs or line breaks. */
    private static String clean(String s) {
        return s.replaceAll("[\\t\\r\\n]", " ");
    }
}
