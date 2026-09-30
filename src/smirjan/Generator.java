package smirjan;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/** Builds words from a {@link Definition}: shapes, phonotactics, then stress and tone. */
final class Generator {
    /** Consecutive failed or duplicate attempts after which we give up. */
    static final int MISS_LIMIT = 200_000;
    /** Attempts at a new compound before settling for a simple word. */
    static final int COMPOUND_ATTEMPTS = 1_000;

    private final Definition def;
    private final Random rng;
    /** Compounding has its own stream, so turning it on doesn't change the simple words. */
    private final Random compoundRng;
    private final Set<String> seen = new HashSet<>();

    Generator(Definition def) {
        this.def = def;
        this.rng = Rng.stream(def.settings.seed, "generate");
        this.compoundRng = Rng.stream(def.settings.seed, "compounds");
    }

    /**
     * A generated word.
     *
     * @param text      the word as printed, with stress and tone marks
     * @param syllables its phonemes, grouped by syllable
     * @param morae     the weight of each syllable
     * @param primary   index of the stressed syllable, or -1
     * @param tones     each syllable's tone as written in the .def ("" where none;
     *                  mora tones joined with '+')
     * @param marks     each syllable's tone marks, kept so compounds can reuse them
     * @param compound  how the word was compounded, or null for a simple word
     */
    record Word(String text, List<List<Seg>> syllables, int[] morae, int primary, List<String> tones,
                List<SylTone> marks, Compound compound) {}

    /**
     * @param kind      dvandva, head-final or head-first
     * @param parts     the component words, in written order
     * @param separator what was written between them ("" for one solid word)
     */
    record Compound(String kind, List<Word> parts, String separator) {}

    /**
     * A syllable's lexical tone: its own tone, one tone per nuclear mora, and the
     * accent it carries if stressed (pitch accent). Marks, not labels.
     */
    record SylTone(String tone, List<String> moraTones, String accent) {
        static final SylTone NONE = new SylTone("", List.of(), "");
    }

    /** Emits up to {@code count} distinct words; returns how many were emitted. */
    int generate(int count, Consumer<String> out) {
        return generateWords(count, w -> out.accept(w.text()));
    }

    /**
     * As {@link #generate}, with each word's structure. Words are distinct by
     * their rendered text. With {@code compounds: yes}, some words are compounds
     * of two simple words emitted earlier.
     */
    int generateWords(int count, Consumer<Word> out) {
        List<Word> simple = new ArrayList<>();
        int made = 0;
        while (made < count) {
            // Decide once per output word, so compound-rate is the share of compounds printed.
            boolean compound = def.compounds && simple.size() >= 2 && compoundRng.nextDouble() < def.compoundRate;
            Word w = compound ? nextCompound(simple) : null;
            if (w == null) {
                w = nextSimple();
                if (w == null) {
                    break; // the phonology has run out of distinct words
                }
                simple.add(w);
            }
            out.accept(w);
            made++;
        }
        return made;
    }

    /** A new compound of two of the given words; null if none can be found. */
    private Word nextCompound(List<Word> parts) {
        for (int misses = 0; misses < COMPOUND_ATTEMPTS; misses++) {
            Word a = parts.get(compoundRng.nextInt(parts.size()));
            Word b = parts.get(compoundRng.nextInt(parts.size()));
            Word w = a == b ? null : compound(compoundKind(), a, b);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    /** A new simple word, distinct from every word so far; null if none can be found. */
    Word nextSimple() {
        for (int misses = 0; misses < MISS_LIMIT; misses++) {
            Word w = word();
            if (w != null && seen.add(w.text())) {
                return w;
            }
        }
        return null;
    }

    /** Picks a compound kind by the definition's weights: dvandva, head-final or head-first. */
    String compoundKind() {
        return compoundKind(compoundRng);
    }

    String compoundKind(Random r) {
        return def.compoundTypes.pick(r).equals("dvandva") ? "dvandva" : def.compoundOrder.pick(r);
    }

    Random compoundRandom() {
        return compoundRng;
    }

    /**
     * Joins two words, in written order, into a compound; null if the result
     * breaks the phonotactics at the join or duplicates an existing word.
     */
    Word compound(String kind, Word first, Word second) {
        Compound c = new Compound(kind, List.of(first, second), def.compoundSeparator);
        Word w = def.compoundSeparator.isEmpty() ? solid(c) : separated(c);
        return w != null && seen.add(w.text()) ? w : null;
    }

    /** Written as separate words: each part keeps its own stress. */
    private Word separated(Compound c) {
        List<List<Seg>> syls = new ArrayList<>();
        List<String> tones = new ArrayList<>();
        List<SylTone> marks = new ArrayList<>();
        List<Integer> morae = new ArrayList<>();
        for (Word p : c.parts()) {
            syls.addAll(p.syllables());
            tones.addAll(p.tones());
            marks.addAll(p.marks());
            for (int m : p.morae()) {
                morae.add(m);
            }
        }
        String text = c.parts().get(0).text() + c.separator() + c.parts().get(1).text();
        return new Word(text, syls, morae.stream().mapToInt(Integer::intValue).toArray(), -1, tones, marks, c);
    }

    /**
     * Written as one word: the join is checked against cluster tables, filters
     * and rejects, then the whole is stressed as one word. Tones stay lexical.
     */
    private Word solid(Compound c) {
        List<Seg> segs = new ArrayList<>();
        Map<Integer, SylTone> tonesBySyl = new HashMap<>();
        int syl = 0;
        for (Word p : c.parts()) {
            for (int i = 0; i < p.syllables().size(); i++, syl++) {
                for (Seg s : p.syllables().get(i)) {
                    segs.add(new Seg(s.text(), syl, s.role(), s.cls()));
                }
                tonesBySyl.put(syl, p.marks().get(i));
            }
        }
        int firstSyllables = c.parts().getFirst().syllables().size();
        if (!def.clusters.apply(segs)) {
            return null;
        }
        for (Filter f : def.filters) {
            int boundary = 0;
            for (Seg s : segs) {
                if (s.syl() < firstSyllables) {
                    boundary += s.text().length();
                }
            }
            segs = f.apply(segs, boundary);
        }
        if (rejected(segs)) {
            return null;
        }
        List<List<Seg>> syls = syllables(segs);
        List<SylTone> marks = new ArrayList<>();
        for (List<Seg> s : syls) {
            marks.add(tonesBySyl.getOrDefault(s.getFirst().syl(), SylTone.NONE));
        }
        int[] morae = morae(syls);
        int primary = stress(def.stress, morae, 0, compoundRng);
        return render(syls, morae, primary, marks, c);
    }

    /** One attempt at a word; null if it broke a constraint. */
    Word word() {
        Integer targetMorae = def.wordMorae != null ? def.wordMorae.pick(rng) : null;
        int k = def.wordSyllables != null ? def.wordSyllables.pick(rng) : 1 + rng.nextInt(targetMorae);

        List<Seg> segs = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            if (!shapes(i, k).pick(rng).generate(rng, i, segs)) {
                return null;
            }
        }
        if (!def.clusters.apply(segs)) {
            return null;
        }
        for (Filter f : def.filters) {
            segs = f.apply(segs);
        }
        if (rejected(segs)) {
            return null;
        }
        List<List<Seg>> syls = syllables(segs);
        int[] morae = morae(syls);
        int total = 0;
        for (int m : morae) {
            total += m;
        }
        if (targetMorae != null && total != targetMorae) {
            return null;
        }
        int primary = primaryStress(morae);
        return render(syls, morae, primary, assignTones(syls, primary), null);
    }

    private boolean rejected(List<Seg> segs) {
        StringBuilder plain = new StringBuilder();
        for (Seg s : segs) {
            plain.append(s.text());
        }
        if (plain.isEmpty()) {
            return true;
        }
        for (var p : def.rejects) {
            if (p.matcher(plain).find()) {
                return true;
            }
        }
        return false;
    }

    private static List<List<Seg>> syllables(List<Seg> segs) {
        List<List<Seg>> syls = new ArrayList<>();
        int current = Integer.MIN_VALUE;
        for (Seg s : segs) {
            if (s.text().isEmpty()) {
                continue;
            }
            if (s.syl() != current) {
                syls.add(new ArrayList<>());
                current = s.syl();
            }
            syls.getLast().add(s);
        }
        return syls;
    }

    private int[] morae(List<List<Seg>> syls) {
        int[] morae = new int[syls.size()];
        for (int i = 0; i < syls.size(); i++) {
            for (Seg s : syls.get(i)) {
                morae[i] += def.morae(s);
            }
        }
        return morae;
    }

    private Weighted<Template> shapes(int i, int k) {
        Weighted<Template> w;
        if (k == 1) {
            w = def.syllableMono;
        } else if (i == 0) {
            w = def.syllableInitial;
        } else if (i == k - 1) {
            w = def.syllableFinal;
        } else {
            w = def.syllableMedial;
        }
        return w != null ? w : def.syllable;
    }

    // ------------------------------------------------------------- stress

    int primaryStress(int[] morae) {
        return stress(def.stress, morae, 0, rng);
    }

    private int stress(Definition.Stress rule, int[] morae, int depth, Random r) {
        int n = morae.length;
        return switch (rule) {
            case NONE -> -1;
            case INITIAL -> 0;
            case SECOND -> Math.min(1, n - 1);
            case FINAL -> n - 1;
            case PENULT -> Math.max(0, n - 2);
            case ANTEPENULT -> Math.max(0, n - 3);
            case HEAVY_PENULT -> n < 3 ? 0 : heavy(morae[n - 2]) ? n - 2 : n - 3;
            case HEAVY_FINAL -> n < 2 ? 0 : heavy(morae[n - 1]) ? n - 1 : n - 2;
            case LEFTMOST_HEAVY -> {
                for (int i = 0; i < n; i++) {
                    if (heavy(morae[i])) {
                        yield i;
                    }
                }
                yield fallback(Definition.Stress.INITIAL, morae, depth, r);
            }
            case RIGHTMOST_HEAVY -> {
                for (int i = n - 1; i >= 0; i--) {
                    if (heavy(morae[i])) {
                        yield i;
                    }
                }
                yield fallback(Definition.Stress.FINAL, morae, depth, r);
            }
            case FREE -> r.nextInt(n);
        };
    }

    private int fallback(Definition.Stress dflt, int[] morae, int depth, Random r) {
        Definition.Stress f = def.stressFallback != null && depth == 0 ? def.stressFallback : dflt;
        return stress(f, morae, depth + 1, r);
    }

    private boolean heavy(int morae) {
        return morae >= def.heavyMorae;
    }

    // ---------------------------------------------------------------- tone

    /** Picks each syllable's lexical tone. */
    private List<SylTone> assignTones(List<List<Seg>> syls, int primary) {
        List<SylTone> out = new ArrayList<>();
        if (def.tones == null) {
            syls.forEach(s -> out.add(SylTone.NONE));
            return out;
        }
        switch (def.toneBearing) {
            case SYLLABLE -> syls.forEach(s -> out.add(new SylTone(def.tones.pick(rng), List.of(), "")));
            case STRESSED -> {
                String accent = primary >= 0 ? def.tones.pick(rng) : "";
                syls.forEach(s -> out.add(new SylTone("", List.of(), accent)));
            }
            case MORA -> {
                for (List<Seg> syl : syls) {
                    List<String> ts = new ArrayList<>();
                    for (Seg seg : syl) {
                        if (seg.role() == Seg.Role.NUCLEUS) {
                            for (int m = 0; m < def.morae(seg); m++) {
                                ts.add(def.tones.pick(rng));
                            }
                        }
                    }
                    out.add(new SylTone("", ts, ""));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------ rendering

    private Word render(List<List<Seg>> syls, int[] morae, int primary, List<SylTone> tones, Compound compound) {
        int n = syls.size();
        Set<Integer> secondary = new HashSet<>();
        if (def.secondaryStress && primary >= 0) {
            for (int i = primary - 2; i >= 0; i -= 2) {
                secondary.add(i);
            }
            for (int i = primary + 2; i < n; i += 2) {
                secondary.add(i);
            }
        }
        boolean nucleusTones = def.tones != null && def.tonePosition == Definition.TonePosition.NUCLEUS;

        StringBuilder word = new StringBuilder();
        List<String> toneLabels = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            List<Seg> syl = syls.get(s);
            SylTone st = tones.get(s);
            String sm = "";
            if (n > 1 || def.markMonosyllables) {
                sm = s == primary ? def.stressMark : secondary.contains(s) ? def.secondaryMark : "";
            }
            boolean smOnNucleus = DefinitionParser.isCombining(sm);

            // This syllable's tone: one per syllable, or one list per nuclear segment.
            String sylTone = "";
            String toneLabel = "";
            List<List<String>> moraTones = new ArrayList<>();
            if (def.tones != null) {
                switch (def.toneBearing) {
                    case SYLLABLE -> {
                        sylTone = st.tone();
                        toneLabel = def.toneLabels.getOrDefault(sylTone, "");
                    }
                    case STRESSED -> {
                        if (s == primary) {
                            sylTone = st.accent();
                            toneLabel = def.toneLabels.getOrDefault(sylTone, "");
                        }
                    }
                    case MORA -> {
                        moraTones = distribute(syl, st.moraTones());
                        List<String> labels = new ArrayList<>();
                        moraTones.forEach(ts -> ts.forEach(t -> labels.add(def.toneLabels.getOrDefault(t, ""))));
                        toneLabel = String.join("+", labels);
                    }
                }
            }
            toneLabels.add(toneLabel);

            int target = syl.size() - 1;
            for (int j = 0; j < syl.size(); j++) {
                if (syl.get(j).role() == Seg.Role.NUCLEUS) {
                    target = j;
                    break;
                }
            }
            StringBuilder body = new StringBuilder();
            for (int j = 0; j < syl.size(); j++) {
                List<String> marks = new ArrayList<>();
                if (j == target) {
                    if (nucleusTones && !sylTone.isEmpty()) {
                        marks.add(sylTone);
                    }
                    if (smOnNucleus) {
                        marks.add(sm);
                    }
                }
                List<String> perMora = nucleusTones && !moraTones.isEmpty() ? moraTones.get(j) : List.of();
                body.append(decorate(syl.get(j).text(), perMora, marks));
            }
            if (def.tones != null && !nucleusTones) {
                StringBuilder t = new StringBuilder(sylTone);
                for (List<String> ts : moraTones) {
                    t.append(contour(ts));
                }
                if (def.tonePosition == Definition.TonePosition.AFTER) {
                    body.append(t);
                } else {
                    body.insert(0, t);
                }
            }

            String prefix = smOnNucleus ? "" : sm;
            if (s > 0 && prefix.isEmpty()) {
                word.append(def.syllableSeparator);
            }
            word.append(prefix).append(body);
        }
        return new Word(Normalizer.normalize(word, Normalizer.Form.NFC), syls, morae, primary, toneLabels,
                tones, compound);
    }

    /**
     * Hands a syllable's mora tones to its nuclear segments, as many as each
     * weighs. Only matters when a compound's join changed the nucleus: missing
     * tones repeat the last one, surplus tones are dropped.
     */
    private List<List<String>> distribute(List<Seg> syl, List<String> tones) {
        List<List<String>> out = new ArrayList<>();
        int next = 0;
        for (Seg seg : syl) {
            List<String> ts = new ArrayList<>();
            if (seg.role() == Seg.Role.NUCLEUS && !tones.isEmpty()) {
                for (int m = 0; m < def.morae(seg); m++, next++) {
                    ts.add(tones.get(Math.min(next, tones.size() - 1)));
                }
            }
            out.add(ts);
        }
        return out;
    }

    /**
     * Places combining marks on a segment. Mora tones go one per base character
     * when the segment has enough of them ("aa", "ai"); otherwise they stack on
     * the first as a contour. Other marks go on the first base character.
     */
    private String decorate(String text, List<String> moraMarks, List<String> marks) {
        if (moraMarks.stream().allMatch(String::isEmpty) && marks.isEmpty()) {
            return text;
        }
        List<StringBuilder> clusters = new ArrayList<>();
        text.codePoints().forEach(cp -> {
            if (clusters.isEmpty() || !Template.isMark(cp)) {
                clusters.add(new StringBuilder());
            }
            clusters.getLast().appendCodePoint(cp);
        });
        // A modifier letter such as ː is not a base that can carry its own tone.
        List<StringBuilder> bases = clusters.stream()
                .filter(c -> Character.getType(c.codePointAt(0)) != Character.MODIFIER_LETTER)
                .toList();
        if (bases.isEmpty()) {
            bases = clusters;
        }
        if (moraMarks.size() > 1 && bases.size() >= moraMarks.size()) {
            for (int i = 0; i < moraMarks.size(); i++) {
                bases.get(i).append(moraMarks.get(i));
            }
        } else {
            bases.getFirst().append(contour(moraMarks));
        }
        for (String m : marks) {
            bases.getFirst().append(m);
        }
        StringBuilder out = new StringBuilder();
        clusters.forEach(out::append);
        return out.toString();
    }

    private String contour(List<String> tones) {
        StringBuilder sb = new StringBuilder();
        for (String t : tones) {
            sb.append(t);
        }
        String joined = sb.toString();
        String mapped = def.toneContours.get(joined);
        if (mapped != null) {
            return mapped;
        }
        // A level tone over several morae is written once: á́ -> á.
        StringBuilder level = new StringBuilder();
        String prev = null;
        for (String t : tones) {
            if (!t.equals(prev)) {
                level.append(t);
            }
            prev = t;
        }
        return level.toString();
    }
}
