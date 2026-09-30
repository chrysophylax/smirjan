package smirjan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * How the concepts on the bundled meaning lists relate to each other, for
 * building compounds. Built by tools/BuildMeaningData.java from sources that
 * are all CC BY 4.0:
 * <ul>
 * <li>Concepticon relations: part/whole, broader/narrower</li>
 * <li>Concepticon semantic fields and ontological categories</li>
 * <li>Urban (2011): meanings languages form from other meanings</li>
 * <li>WOLD simplicity scores (Haspelmath &amp; Tadmor 2009)</li>
 * <li>CLICS colexification communities (Rzymski et al. 2020), via NoRaRe:
 *     groups of meanings that languages often express with one word</li>
 * </ul>
 */
final class ConceptGraph {
    enum Kind { PART, WHOLE, BROADER, NARROWER, SIMILAR, DERIVED_FROM }

    /**
     * @param simplicity the share of languages that express this meaning with a
     *                   simple, unanalysable word (Haspelmath &amp; Tadmor 2009); NaN if unknown
     */
    record Concept(int id, String gloss, String field, String category, double simplicity, String community) {
        boolean isThing() {
            return category.equals("Person/Thing");
        }
    }

    /** A related concept, and how it relates. */
    record Link(int concept, Kind kind) {}

    final Map<Integer, Concept> concepts = new LinkedHashMap<>();
    private final Map<Integer, List<Link>> links = new HashMap<>();
    private final Map<String, List<Integer>> fields = new HashMap<>();
    private final Map<String, List<Integer>> communities = new HashMap<>();
    /** The average share of languages using a complex word for a meaning. */
    final double meanComplexity;

    private static ConceptGraph instance;

    static synchronized ConceptGraph get() {
        if (instance == null) {
            instance = new ConceptGraph();
        }
        return instance;
    }

    private ConceptGraph() {
        read("concepts.tsv", c -> {
            Concept k = new Concept(Integer.parseInt(c[0]), c[1], c[2], c[3],
                    c[4].isEmpty() ? Double.NaN : Double.parseDouble(c[4]), c[5]);
            concepts.put(k.id, k);
            fields.computeIfAbsent(k.field, f -> new ArrayList<>()).add(k.id);
            if (!k.community.isEmpty()) {
                communities.computeIfAbsent(k.community, f -> new ArrayList<>()).add(k.id);
            }
        });
        meanComplexity = concepts.values().stream().filter(k -> !Double.isNaN(k.simplicity))
                .mapToDouble(k -> 1 - k.simplicity).average().orElse(0.16);
        read("relations.tsv", c -> {
            for (String item : c[1].split(",")) {
                String[] p = item.split(":");
                Kind kind = p[1].equals("derived") ? Kind.DERIVED_FROM : Kind.valueOf(p[1].toUpperCase());
                add(Integer.parseInt(c[0]), Integer.parseInt(p[0]), kind);
            }
        });
    }

    private void add(int from, int to, Kind kind) {
        if (concepts.containsKey(from) && concepts.containsKey(to)) {
            links.computeIfAbsent(from, k -> new ArrayList<>()).add(new Link(to, kind));
        }
    }

    Concept concept(int id) {
        return concepts.get(id);
    }

    List<Link> links(int id) {
        return links.getOrDefault(id, List.of());
    }

    /** Concepts in the same CLICS colexification community, including this one. */
    List<Integer> community(int id) {
        Concept c = concepts.get(id);
        return c == null || c.community.isEmpty() ? List.of() : communities.getOrDefault(c.community, List.of());
    }

    /** Other concepts in the same semantic field. */
    List<Integer> field(int id) {
        Concept c = concepts.get(id);
        return c == null ? List.of() : fields.getOrDefault(c.field, List.of());
    }

    private static void read(String name, Consumer<String[]> row) {
        String resource = "meanings/" + name;
        try (InputStream in = ConceptGraph.class.getResourceAsStream(resource)) {
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
