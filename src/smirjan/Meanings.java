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
import java.util.function.Consumer;

/**
 * Standard meaning lists bundled from Concepticon (CC BY 4.0). {@link Lexicon}
 * pairs them with generated words.
 */
enum Meanings {
    LPJ("lpj", "Leipzig-Jakarta list"),
    DLG("dlg", "Dolgopolsky list"),
    WLT("wlt", "Loanword Typology (WOLD) meaning list");

    /**
     * @param concept the Concepticon concept set ID, or 0 where the list entry has none
     * @param rank    how basic the meaning is, 1 being the most basic: the
     *                list's published ranking (lpj, wlt) or order (dlg)
     */
    record Meaning(int number, int concept, String gloss, int rank) {}

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

    private List<Meaning> list;

    /** The list in its published order. */
    synchronized List<Meaning> load() {
        if (list == null) {
            List<Meaning> out = new ArrayList<>();
            read(key + ".tsv", cols -> out.add(new Meaning(Integer.parseInt(cols[0]),
                    cols[1].isEmpty() ? 0 : Integer.parseInt(cols[1]), cols[2], Integer.parseInt(cols[3]))));
            list = List.copyOf(out);
        }
        return list;
    }

    /** The list in a seeded random order: the order in which meanings are handed out. */
    List<Meaning> shuffled(String seed) {
        List<Meaning> meanings = new ArrayList<>(load());
        Random r = Rng.stream(seed, "assign:" + key);
        for (int i = meanings.size() - 1; i > 0; i--) { // Fisher-Yates, stable across JDKs
            int j = r.nextInt(i + 1);
            meanings.set(j, meanings.set(i, meanings.get(j)));
        }
        return meanings;
    }

    /** Reads a bundled tab-separated file, skipping blank and '#' lines. */
    static void read(String name, Consumer<String[]> row) {
        String resource = "meanings/" + name;
        try (InputStream in = Meanings.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing bundled resource " + resource);
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    row.accept(line.split("\t", -1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
