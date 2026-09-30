package smirjan;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/** Builds words from a {@link Definition}: shapes, phonotactics, then stress and tone. */
final class Generator {
    /** Consecutive failed or duplicate attempts after which we give up. */
    static final int MISS_LIMIT = 200_000;

    private final Definition def;
    private final Random rng;

    Generator(Definition def) {
        this.def = def;
        this.rng = Rng.stream(def.settings.seed, "generate");
    }

    /** Emits up to {@code count} distinct words; returns how many were emitted. */
    int generate(int count, Consumer<String> out) {
        return generateWords(count, w -> out.accept(w.text()));
    }

    /** As {@link #generate}, with each word's structure. Words are distinct by their rendered text. */
    int generateWords(int count, Consumer<Word> out) {
        Set<String> seen = new HashSet<>();
        int misses = 0;
        while (seen.size() < count && misses < MISS_LIMIT) {
            Word w = word();
            if (w == null || !seen.add(w.text())) {
                misses++;
            } else {
                out.accept(w);
                misses = 0;
            }
        }
        return seen.size();
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
     */
    record Word(String text, List<List<Seg>> syllables, int[] morae, int primary, List<String> tones) {}

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
        StringBuilder plain = new StringBuilder();
        for (Seg s : segs) {
            plain.append(s.text());
        }
        if (plain.isEmpty()) {
            return null;
        }
        for (var p : def.rejects) {
            if (p.matcher(plain).find()) {
                return null;
            }
        }

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
        int[] morae = new int[syls.size()];
        int total = 0;
        for (int i = 0; i < syls.size(); i++) {
            for (Seg s : syls.get(i)) {
                morae[i] += def.morae(s);
            }
            total += morae[i];
        }
        if (targetMorae != null && total != targetMorae) {
            return null;
        }
        return render(syls, morae);
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
        return stress(def.stress, morae, 0);
    }

    private int stress(Definition.Stress rule, int[] morae, int depth) {
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
                yield fallback(Definition.Stress.INITIAL, morae, depth);
            }
            case RIGHTMOST_HEAVY -> {
                for (int i = n - 1; i >= 0; i--) {
                    if (heavy(morae[i])) {
                        yield i;
                    }
                }
                yield fallback(Definition.Stress.FINAL, morae, depth);
            }
            case FREE -> rng.nextInt(n);
        };
    }

    private int fallback(Definition.Stress dflt, int[] morae, int depth) {
        Definition.Stress f = def.stressFallback != null && depth == 0 ? def.stressFallback : dflt;
        return stress(f, morae, depth + 1);
    }

    private boolean heavy(int morae) {
        return morae >= def.heavyMorae;
    }

    // ------------------------------------------------------------ rendering

    private Word render(List<List<Seg>> syls, int[] morae) {
        int n = syls.size();
        int primary = primaryStress(morae);
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
            String sm = "";
            if (n > 1 || def.markMonosyllables) {
                sm = s == primary ? def.stressMark : secondary.contains(s) ? def.secondaryMark : "";
            }
            boolean smOnNucleus = DefinitionParser.isCombining(sm);

            // Tones for this syllable: one per syllable, or one list per nuclear segment.
            String sylTone = "";
            String toneLabel = "";
            List<List<String>> moraTones = new ArrayList<>();
            if (def.tones != null) {
                switch (def.toneBearing) {
                    case SYLLABLE -> {
                        sylTone = def.tones.pick(rng);
                        toneLabel = def.toneLabels.get(sylTone);
                    }
                    case STRESSED -> {
                        if (s == primary) {
                            sylTone = def.tones.pick(rng);
                            toneLabel = def.toneLabels.get(sylTone);
                        }
                    }
                    case MORA -> {
                        List<String> labels = new ArrayList<>();
                        for (Seg seg : syl) {
                            List<String> ts = new ArrayList<>();
                            if (seg.role() == Seg.Role.NUCLEUS) {
                                for (int m = 0; m < def.morae(seg); m++) {
                                    String t = def.tones.pick(rng);
                                    ts.add(t);
                                    labels.add(def.toneLabels.get(t));
                                }
                            }
                            moraTones.add(ts);
                        }
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
        return new Word(Normalizer.normalize(word, Normalizer.Form.NFC), syls, morae, primary, toneLabels);
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
