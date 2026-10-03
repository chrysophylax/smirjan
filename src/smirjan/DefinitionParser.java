package smirjan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reads the {@code key: value} .def format. Keys are processed in dependency
 * order (settings, then classes, then everything that refers to classes), so
 * lines may appear in any order, except that a class can only include classes
 * defined above it.
 */
final class DefinitionParser {
    private static final Pattern KEY_LINE = Pattern.compile("^([A-Z]|[A-Za-z][A-Za-z0-9-]+)\\s*:(.*)$");
    private static final Pattern CLASS_REF = Pattern.compile("\\{([A-Z])}");

    private static final Set<String> SETTINGS = Set.of(
            "seed", "distribution", "yule-b", "yule-c", "zipf-s", "jitter", "random-rate");
    private static final Set<String> KEYS = Set.of(
            "nucleus", "syllable", "syllable-initial", "syllable-medial", "syllable-final", "syllable-mono",
            "word-syllables", "word-morae", "morae", "heavy-morae",
            "stress", "stress-fallback", "stress-mark", "secondary-stress", "secondary-stress-mark",
            "stress-monosyllables", "syllable-separator",
            "tones", "tone-bearing", "tone-position", "tone-contour",
            "filter", "reject",
            "compounds", "compound-rate", "compound-types", "compound-order", "compound-separator");
    /** Keys whose repeated lines add to each other rather than replace. */
    private static final Set<String> ACCUMULATING = Set.of(
            "syllable", "syllable-initial", "syllable-medial", "syllable-final", "syllable-mono",
            "filter", "reject", "tones", "tone-contour", "morae");

    /** Names usable wherever a combining diacritic is expected. */
    static final Map<String, String> DIACRITICS = Map.ofEntries(
            Map.entry("acute", "́"), Map.entry("grave", "̀"),
            Map.entry("macron", "̄"), Map.entry("circumflex", "̂"),
            Map.entry("caron", "̌"), Map.entry("tilde", "̃"),
            Map.entry("double-acute", "̋"), Map.entry("double-grave", "̏"),
            Map.entry("breve", "̆"), Map.entry("inverted-breve", "̑"),
            Map.entry("diaeresis", "̈"), Map.entry("ring", "̊"),
            Map.entry("dot", "̇"), Map.entry("vertical-line", "̍"));

    private record Entry(String key, String value, int line) {}

    private record Table(List<String> rows, List<Integer> lines) {}

    private DefinitionParser() {}

    static Definition parse(Path file) throws IOException, DefinitionException {
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    static Definition parse(String text) throws DefinitionException {
        List<Entry> entries = new ArrayList<>();
        List<Table> tables = new ArrayList<>();
        read(Normalizer.normalize(text, Normalizer.Form.NFC), entries, tables);

        Definition def = new Definition();
        settings(def, entries);
        classes(def, entries);
        List<String> phonemes = phonemesLongestFirst(def.classes);
        rest(def, entries, phonemes);
        for (Table t : tables) {
            ClusterTable.parse(t.rows, t.lines, def.classes, phonemes, def.clusters);
        }
        return def;
    }

    // ------------------------------------------------------------ reading

    private static void read(String text, List<Entry> entries, List<Table> tables) throws DefinitionException {
        String[] lines = text.split("\\R", -1);
        Table table = null;
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = stripComment(lines[i]).strip();
            if (line.isEmpty()) {
                // A blank line ends a cluster table; a comment-only line does not.
                if (lines[i].strip().isEmpty()) {
                    table = null;
                }
                continue;
            }
            if (line.startsWith("%")) {
                table = new Table(new ArrayList<>(), new ArrayList<>());
                tables.add(table);
                table.rows.add(line);
                table.lines.add(lineNo);
                continue;
            }
            Matcher m = KEY_LINE.matcher(line);
            if (m.matches()) {
                table = null;
                String key = m.group(1);
                if (key.length() > 1) {
                    key = key.toLowerCase();
                }
                entries.add(new Entry(key, m.group(2).strip(), lineNo));
                continue;
            }
            if (table != null) {
                table.rows.add(line);
                table.lines.add(lineNo);
                continue;
            }
            throw DefinitionException.at(lineNo, "expected 'key: value' or a cluster table starting with '%'");
        }
    }

    private static String stripComment(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '#' && (i == 0 || Character.isWhitespace(line.charAt(i - 1)))) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    // ---------------------------------------------------------- settings

    private static void settings(Definition def, List<Entry> entries) throws DefinitionException {
        Settings s = def.settings;
        for (Entry e : entries) {
            if (!SETTINGS.contains(e.key)) {
                continue;
            }
            try {
                switch (e.key) {
                    case "seed" -> {
                        s.seed = e.value;
                        s.seedGiven = true;
                    }
                    case "distribution" -> s.distribution = Distribution.parse(e.value);
                    case "yule-b" -> s.yuleB = number(e);
                    case "yule-c" -> {
                        s.yuleC = number(e);
                        if (s.yuleC <= 0) {
                            throw DefinitionException.at(e.line, "yule-c must be positive");
                        }
                    }
                    case "zipf-s" -> s.zipfS = number(e);
                    case "jitter" -> {
                        s.jitter = fraction(e);
                        if (s.jitter >= 1) {
                            throw DefinitionException.at(e.line, "jitter must be below 1 (100%)");
                        }
                    }
                    case "random-rate" -> s.randomRate = fraction(e);
                    default -> throw new AssertionError(e.key);
                }
            } catch (IllegalArgumentException ex) {
                throw DefinitionException.at(e.line, ex.getMessage());
            }
        }
        if (s.seed == null) {
            s.seed = Long.toHexString(new SecureRandom().nextLong());
        }
    }

    private static double number(Entry e) throws DefinitionException {
        try {
            return Double.parseDouble(e.value);
        } catch (NumberFormatException ex) {
            throw DefinitionException.at(e.line, e.key + ": '" + e.value + "' is not a number");
        }
    }

    /** "30%", "30" and "0.3" all mean 0.3. */
    private static double fraction(Entry e) throws DefinitionException {
        String v = e.value;
        boolean pct = v.endsWith("%");
        if (pct) {
            v = v.substring(0, v.length() - 1).strip();
        }
        double d;
        try {
            d = Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            throw DefinitionException.at(e.line, e.key + ": '" + e.value + "' is not a number or percentage");
        }
        if (pct || d > 1) {
            d /= 100;
        }
        if (d < 0 || d > 1) {
            throw DefinitionException.at(e.line, e.key + " must be between 0 and 100%");
        }
        return d;
    }

    // ------------------------------------------------------------ classes

    private static void classes(Definition def, List<Entry> entries) throws DefinitionException {
        for (Entry e : entries) {
            if (e.key.length() != 1) {
                continue;
            }
            char name = e.key.charAt(0);
            if (def.classes.containsKey(name)) {
                throw DefinitionException.at(e.line, "class " + name + " is defined twice");
            }
            List<String> items = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (String tok : tokens(e.value)) {
                Weight w = weight(tok, e);
                List<String> expanded = w.item.length() == 1 && def.classes.containsKey(w.item.charAt(0))
                        ? def.classes.get(w.item.charAt(0)).list()
                        : List.of(w.item);
                for (String p : expanded) {
                    if (!seen.add(p)) {
                        System.err.println("smirjan: warning: line " + e.line + ": '" + p
                                + "' appears twice in class " + name + "; keeping the first");
                        continue;
                    }
                    items.add(p);
                    weights.add(w.weight);
                }
            }
            if (items.isEmpty()) {
                throw DefinitionException.at(e.line, "class " + name + " is empty");
            }
            def.classes.put(name, new PhonemeClass(name,
                    Weighted.build(items, weights, def.settings, "class:" + name)));
        }
    }

    // --------------------------------------------------------------- rest

    /** Every phoneme of every class, once, longest first: the order for splitting text into phonemes. */
    private static List<String> phonemesLongestFirst(Map<Character, PhonemeClass> classes) {
        List<String> phonemes = new ArrayList<>();
        for (PhonemeClass c : classes.values()) {
            for (String p : c.list()) {
                if (!phonemes.contains(p)) {
                    phonemes.add(p);
                }
            }
        }
        phonemes.sort(Comparator.comparingInt(String::length).reversed());
        return phonemes;
    }

    private static void rest(Definition def, List<Entry> entries, List<String> phonemes) throws DefinitionException {
        Map<String, List<Entry>> byKey = new java.util.LinkedHashMap<>();
        for (Entry e : entries) {
            if (e.key.length() == 1 || SETTINGS.contains(e.key)) {
                continue;
            }
            if (!KEYS.contains(e.key)) {
                throw DefinitionException.at(e.line, "unknown key '" + e.key + "'");
            }
            if (!ACCUMULATING.contains(e.key)) {
                byKey.remove(e.key); // last one wins
            }
            byKey.computeIfAbsent(e.key, k -> new ArrayList<>()).add(e);
        }

        // Nucleus classes must be known before shapes can assign roles.
        if (byKey.containsKey("nucleus")) {
            Entry e = byKey.get("nucleus").getFirst();
            for (String tok : tokens(e.value)) {
                if (tok.length() != 1 || !def.classes.containsKey(tok.charAt(0))) {
                    throw DefinitionException.at(e.line, "nucleus: '" + tok + "' is not a defined class");
                }
                def.nuclei.add(tok.charAt(0));
            }
        } else if (def.classes.containsKey('V')) {
            def.nuclei.add('V');
        }

        Template.Context ctx = new Template.Context(def.classes, phonemes, def.nuclei, def.settings);

        def.syllable = shapes(byKey, "syllable", ctx, def.settings);
        def.syllableInitial = shapes(byKey, "syllable-initial", ctx, def.settings);
        def.syllableMedial = shapes(byKey, "syllable-medial", ctx, def.settings);
        def.syllableFinal = shapes(byKey, "syllable-final", ctx, def.settings);
        def.syllableMono = shapes(byKey, "syllable-mono", ctx, def.settings);
        if (def.syllable == null && (def.syllableInitial == null || def.syllableFinal == null
                || def.syllableMedial == null || def.syllableMono == null)) {
            throw new DefinitionException("no 'syllable:' shapes given (needed unless initial, medial, final and"
                    + " mono shapes are all given)");
        }

        def.wordSyllables = lengths(byKey, "word-syllables", def.settings);
        def.wordMorae = lengths(byKey, "word-morae", def.settings);
        if (def.wordSyllables == null && def.wordMorae == null) {
            def.wordSyllables = Weighted.build(List.of(2, 1, 3), null, def.settings, "list:word-syllables");
        }

        for (Entry e : byKey.getOrDefault("morae", List.of())) {
            morae(def, e);
        }
        Entry e = null;
        if ((e = one(byKey, "heavy-morae")) != null) {
            def.heavyMorae = (int) number(e);
        }

        try {
            if ((e = one(byKey, "stress")) != null) {
                def.stress = Definition.Stress.parse(e.value);
            }
            if ((e = one(byKey, "stress-fallback")) != null) {
                def.stressFallback = Definition.Stress.parse(e.value);
            }
            if ((e = one(byKey, "secondary-stress")) != null) {
                def.secondaryStress = switch (e.value.toLowerCase()) {
                    case "alternating", "yes", "true" -> true;
                    case "none", "no", "false" -> false;
                    default -> throw new IllegalArgumentException("secondary-stress: expected alternating or none");
                };
            }
            if ((e = one(byKey, "stress-monosyllables")) != null) {
                def.markMonosyllables = bool(e.value);
            }
            if ((e = one(byKey, "tone-bearing")) != null) {
                def.toneBearing = switch (e.value.toLowerCase()) {
                    case "syllable" -> Definition.ToneBearing.SYLLABLE;
                    case "mora" -> Definition.ToneBearing.MORA;
                    case "stressed", "pitch-accent" -> Definition.ToneBearing.STRESSED;
                    default -> throw new IllegalArgumentException("tone-bearing: expected syllable, mora or stressed");
                };
            }
            if ((e = one(byKey, "tone-position")) != null) {
                def.tonePosition = switch (e.value.toLowerCase()) {
                    case "nucleus" -> Definition.TonePosition.NUCLEUS;
                    case "after", "end" -> Definition.TonePosition.AFTER;
                    case "before", "start" -> Definition.TonePosition.BEFORE;
                    default -> throw new IllegalArgumentException("tone-position: expected nucleus, after or before");
                };
            }
        } catch (IllegalArgumentException ex) {
            throw DefinitionException.at(e.line, ex.getMessage());
        }
        if ((e = one(byKey, "stress-mark")) != null) {
            def.stressMark = mark(e.value);
        }
        if ((e = one(byKey, "secondary-stress-mark")) != null) {
            def.secondaryMark = mark(e.value);
        }
        if ((e = one(byKey, "syllable-separator")) != null) {
            def.syllableSeparator = e.value.equalsIgnoreCase("none") ? "" : e.value;
        }

        tones(def, byKey);
        compounds(def, byKey);
        Map<String, Character> classOf = new java.util.HashMap<>();
        Set<String> nuclear = new java.util.HashSet<>();
        for (PhonemeClass c : def.classes.values()) {
            for (String p : c.list()) {
                classOf.putIfAbsent(p, c.name);
                if (def.nuclei.contains(c.name)) {
                    nuclear.add(p);
                }
            }
        }
        for (Entry f : byKey.getOrDefault("filter", List.of())) {
            filters(def, f, phonemes, classOf, nuclear);
        }
        for (Entry r : byKey.getOrDefault("reject", List.of())) {
            for (String tok : tokens(r.value)) {
                def.rejects.add(regex(expandClasses(tok, def, r), r));
            }
        }
    }

    private static Entry one(Map<String, List<Entry>> byKey, String key) {
        List<Entry> l = byKey.get(key);
        return l == null ? null : l.getLast();
    }

    private static Weighted<Template> shapes(Map<String, List<Entry>> byKey, String key, Template.Context ctx,
                                             Settings s) throws DefinitionException {
        List<Entry> lines = byKey.get(key);
        if (lines == null) {
            return null;
        }
        List<Template> items = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Entry e : lines) {
            for (String tok : tokens(e.value)) {
                Weight w = weight(tok, e);
                try {
                    items.add(Template.parse(w.item, ctx, key));
                } catch (DefinitionException ex) {
                    throw DefinitionException.at(e.line, ex.getMessage());
                }
                weights.add(w.weight);
            }
        }
        if (items.isEmpty()) {
            throw DefinitionException.at(lines.getFirst().line, key + ": no shapes given");
        }
        return Weighted.build(items, weights, s, "list:" + key);
    }

    private static Weighted<Integer> lengths(Map<String, List<Entry>> byKey, String key, Settings s)
            throws DefinitionException {
        Entry e = one(byKey, key);
        if (e == null) {
            return null;
        }
        List<Integer> items = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (String tok : tokens(e.value)) {
            Weight w = weight(tok, e);
            try {
                String[] range = w.item.split("-", -1);
                int lo = Integer.parseInt(range[0]);
                int hi = range.length == 2 ? Integer.parseInt(range[1]) : lo;
                if (range.length > 2 || lo < 1 || hi < lo) {
                    throw new NumberFormatException();
                }
                for (int n = lo; n <= hi; n++) {
                    if (!items.contains(n)) {
                        items.add(n);
                        weights.add(w.weight);
                    }
                }
            } catch (NumberFormatException ex) {
                throw DefinitionException.at(e.line, key + ": '" + w.item + "' is not a positive number or range");
            }
        }
        if (items.isEmpty()) {
            throw DefinitionException.at(e.line, key + ": no lengths given");
        }
        return Weighted.build(items, weights, s, "list:" + key);
    }

    private static void morae(Definition def, Entry e) throws DefinitionException {
        for (String tok : tokens(e.value)) {
            int eq = tok.lastIndexOf('=');
            if (eq <= 0) {
                throw DefinitionException.at(e.line, "morae: expected name=number, got '" + tok + "'");
            }
            String k = tok.substring(0, eq);
            int v;
            try {
                v = Integer.parseInt(tok.substring(eq + 1));
            } catch (NumberFormatException ex) {
                throw DefinitionException.at(e.line, "morae: '" + tok.substring(eq + 1) + "' is not a whole number");
            }
            switch (k) {
                case "onset" -> def.roleMorae[0] = v;
                case "nucleus" -> def.roleMorae[1] = v;
                case "coda" -> def.roleMorae[2] = v;
                default -> {
                    if (k.length() == 1 && def.classes.containsKey(k.charAt(0))) {
                        def.classMorae.put(k.charAt(0), v);
                    } else {
                        def.phonemeMorae.put(k, v);
                    }
                }
            }
        }
    }

    private static void tones(Definition def, Map<String, List<Entry>> byKey) throws DefinitionException {
        List<Entry> lines = byKey.get("tones");
        if (lines == null) {
            return;
        }
        List<String> items = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Entry e : lines) {
            for (String tok : tokens(e.value)) {
                Weight w = weight(tok, e);
                String t = w.item.equals("-") || w.item.equals("∅") ? "" : mark(w.item);
                items.add(t);
                weights.add(w.weight);
                def.toneLabels.putIfAbsent(t, w.item);
            }
        }
        def.tones = Weighted.build(items, weights, def.settings, "tones");
        if (def.tonePosition == null) {
            boolean allCombining = items.stream().filter(t -> !t.isEmpty()).allMatch(DefinitionParser::isCombining);
            def.tonePosition = allCombining ? Definition.TonePosition.NUCLEUS : Definition.TonePosition.AFTER;
        }
        for (Entry e : byKey.getOrDefault("tone-contour", List.of())) {
            for (String tok : tokens(e.value)) {
                int eq = tok.indexOf('=');
                if (eq <= 0 || eq == tok.length() - 1) {
                    throw DefinitionException.at(e.line, "tone-contour: expected tone+tone=contour, got '" + tok + "'");
                }
                StringBuilder seq = new StringBuilder();
                for (String part : tok.substring(0, eq).split("\\+")) {
                    seq.append(mark(part));
                }
                def.toneContours.put(seq.toString(), mark(tok.substring(eq + 1)));
            }
        }
    }

    private static void compounds(Definition def, Map<String, List<Entry>> byKey) throws DefinitionException {
        Entry e;
        if ((e = one(byKey, "compounds")) != null) {
            try {
                def.compounds = bool(e.value);
            } catch (IllegalArgumentException ex) {
                throw DefinitionException.at(e.line, "compounds: " + ex.getMessage());
            }
        }
        if ((e = one(byKey, "compound-rate")) != null) {
            def.compoundRate = fraction(e);
        }
        def.compoundTypes = choices(one(byKey, "compound-types"), List.of("determinative", "dvandva"),
                Map.of("determinative", "determinative", "tatpurusha", "determinative", "dvandva", "dvandva",
                        "coordinative", "dvandva"), def.settings, "list:compound-types");
        def.compoundOrder = choices(one(byKey, "compound-order"), List.of("head-final"),
                Map.of("head-final", "head-final", "head-first", "head-first",
                        "head-initial", "head-first", "left-headed", "head-first", "right-headed", "head-final"),
                def.settings, "list:compound-order");
        if ((e = one(byKey, "compound-separator")) != null) {
            def.compoundSeparator = switch (e.value.toLowerCase()) {
                case "none", "" -> "";
                case "space" -> " ";
                case "hyphen" -> "-";
                default -> e.value;
            };
        }
    }

    /** A ranked list of fixed choices, e.g. {@code head-final*95 head-first*5}. */
    private static Weighted<String> choices(Entry e, List<String> dflt, Map<String, String> allowed, Settings s,
                                            String label) throws DefinitionException {
        if (e == null) {
            return Weighted.build(dflt, null, s, label);
        }
        List<String> items = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (String tok : tokens(e.value)) {
            Weight w = weight(tok, e);
            String v = allowed.get(w.item.toLowerCase());
            if (v == null) {
                throw DefinitionException.at(e.line, e.key + ": unknown value '" + w.item + "' (expected "
                        + String.join(", ", new java.util.TreeSet<>(allowed.values())) + ")");
            }
            if (items.contains(v)) {
                throw DefinitionException.at(e.line, e.key + ": '" + v + "' given twice");
            }
            items.add(v);
            weights.add(w.weight);
        }
        if (items.isEmpty()) {
            throw DefinitionException.at(e.line, e.key + ": nothing given");
        }
        return Weighted.build(items, weights, s, label);
    }

    private static void filters(Definition def, Entry e, List<String> phonemes, Map<String, Character> classOf,
                                Set<String> nuclear) throws DefinitionException {
        for (String rule : e.value.split(";")) {
            rule = rule.strip();
            if (rule.isEmpty()) {
                continue;
            }
            int gt = rule.indexOf('>');
            if (gt < 0) {
                throw DefinitionException.at(e.line, "filter: expected 'from > to', got '" + rule + "'");
            }
            String from = rule.substring(0, gt).strip();
            String to = rule.substring(gt + 1).strip();
            if (from.isEmpty()) {
                throw DefinitionException.at(e.line, "filter: empty pattern in '" + rule + "'");
            }
            if (to.equals("!")) {
                to = "";
            }
            def.filters.add(new Filter(rule, regex(expandClasses(from, def, e), e), to, phonemes, classOf, nuclear));
        }
    }

    /** {C} in a pattern stands for any phoneme of class C. */
    private static String expandClasses(String pattern, Definition def, Entry e) throws DefinitionException {
        Matcher m = CLASS_REF.matcher(pattern);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            PhonemeClass c = def.classes.get(m.group(1).charAt(0));
            if (c == null) {
                throw DefinitionException.at(e.line, "undefined class '" + m.group(1) + "' in pattern '" + pattern + "'");
            }
            List<String> ps = new ArrayList<>(c.list());
            ps.sort(Comparator.comparingInt(String::length).reversed());
            StringBuilder alt = new StringBuilder("(?:");
            for (int i = 0; i < ps.size(); i++) {
                alt.append(i > 0 ? "|" : "").append(Pattern.quote(ps.get(i)));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(alt.append(')').toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Pattern regex(String p, Entry e) throws DefinitionException {
        try {
            return Pattern.compile(p);
        } catch (PatternSyntaxException ex) {
            throw DefinitionException.at(e.line, e.key + ": bad pattern '" + p + "': " + ex.getDescription());
        }
    }

    // ------------------------------------------------------------ helpers

    /** Splits on whitespace, except inside [...], so C[-r l]V stays one shape. */
    private static List<String> tokens(String value) {
        List<String> out = new ArrayList<>();
        StringBuilder tok = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']' && depth > 0) {
                depth--;
            }
            if (Character.isWhitespace(c) && depth == 0) {
                if (!tok.isEmpty()) {
                    out.add(tok.toString());
                    tok.setLength(0);
                }
            } else {
                tok.append(c);
            }
        }
        if (!tok.isEmpty()) {
            out.add(tok.toString());
        }
        return out;
    }

    private record Weight(String item, Double weight) {}

    /** {@code item*3} gives an explicit weight; without one, rank decides. */
    private static Weight weight(String tok, Entry e) throws DefinitionException {
        int star = tok.lastIndexOf('*');
        if (star <= 0 || star == tok.length() - 1) {
            return new Weight(tok, null);
        }
        try {
            double w = Double.parseDouble(tok.substring(star + 1));
            if (w < 0) {
                throw new NumberFormatException();
            }
            return new Weight(tok.substring(0, star), w);
        } catch (NumberFormatException ex) {
            throw DefinitionException.at(e.line, "bad weight in '" + tok + "'");
        }
    }

    private static boolean bool(String v) {
        return switch (v.toLowerCase()) {
            case "yes", "true", "on", "1" -> true;
            case "no", "false", "off", "0" -> false;
            default -> throw new IllegalArgumentException("expected yes or no, got '" + v + "'");
        };
    }

    /** A diacritic name, "none", or the literal text. */
    static String mark(String v) {
        if (v.equalsIgnoreCase("none")) {
            return "";
        }
        String d = DIACRITICS.get(v.toLowerCase());
        return d != null ? d : v;
    }

    static boolean isCombining(String s) {
        return !s.isEmpty() && s.codePoints().allMatch(Template::isMark);
    }
}
