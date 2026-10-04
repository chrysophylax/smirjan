package smirjan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Pairs generated words with meanings from a list (--assign), expressing some
 * meanings as compounds of words for related meanings.
 *
 * <p>Basic meanings tend to have short words (Zipf's law of abbreviation): a
 * language rarely spends four syllables on 'I' or 'two'. The words are
 * generated first, and the meanings then take their words from most to least
 * basic, each drawing the length of its word from a Yule distribution over
 * the lengths still available, shortest first. The most basic meanings thus
 * usually, but not always, receive the shortest words.
 *
 * <p>Compounds are built backwards, as a language made from nothing must: for a
 * meaning such as 'body', find two related meanings in the concept graph
 * ('bone', 'blood'), give each a simple word (reusing one if the meaning already
 * has a word), and join the two words.
 *
 * <ul>
 * <li>A <b>dvandva</b> (samāhāra dvandva) joins two coordinate meanings into a
 *     collective whole: parts, members or close associates, e.g. 'bone' + 'blood' = 'body'.</li>
 * <li>A <b>determinative</b> compound has a head naming what kind of thing it is
 *     and a modifier narrowing it down: 'animal' + a modifier = 'bird'. Its
 *     written order follows {@code compound-order}.</li>
 * </ul>
 *
 * Only meanings for things (Concepticon's Person/Thing category) are compounded.
 * Every component must be linked to the meaning: by a Concepticon relation
 * (part, kind, similar), by Urban's (2011) derivation data, or by sharing a
 * CLICS colexification community. Candidates from the meaning's own semantic
 * field get a boost. See {@link ConceptGraph}.
 */
final class Lexicon {
    private static final int PAIR_ATTEMPTS = 6;
    /**
     * Yule weights r^-b c^r for the r-th shortest length still available. With
     * b = 1 and c = 0.9, a meaning takes a word of the shortest length about
     * half the time when five lengths remain.
     */
    private static final Distribution.Params LENGTH_WEIGHTS = new Distribution.Params(1.0, 0.9, 1.0);
    /**
     * Seeded noise on each meaning's basicness rank, which multiplies it by a
     * factor between 1 - RANK_JITTER and 1 + RANK_JITTER. Basicness is not the
     * only thing that shapes a word's length, and some basic words are long
     * ('sibling' beside 'mother'); the noise lets a basic meaning sometimes
     * choose after less basic ones and so receive a longer word.
     */
    private static final double RANK_JITTER = 0.5;

    /**
     * @param number    the meaning's number in the list, or null if it isn't on it
     * @param gloss     the meaning, "" for words beyond the end of the list
     * @param parts     for a compound, its components in written order
     * @param requested whether the meaning was one of those asked for, rather
     *                  than brought in as a component
     */
    record Entry(Integer number, int concept, String gloss, Generator.Word word, List<Entry> parts,
                 boolean requested) {}

    private final Definition def;
    private final Generator gen;
    private final ConceptGraph graph;
    private final Map<Integer, Meanings.Meaning> onList = new HashMap<>();
    /** Every entry made so far. */
    private final List<Entry> entries = new ArrayList<>();
    /** The word each concept is expressed by, for reusing component words. */
    private final Map<Integer, Entry> byConcept = new HashMap<>();
    /** Component pairs already used, in either order: one pair makes one compound. */
    private final Set<String> pairs = new HashSet<>();
    /** Simple words generated but not yet given a meaning, in the order they were generated. */
    private final List<Generator.Word> pool = new ArrayList<>();
    private final Random lengths;

    private Lexicon(Definition def, Generator gen, Meanings list) {
        this.def = def;
        this.gen = gen;
        this.graph = def.compounds ? ConceptGraph.get() : null;
        this.lengths = Rng.stream(def.settings.seed, "assign:lengths");
        for (Meanings.Meaning m : list.load()) {
            if (m.concept() > 0) {
                onList.putIfAbsent(m.concept(), m);
            }
        }
    }

    /**
     * Words for {@code count} meanings of {@code list}, chosen by the seed and
     * paired by basicness and length, plus any component words compounds
     * needed. Sorted by list number; components that aren't on the list
     * follow, then words beyond the list's end.
     */
    static List<Entry> build(Definition def, Generator gen, Meanings list, int count) {
        return new Lexicon(def, gen, list).build(list, count);
    }

    private List<Entry> build(Meanings list, int count) {
        List<Meanings.Meaning> order = list.shuffled(def.settings.seed);
        List<Meanings.Meaning> chosen = new ArrayList<>(order.subList(0, Math.min(count, order.size())));
        Random noise = Rng.stream(def.settings.seed, "assign:basicness");
        Map<Meanings.Meaning, Double> basicness = new HashMap<>();
        for (Meanings.Meaning m : list.load()) { // in list order, so each meaning's noise is fixed by the seed
            basicness.put(m, m.rank() * Rng.jitter(noise, RANK_JITTER));
        }
        chosen.sort(Comparator.comparingDouble((Meanings.Meaning m) -> basicness.get(m))
                .thenComparingInt(Meanings.Meaning::number));
        for (int i = 0; i < count; i++) { // the same words as without --assign
            Generator.Word w = gen.nextSimple();
            if (w == null) {
                break; // the phonology has run out of distinct words
            }
            pool.add(w);
        }
        Random r = gen.compoundRandom();
        for (Meanings.Meaning m : chosen) {
            Entry existing = m.concept() > 0 ? byConcept.get(m.concept()) : null;
            if (existing != null && !existing.requested
                    && (existing.number == null || existing.number == m.number())) {
                // Already made as a component of an earlier compound: now it's asked for too.
                Entry e = new Entry(m.number(), m.concept(), m.gloss(), existing.word, existing.parts, true);
                entries.set(entries.indexOf(existing), e);
                byConcept.put(m.concept(), e);
                continue;
            }
            Entry e = null;
            if (def.compounds && m.concept() > 0 && r.nextDouble() < chance(m.concept())) {
                e = compound(m, r);
            }
            if (e == null) {
                Generator.Word w = simple();
                if (w == null) {
                    break; // the phonology has run out of distinct words
                }
                e = new Entry(m.number(), m.concept(), m.gloss(), w, List.of(), true);
            }
            add(e);
        }
        for (int i = order.size(); i < count; i++) {
            Generator.Word w = pool.isEmpty() ? gen.nextSimple() : pool.removeFirst();
            if (w == null) {
                break;
            }
            add(new Entry(null, 0, "", w, List.of(), true));
        }
        entries.sort(Comparator.comparing((Entry e) -> e.number == null ? 1 : 0)
                .thenComparing(e -> e.number == null ? 0 : e.number)
                .thenComparing(e -> e.gloss.isEmpty() ? 1 : 0)
                .thenComparing(Entry::gloss));
        return entries;
    }

    /** Records an entry. Two list meanings can share a concept (dinner, supper); the first keeps it. */
    private void add(Entry e) {
        entries.add(e);
        if (e.concept > 0) {
            byConcept.putIfAbsent(e.concept, e);
        }
    }

    /**
     * How likely a meaning is to be a compound. {@code compound-rate} is the
     * chance for a meaning of average complexity; meanings that languages
     * nearly always express with a simple word ('fire', 'water') are compounded
     * much less often, and ones often expressed by complex words ('potter')
     * more often.
     */
    private double chance(int concept) {
        ConceptGraph.Concept c = graph.concept(concept);
        if (c == null || Double.isNaN(c.simplicity())) {
            return def.compoundRate;
        }
        return Math.min(1, def.compoundRate * (1 - c.simplicity()) / graph.meanComplexity);
    }

    /** Tries to express {@code m} as a compound; null if no suitable components are found. */
    private Entry compound(Meanings.Meaning m, Random r) {
        ConceptGraph.Concept c = graph.concept(m.concept());
        if (c == null || !c.isThing()) {
            return null;
        }
        String kind = gen.compoundKind();
        Function<Random, int[]> draw = kind.equals("dvandva") ? dvandva(c.id()) : determinative(c.id(), kind);
        for (int attempt = 0; attempt < PAIR_ATTEMPTS; attempt++) {
            int[] pair = draw.apply(r);
            if (pair == null) {
                return null;
            }
            String key = Math.min(pair[0], pair[1]) + ":" + Math.max(pair[0], pair[1]);
            if (pairs.contains(key)) {
                continue;
            }
            Entry a = component(pair[0]);
            Entry b = component(pair[1]);
            if (a == null || b == null) {
                return null;
            }
            Generator.Word w = gen.compound(kind, a.word, b.word);
            if (w != null) {
                pairs.add(key);
                // A component is new unless it is the concept's recorded entry.
                if (byConcept.get(a.concept) != a) {
                    add(a);
                }
                if (byConcept.get(b.concept) != b) {
                    add(b);
                }
                return new Entry(m.number(), m.concept(), m.gloss(), w, List.of(a, b), true);
            }
        }
        return null;
    }

    /**
     * A simple word for the next meaning: a length drawn from the Yule
     * weights over the lengths left in the pool, shortest first, and the
     * earliest generated word of that length. Null if the phonology has run
     * out of distinct words.
     */
    private Generator.Word simple() {
        if (pool.isEmpty()) { // compounds' components can use up the pool
            Generator.Word w = gen.nextSimple();
            if (w == null) {
                return null;
            }
            pool.add(w);
        }
        TreeMap<Integer, Generator.Word> shortest = new TreeMap<>();
        for (Generator.Word w : pool) {
            shortest.putIfAbsent(length(w), w);
        }
        double[] weights = Distribution.YULE.weights(shortest.size(), LENGTH_WEIGHTS);
        Map<Generator.Word, Double> candidates = new LinkedHashMap<>();
        int i = 0;
        for (Generator.Word w : shortest.values()) {
            candidates.put(w, weights[i++]);
        }
        Generator.Word w = Weighted.of(candidates).pick(lengths);
        pool.remove(w);
        return w;
    }

    /** A word's length in phonemes. */
    static int length(Generator.Word w) {
        return w.syllables().stream().mapToInt(List::size).sum();
    }

    /** The word for a component meaning: the existing one, or a new simple word. */
    private Entry component(int concept) {
        Entry e = byConcept.get(concept);
        if (e != null) {
            return e;
        }
        Generator.Word w = simple();
        if (w == null) {
            return null;
        }
        Meanings.Meaning m = onList.get(concept);
        return m != null
                ? new Entry(m.number(), concept, m.gloss(), w, List.of(), false)
                : new Entry(null, concept, graph.concept(concept).gloss(), w, List.of(), false);
    }

    // ------------------------------------------------------ choosing parts

    // The candidates for a meaning stay the same across attempts, so each
    // method gathers them once and returns a function drawing one pair.

    /** Two coordinate things that together make up {@code target}. */
    private Function<Random, int[]> dvandva(int target) {
        Map<Integer, Double> candidates = new LinkedHashMap<>();
        for (ConceptGraph.Link l : graph.links(target)) {
            double w = switch (l.kind()) {
                case PART -> 1.0;
                case NARROWER -> 0.6;
                case DERIVED_FROM -> 0.5;
                case SIMILAR -> 0.3;
                case WHOLE, BROADER -> 0;
            };
            offer(candidates, target, l.concept(), w, true);
        }
        community(candidates, target, true);
        return r -> {
            Map<Integer, Double> pool = new LinkedHashMap<>(candidates);
            Integer a = Weighted.of(pool).pick(r);
            if (a == null) {
                return null;
            }
            pool.remove(a);
            // Coordinates are more convincing from the same semantic field.
            String field = graph.concept(a).field();
            pool.replaceAll((k, w) -> graph.concept(k).field().equals(field) ? 2 * w : w);
            Integer b = Weighted.of(pool).pick(r);
            return b == null ? null : new int[] {a, b};
        };
    }

    /** A head naming what kind of thing {@code target} is, and a modifier; in written order. */
    private Function<Random, int[]> determinative(int target, String order) {
        Map<Integer, Double> heads = new LinkedHashMap<>();
        Map<Integer, Double> broader = new LinkedHashMap<>();
        Map<Integer, Double> modifiers = new LinkedHashMap<>();
        for (ConceptGraph.Link l : graph.links(target)) {
            switch (l.kind()) {
                case BROADER, DERIVED_FROM -> offer(broader, target, l.concept(), 1.0, true);
                case SIMILAR -> offer(heads, target, l.concept(), 0.3, true);
                case PART, WHOLE -> offer(modifiers, target, l.concept(), 0.6, false);
                case NARROWER -> { }
            }
        }
        community(heads, target, true);
        community(modifiers, target, false);
        // A broader term is the natural head (ant: insect); fall back to close relatives.
        Map<Integer, Double> headPool = broader.isEmpty() ? heads : broader;
        return r -> {
            Integer head = Weighted.of(headPool).pick(r);
            if (head == null) {
                return null;
            }
            Map<Integer, Double> mods = new LinkedHashMap<>(modifiers);
            mods.remove(head);
            Integer mod = Weighted.of(mods).pick(r);
            if (mod == null) {
                return null;
            }
            return order.equals("head-first") ? new int[] {head, mod} : new int[] {mod, head};
        };
    }

    /**
     * Offers the meanings that share the target's CLICS colexification
     * community: meanings that languages often express with the same word,
     * such as fire, wood, flame and firewood. Together they weigh as much as
     * two direct relations. Kinship terms only use explicit relations.
     */
    private void community(Map<Integer, Double> pool, int target, boolean thingsOnly) {
        if (graph.concept(target).field().equals("Kinship")) {
            return; // CLICS lumps nearly all kin terms together, so a shared community says little
        }
        List<Integer> members = graph.community(target);
        for (int id : members) {
            offer(pool, target, id, 2.0 / members.size(), thingsOnly);
        }
    }

    /** Adds a candidate component; candidates from the target's semantic field count double. */
    private void offer(Map<Integer, Double> pool, int target, int id, double w, boolean thingsOnly) {
        ConceptGraph.Concept c = graph.concept(id);
        ConceptGraph.Concept t = graph.concept(target);
        if (w <= 0 || id == target || c == null || restates(c, t)) {
            return;
        }
        boolean nominal = c.isThing() || (!thingsOnly && c.category().equals("Property"));
        Entry e = byConcept.get(id);
        if (nominal && (e == null || e.parts.isEmpty())) { // components are simple words
            pool.merge(id, c.field().equals(t.field()) ? 2 * w : w, Double::sum);
        }
    }

    private static final Pattern PARENTHESES = Pattern.compile("\\(.*?\\)");
    private static final Pattern ARTICLE = Pattern.compile("^(the|to|a) ");
    private static final Pattern ALTERNATIVES = Pattern.compile("/| or |,");

    /** A part that is one of the meaning's own glosses (leg/foot: foot) says nothing new. */
    static boolean restates(ConceptGraph.Concept part, ConceptGraph.Concept whole) {
        String p = headword(part.gloss());
        for (String alt : ALTERNATIVES.split(withoutParentheses(whole.gloss()))) {
            if (withoutArticle(alt.strip()).equals(p)) {
                return true;
            }
        }
        return false;
    }

    private static String headword(String gloss) {
        String g = withoutArticle(withoutParentheses(gloss).strip());
        int cut = g.indexOf('/');
        return (cut > 0 ? g.substring(0, cut) : g).strip();
    }

    private static String withoutParentheses(String gloss) {
        return PARENTHESES.matcher(gloss.toLowerCase()).replaceAll("");
    }

    private static String withoutArticle(String gloss) {
        return ARTICLE.matcher(gloss).replaceFirst("");
    }
}
