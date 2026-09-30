package smirjan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Dependency-free tests; run with {@code ./build.sh test}. */
public final class SelfTest {
    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        distributions();
        determinism();
        numberedSlots();
        exceptions();
        optionals();
        clusterTables();
        filtersAndRejects();
        morae();
        stress();
        tones();
        uniqueness();
        tsv();
        filterResegments();
        meanings();
        errors();

        System.out.println(passed + " passed, " + failures.size() + " failed");
        failures.forEach(f -> System.out.println("FAIL: " + f));
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ tests

    static void distributions() {
        // Values from https://lachi-lochu.conlang.org/conlang/gusein-zade.html for 7 phonemes.
        double[] expected = {29.71, 19.80, 14.01, 9.90, 6.71, 4.11, 1.91};
        double[] gz = Distribution.GUSEIN_ZADE.weights(7, null);
        for (int i = 0; i < 7; i++) {
            check(Math.abs(gz[i] * 100 - expected[i]) < 0.01, "gusein-zade rank " + (i + 1) + " = " + gz[i]);
        }
        double[] y = Distribution.YULE.weights(5, new Distribution.Params(0.5, 0.9, 1));
        check(Math.abs(y[2] - Math.pow(3, -0.5) * Math.pow(0.9, 3)) < 1e-12, "yule r^-b c^r");
        for (int i = 1; i < 5; i++) {
            check(y[i] < y[i - 1], "yule decreasing");
        }
    }

    static void determinism() throws Exception {
        String d = "seed: gobbledygook\nC: p t k s m n\nV: a i u\nsyllable: CV CVC\n";
        check(words(d, 50).equals(words(d, 50)), "same seed, same words");
        check(!words(d, 50).equals(words(d.replace("gobbledygook", "other"), 50)), "different seed, different words");
        // Frequency ordering survives jitter: the first phoneme beats the last.
        List<String> ws = words("seed: x\nC: p t k s m n\nV: a\nsyllable: CV\nword-syllables: 1\n"
                + "jitter: 0\n", 6);
        check(ws.size() == 6, "all six CV words");
        String many = String.join("", words("seed: x\nC: p t k s m n\nV: a\nsyllable: CV\nword-syllables: 6\n", 400));
        check(count(many, 'p') > count(many, 'n') * 2, "rank 1 much more frequent than rank 6");
    }

    static void numberedSlots() throws Exception {
        String base = "seed: s\nC: p t k\nV: a\nword-syllables: 1\n";
        all(words(base + "syllable: C1VC2\n", 6), w -> w.charAt(0) != w.charAt(2), "C1VC2 distinct");
        List<String> gem = words(base + "syllable: VC1C1\n", 3);
        all(gem, w -> w.charAt(1) == w.charAt(2), "VC1C1 geminate");
        check(gem.size() == 3, "VC1C1 has exactly three words");
        all(words(base + "syllable: C1C2C3V\n", 6), w -> w.chars().distinct().count() == 4, "C1C2C3 all distinct");
    }

    static void exceptions() throws Exception {
        String base = "seed: s\nC: p t k r\nN: n m\nV: a\nword-syllables: 1\n";
        all(words(base + "syllable: C[-r]V\n", 10), w -> !w.contains("r"), "C[-r]");
        check(words(base + "syllable: C[-r]V\n", 10).size() == 3, "C[-r] leaves three");
        check(words(base + "syllable: C[-r,k]V\n", 10).size() == 2, "C[-r,k]");
        check(words(base + "syllable: C[-rk]V\n", 10).size() == 2, "C[-rk] tokenised");
        check(words(base + "syllable: C[+p t]V\n", 10).size() == 2, "C[+p t]");
        String nasal = "seed: s\nN: n m\nC: p N\nV: a\nword-syllables: 1\nsyllable: C[-N]V\n";
        check(words(nasal, 10).equals(List.of("pa")), "C[-N] excludes a class");
        List<String> ex = words(base + "syllable: C[-r]1VC1\n", 10);
        all(ex, w -> w.charAt(0) == w.charAt(2) && w.charAt(0) != 'r', "C[-r]1VC1");
    }

    static void optionals() throws Exception {
        String base = "seed: s\nC: p\nV: a\nword-syllables: 1\n";
        check(words(base + "syllable: CVC?0\n", 5).equals(List.of("pa")), "?0 never");
        check(words(base + "syllable: CVC?100\n", 5).equals(List.of("pap")), "?100 always");
        List<String> both = sorted(words(base + "syllable: CVC?\n", 5));
        check(both.equals(List.of("pa", "pap")), "C? sometimes: " + both);
        check(sorted(words(base + "syllable: (C)V(C)\n", 5)).equals(List.of("a", "ap", "pa", "pap")), "(C)V(C)");
    }

    static void clusterTables() throws Exception {
        String base = "seed: s\nC: n t k\nV: a\nsyllable: CVC\nword-syllables: 2\n";
        List<String> ws = words(base + "% t k\nn - ŋk\n", 200);
        all(ws, w -> !w.contains("nt") && !w.contains("nk"), "cluster table rejects and substitutes");
        check(ws.stream().anyMatch(w -> w.contains("ŋk")), "cluster table substitution happens");
        List<String> cls = words("seed: s\nN: n m\nT: t k\nV: a\nsyllable: VN TV\nword-syllables: 2\n"
                + "% T\nN -\n", 50);
        all(cls, w -> !w.matches(".*[nm][tk].*"), "class labels in cluster tables");
    }

    static void filtersAndRejects() throws Exception {
        String base = "seed: s\nC: t k\nV: a i\nsyllable: CV\nword-syllables: 2\n";
        List<String> ws = words(base + "filter: ti > tʃi\n", 20);
        all(ws, w -> !w.contains("ti") || w.contains("tʃi"), "filter");
        check(ws.stream().anyMatch(w -> w.contains("tʃi")), "filter applied");
        all(words(base + "reject: ^{C}i\n", 20), w -> !w.matches("^[tk]i.*"), "reject with {C}");
        all(words(base + "reject: ^{C}i\n", 20), w -> !w.isEmpty(), "reject leaves words");
    }

    static void morae() throws Exception {
        String d = "seed: s\nC: p t k\nV: a i\nA: aː\nnucleus: V A\nmorae: A=2\n"
                + "syllable: CV CVC CA\nword-morae: 3\n";
        for (String w : words(d, 100)) {
            int m = 0;
            for (char c : w.toCharArray()) {
                if (c == 'a' || c == 'i') {
                    m++;
                }
                if (c == 'ː') {
                    m++;
                }
            }
            // codas are the consonants followed by a consonant or the end
            for (int i = 0; i < w.length(); i++) {
                boolean cons = "ptk".indexOf(w.charAt(i)) >= 0;
                if (cons && (i == w.length() - 1 || "ptk".indexOf(w.charAt(i + 1)) >= 0)) {
                    m++;
                }
            }
            check(m == 3, "three morae in " + w);
        }
    }

    static void stress() throws Exception {
        String base = "seed: s\nC: p\nV: a\nsyllable: CV\n";
        check(primary(base + "stress: initial", 1, 1, 1) == 0, "initial");
        check(primary(base + "stress: final", 1, 1, 1) == 2, "final");
        check(primary(base + "stress: penult", 1, 1, 1) == 1, "penult");
        check(primary(base + "stress: antepenult", 1, 1, 1, 1) == 1, "antepenult");
        check(primary(base + "stress: antepenult", 1, 1) == 0, "antepenult in two syllables");
        check(primary(base + "stress: latin", 1, 2, 1) == 1, "latin heavy penult");
        check(primary(base + "stress: latin", 1, 1, 1, 1) == 1, "latin light penult");
        check(primary(base + "stress: heavy-final", 1, 1, 2) == 2, "heavy final");
        check(primary(base + "stress: heavy-final", 1, 1, 1) == 1, "light final");
        check(primary(base + "stress: leftmost-heavy", 1, 2, 2) == 1, "leftmost heavy");
        check(primary(base + "stress: leftmost-heavy", 1, 1, 1) == 0, "leftmost heavy fallback");
        check(primary(base + "stress: rightmost-heavy", 2, 2, 1) == 1, "rightmost heavy");
        check(primary(base + "stress: rightmost-heavy\nstress-fallback: penult", 1, 1, 1) == 1,
                "rightmost heavy custom fallback");
        check(primary(base + "stress: none", 1, 1) == -1, "none");

        List<String> ws = words(base + "stress: penult\nword-syllables: 3\nsyllable-separator: .\n", 1);
        check(ws.equals(List.of("paˈpa.pa")), "stress mark and separator: " + ws);
        ws = words(base + "stress: initial\nstress-mark: acute\nword-syllables: 2\n", 1);
        check(ws.equals(List.of("pápa")), "acute stress: " + ws);
        ws = words(base + "stress: initial\nsecondary-stress: alternating\nword-syllables: 5\n", 1);
        check(ws.equals(List.of("ˈpapaˌpapaˌpa")), "secondary stress: " + ws);
        check(words(base + "stress: initial\nword-syllables: 1\n", 1).equals(List.of("pa")), "monosyllable unmarked");
    }

    static void tones() throws Exception {
        String base = "seed: s\nC: p\nV: a\nsyllable: CV\nword-syllables: 1\n";
        check(sorted(words(base + "tones: acute grave\n", 5)).equals(List.of("pà", "pá")), "syllable tones");
        check(sorted(words(base + "tones: 1 2\n", 5)).equals(List.of("pa1", "pa2")), "tone numbers after");
        check(sorted(words(base + "tones: ˥ ˩\ntone-position: before\n", 5)).equals(List.of("˥pa", "˩pa")),
                "tone letters before");
        String mora = "seed: s\nC: p\nA: aː\nnucleus: A\nmorae: A=2\nsyllable: CA\nword-syllables: 1\n"
                + "tones: acute grave\ntone-bearing: mora\ntone-contour: acute+grave=circumflex grave+acute=caron\n";
        check(sorted(words(mora, 10)).equals(List.of("pàː", "páː", "pâː", "pǎː")), "mora tones with contours: "
                + sorted(words(mora, 10)));
        String split = "seed: s\nC: p\nA: aa\nnucleus: A\nmorae: A=2\nsyllable: CA\nword-syllables: 1\n"
                + "tones: acute grave\ntone-bearing: mora\n";
        check(sorted(words(split, 10)).equals(List.of("pàà", "pàá", "páà", "páá")), "mora tones per vowel");
        String accent = "seed: s\nC: p\nV: a\nsyllable: CV\nword-syllables: 2\nstress: initial\nstress-mark: none\n"
                + "tones: acute\ntone-bearing: stressed\n";
        check(words(accent, 5).equals(List.of("pápa")), "pitch accent");
    }

    static void uniqueness() throws Exception {
        Definition def = DefinitionParser.parse("seed: s\nC: p t\nV: a\nsyllable: CV\nword-syllables: 1\n");
        List<String> out = new ArrayList<>();
        int made = new Generator(def).generate(10, out::add);
        check(made == 2 && out.size() == 2 && !out.get(0).equals(out.get(1)), "stops at the two possible words");
        List<String> big = words("seed: s\nC: p t k s m n l r\nV: a i u e o\nsyllable: CV CVC\n", 3000);
        check(big.size() == new java.util.HashSet<>(big).size(), "no duplicates in 3000");
    }

    static void tsv() throws Exception {
        Definition def = DefinitionParser.parse("seed: s\nC: p\nV: a\nA: aː\nnucleus: V A\nmorae: A=2\n"
                + "syllable-initial: CV\nsyllable-final: CAC\nsyllable: CV\nword-syllables: 2\n"
                + "stress: heavy-final\nsyllable-separator: .\ntones: 1\n");
        List<String> rows = new ArrayList<>();
        new Generator(def).generateWords(1, w -> rows.add(Tsv.row(w, def.tones != null)));
        check(rows.equals(List.of("pa1ˈpaːp1\tp a p aː p\tpa.paːp\t2\t4\t1.3\t2\t1.1")), "tsv row: " + rows);
        check(Tsv.HEADER.split("\t").length == rows.getFirst().split("\t", -1).length, "tsv column count");

        Definition plain = DefinitionParser.parse("seed: s\nC: p\nV: a\nsyllable: CV\nword-syllables: 1\n");
        rows.clear();
        new Generator(plain).generateWords(1, w -> rows.add(Tsv.row(w, false)));
        check(rows.equals(List.of("pa\tp a\tpa\t1\t1\t1\t\t")), "tsv without stress or tones: " + rows);

        Definition mora = DefinitionParser.parse("seed: s\nC: p\nA: aa\nnucleus: A\nmorae: A=2\n"
                + "syllable: CA\nword-syllables: 1\ntones: - acute\ntone-bearing: mora\njitter: 0\n");
        rows.clear();
        new Generator(mora).generateWords(4, w -> rows.add(Tsv.row(w, true).split("\t")[7]));
        check(sorted(rows).equals(List.of("-+-", "-+acute", "acute+-", "acute+acute")), "tsv mora tone labels: " + rows);
    }

    static void filterResegments() throws Exception {
        Definition def = DefinitionParser.parse("seed: s\nC: t\nV: i\nA: iː\nnucleus: V A\nmorae: A=2\n"
                + "syllable: CA\nword-syllables: 1\ntones: acute\nfilter: ti > tʃi\n");
        List<String> rows = new ArrayList<>();
        new Generator(def).generateWords(1, w -> rows.add(Tsv.row(w, true)));
        check(rows.equals(List.of("tʃíː\ttʃ iː\ttʃiː\t1\t2\t2\t\tacute")), "filter output resegmented: " + rows);
    }

    static void meanings() throws Exception {
        check(Meanings.LPJ.load().size() == 100, "leipzig-jakarta has 100");
        check(Meanings.DLG.load().size() == 15, "dolgopolsky has 15");
        check(Meanings.WLT.load().size() == 1460, "wold has 1460");
        check(Meanings.LPJ.load().getFirst().gloss().equals("fire"), "lpj starts with fire");
        check(Meanings.parse("dolgopolsky") == Meanings.DLG, "list aliases");

        List<String> ws = words("seed: s\nC: p t k s m n l r\nV: a i u e o\nsyllable: CV CVC\n", 40);
        var a = Meanings.LPJ.assign(ws, "s");
        check(a.equals(Meanings.LPJ.assign(ws, "s")), "assignment is replicable");
        check(!a.equals(Meanings.LPJ.assign(ws, "other")), "assignment depends on the seed");
        check(a.stream().map(x -> x.meaning().number()).distinct().count() == 40, "distinct meanings");
        check(a.stream().map(Meanings.Assigned::word).sorted().toList().equals(ws.stream().sorted().toList()),
                "every word assigned once");
        for (int i = 1; i < a.size(); i++) {
            check(a.get(i - 1).meaning().number() < a.get(i).meaning().number(), "output in list order");
        }
        var over = Meanings.DLG.assign(ws, "s");
        check(over.stream().filter(x -> x.meaning() != null).count() == 15, "15 dolgopolsky meanings used");
        check(over.subList(15, 40).stream().allMatch(x -> x.meaning() == null), "extra words unassigned, last");
    }

    static void errors() {
        error("C: p\nV: a\nsyllable: CV\nsylable: CV\n", "line 4: unknown key 'sylable'");
        error("C: p\nV: a\nsyllable: CXV\n", "undefined class 'X'");
        error("C: p\nV: a\nsyllable: C[-z]V\n", "'z' is not a phoneme of class C");
        error("C: p\nV: a\n", "no 'syllable:' shapes");
        error("C: p\nV: a\nsyllable: CV\ndistribution: normal\n", "unknown distribution");
        error("C: p\nV: a\nsyllable: CV\n% p\na + +\n", "row has 2 cells");
        error("C: p\nV: a\nsyllable: (CV\n", "missing ')'");
    }

    // ---------------------------------------------------------------- helpers

    static List<String> words(String def, int n) throws DefinitionException {
        List<String> out = new ArrayList<>();
        new Generator(DefinitionParser.parse(def)).generate(n, out::add);
        return out;
    }

    static int primary(String def, int... morae) throws DefinitionException {
        return new Generator(DefinitionParser.parse(def)).primaryStress(morae);
    }

    static void error(String def, String fragment) {
        try {
            DefinitionParser.parse(def);
            check(false, "expected error containing '" + fragment + "'");
        } catch (DefinitionException e) {
            check(e.getMessage().contains(fragment), "error '" + e.getMessage() + "' should contain '" + fragment + "'");
        }
    }

    static List<String> sorted(List<String> l) {
        return l.stream().sorted().toList();
    }

    static long count(String s, char c) {
        return s.chars().filter(x -> x == c).count();
    }

    static void all(List<String> ws, Predicate<String> p, String what) {
        check(!ws.isEmpty(), what + ": no words generated");
        for (String w : ws) {
            check(p.test(w), what + ": " + w);
        }
    }

    static void check(boolean ok, String what) {
        if (ok) {
            passed++;
        } else {
            failures.add(what);
        }
    }
}
