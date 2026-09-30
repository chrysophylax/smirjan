package smirjan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Standard meaning lists bundled from Concepticon (CC BY 4.0), and seeded
 * assignment of generated words to their meanings.
 */
enum Meanings {
    LPJ("lpj", "Leipzig-Jakarta list"),
    DLG("dlg", "Dolgopolsky list"),
    WLT("wlt", "Loanword Typology (WOLD) meaning list");

    record Meaning(int number, String gloss) {}

    final String key;
    final String title;

    Meanings(String key, String title) {
        this.key = key;
        this.title = title;
    }

    static Meanings parse(String s) {
        return switch (s.toLowerCase()) {
            case "lpj", "leipzig-jakarta" -> LPJ;
            case "dlg", "dolgopolsky" -> DLG;
            case "wlt", "wold", "loanword-typology" -> WLT;
            default -> throw new IllegalArgumentException(
                    "unknown meaning list '" + s + "' (expected lpj, dlg or wlt)");
        };
    }

    /** The list in its published order. */
    List<Meaning> load() {
        String resource = "meanings/" + key + ".tsv";
        try (InputStream in = Meanings.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing bundled resource " + resource);
            }
            List<Meaning> out = new ArrayList<>();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] cols = line.split("\t", 2);
                out.add(new Meaning(Integer.parseInt(cols[0]), cols[1]));
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Pairs each word with a distinct meaning, chosen by a seeded shuffle that is
     * independent of word generation, so the words are the same with or without
     * --assign. The result is in list order; words beyond the list's length
     * follow with a null meaning.
     */
    <W> List<Assigned<W>> assign(List<W> words, String seed) {
        List<Meaning> meanings = new ArrayList<>(load());
        Random r = Rng.stream(seed, "assign:" + key);
        for (int i = meanings.size() - 1; i > 0; i--) { // Fisher-Yates, stable across JDKs
            int j = r.nextInt(i + 1);
            meanings.set(j, meanings.set(i, meanings.get(j)));
        }
        List<Assigned<W>> out = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            out.add(new Assigned<>(words.get(i), i < meanings.size() ? meanings.get(i) : null));
        }
        out.sort((a, b) -> a.meaning == null || b.meaning == null
                ? Boolean.compare(a.meaning == null, b.meaning == null)
                : Integer.compare(a.meaning.number, b.meaning.number));
        return out;
    }

    record Assigned<W>(W word, Meaning meaning) {}
}
