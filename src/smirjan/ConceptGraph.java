package smirjan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Meanings.read("concepts.tsv", c -> {
            Concept k = new Concept(Integer.parseInt(c[0]), c[1], c[2], c[3],
                    c[4].isEmpty() ? Double.NaN : Double.parseDouble(c[4]), c[5]);
            concepts.put(k.id, k);
            if (!k.community.isEmpty()) {
                communities.computeIfAbsent(k.community, f -> new ArrayList<>()).add(k.id);
            }
        });
        meanComplexity = concepts.values().stream().filter(k -> !Double.isNaN(k.simplicity))
                .mapToDouble(k -> 1 - k.simplicity).average().orElse(0.16);
        Meanings.read("relations.tsv", c -> {
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
}
