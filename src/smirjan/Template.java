package smirjan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A syllable shape such as {@code C[-r]VC1C2?} or {@code (C)V(N)}.
 *
 * <pre>
 * shape    := element*
 * element  := atom postfix*
 * atom     := CLASS | literal | '(' shape ')'      parentheses make the group optional
 * postfix  := digits                               numbered slot: same number = same phoneme,
 *                                                  different numbers = distinct phonemes
 *           | '[' ('-' | '+') items ']'            exclude (-) or restrict to (+) phonemes/classes
 *           | '?' digits?                          optional, with an optional percentage
 * </pre>
 *
 * Numbered slots are scoped to one syllable. Roles (onset, nucleus, coda) are
 * derived from where the nucleus classes sit in the shape.
 */
final class Template {
    final String source;
    private final List<Node> nodes;

    private Template(String source, List<Node> nodes) {
        this.source = source;
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return source;
    }

    /** Appends one syllable's segments to {@code out}; false if the shape cannot be satisfied. */
    boolean generate(Random r, int syl, List<Seg> out) {
        return generate(nodes, r, syl, new java.util.LinkedHashMap<>(), out);
    }

    private static boolean generate(List<Node> nodes, Random r, int syl, Map<String, String> bound, List<Seg> out) {
        for (Node n : nodes) {
            if (n.optional && r.nextDouble() >= n.probability) {
                continue;
            }
            switch (n) {
                case Group g -> {
                    if (!generate(g.children, r, syl, bound, out)) {
                        return false;
                    }
                }
                case Literal l -> out.add(new Seg(l.text, syl, l.role, null));
                case Slot s -> {
                    String p;
                    String key = s.index > 0 ? s.cls.name + "" + s.index : null;
                    if (key != null && bound.containsKey(key)) {
                        p = bound.get(key);
                        if (!s.allows(p)) {
                            return false;
                        }
                    } else {
                        Set<String> taken = key == null ? Set.of() : new HashSet<>(bound.values());
                        p = s.cls.phonemes.pick(r, x -> s.allows(x) && !taken.contains(x));
                        if (p == null) {
                            return false;
                        }
                        if (key != null) {
                            bound.put(key, p);
                        }
                    }
                    out.add(new Seg(p, syl, s.role, s.cls.name));
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- nodes

    private abstract static sealed class Node permits Group, Leaf {
        boolean optional;
        Double explicitRate;
        double probability = 1.0;
    }

    private static final class Group extends Node {
        final List<Node> children;

        Group(List<Node> children) {
            this.children = children;
        }
    }

    private abstract static sealed class Leaf extends Node permits Literal, Slot {
        Seg.Role role = Seg.Role.ONSET;

        abstract boolean isNuclear(Context ctx);
    }

    private static final class Literal extends Leaf {
        final String text;

        Literal(String text) {
            this.text = text;
        }

        @Override
        boolean isNuclear(Context ctx) {
            return ctx.nuclei.stream().anyMatch(c -> ctx.classes.get(c).members.contains(text));
        }
    }

    private static final class Slot extends Leaf {
        final PhonemeClass cls;
        int index;
        Set<String> include;
        final Set<String> exclude = new HashSet<>();

        Slot(PhonemeClass cls) {
            this.cls = cls;
        }

        boolean allows(String p) {
            return (include == null || include.contains(p)) && !exclude.contains(p);
        }

        @Override
        boolean isNuclear(Context ctx) {
            return ctx.nuclei.contains(cls.name);
        }
    }

    // --------------------------------------------------------------- parsing

    /** What a shape may refer to. */
    record Context(Map<Character, PhonemeClass> classes, List<String> phonemesLongestFirst,
                   Set<Character> nuclei, Settings settings) {}

    static Template parse(String src, Context ctx, String label) throws DefinitionException {
        Parser p = new Parser(src, ctx);
        List<Node> nodes = p.sequence(false);
        if (p.pos < src.length()) {
            throw p.error("unbalanced ')'");
        }
        List<Leaf> leaves = new ArrayList<>();
        List<Node> optionals = new ArrayList<>();
        flatten(nodes, leaves, optionals);
        assignRoles(leaves, ctx);
        for (int i = 0; i < optionals.size(); i++) {
            Node n = optionals.get(i);
            if (n.explicitRate != null) {
                n.probability = n.explicitRate;
            } else {
                // Each optional element gets its own seeded chance of existence.
                Random r = Rng.stream(ctx.settings.seed, "optional:" + label + ":" + src + ":" + i);
                double rate = ctx.settings.randomRate * Rng.jitter(r, ctx.settings.jitter);
                n.probability = Math.clamp(rate, 0.0, 1.0);
            }
        }
        return new Template(src, nodes);
    }

    private static void flatten(List<Node> nodes, List<Leaf> leaves, List<Node> optionals) {
        for (Node n : nodes) {
            if (n.optional) {
                optionals.add(n);
            }
            if (n instanceof Group g) {
                flatten(g.children, leaves, optionals);
            } else {
                leaves.add((Leaf) n);
            }
        }
    }

    private static void assignRoles(List<Leaf> leaves, Context ctx) {
        int first = -1;
        int last = -1;
        for (int i = 0; i < leaves.size(); i++) {
            if (leaves.get(i).isNuclear(ctx)) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        for (int i = 0; i < leaves.size(); i++) {
            Leaf l = leaves.get(i);
            if (first < 0 || i < first) {
                l.role = Seg.Role.ONSET;
            } else if (i > last) {
                l.role = Seg.Role.CODA;
            } else {
                l.role = l.isNuclear(ctx) ? Seg.Role.NUCLEUS : Seg.Role.ONSET;
            }
        }
    }

    private static final class Parser {
        final String src;
        final Context ctx;
        int pos;

        Parser(String src, Context ctx) {
            this.src = src;
            this.ctx = ctx;
        }

        DefinitionException error(String msg) {
            return new DefinitionException("in shape '" + src + "' at position " + (pos + 1) + ": " + msg);
        }

        List<Node> sequence(boolean inGroup) throws DefinitionException {
            List<Node> out = new ArrayList<>();
            while (pos < src.length()) {
                int c = src.codePointAt(pos);
                if (c == ')') {
                    if (!inGroup) {
                        throw error("unbalanced ')'");
                    }
                    return out;
                }
                Node n;
                if (c == '(') {
                    pos++;
                    List<Node> children = sequence(true);
                    if (pos >= src.length()) {
                        throw error("missing ')'");
                    }
                    pos++;
                    if (children.isEmpty()) {
                        throw error("empty group '()'");
                    }
                    n = new Group(children);
                    n.optional = true;
                } else if (c >= 'A' && c <= 'Z') {
                    PhonemeClass cls = ctx.classes.get((char) c);
                    if (cls == null) {
                        throw error("undefined class '" + (char) c + "'");
                    }
                    pos++;
                    n = new Slot(cls);
                } else if ("[]?*".indexOf(c) >= 0 || Character.isDigit(c)) {
                    throw error("unexpected '" + Character.toString(c) + "'");
                } else {
                    n = new Literal(literal());
                }
                postfix(n);
                out.add(n);
            }
            if (inGroup) {
                throw error("missing ')'");
            }
            return out;
        }

        private String literal() {
            for (String p : ctx.phonemesLongestFirst) {
                if (src.startsWith(p, pos)) {
                    pos += p.length();
                    return p;
                }
            }
            int start = pos;
            pos += Character.charCount(src.codePointAt(pos));
            while (pos < src.length() && isMark(src.codePointAt(pos))) {
                pos += Character.charCount(src.codePointAt(pos));
            }
            return src.substring(start, pos);
        }

        private void postfix(Node n) throws DefinitionException {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c)) {
                    if (!(n instanceof Slot s)) {
                        throw error("only class slots can be numbered");
                    }
                    if (s.index != 0) {
                        throw error("slot numbered twice");
                    }
                    s.index = digits();
                    if (s.index == 0) {
                        throw error("slot numbers start at 1");
                    }
                } else if (c == '[') {
                    if (!(n instanceof Slot s)) {
                        throw error("only class slots can have exceptions");
                    }
                    int close = src.indexOf(']', pos);
                    if (close < 0) {
                        throw error("missing ']'");
                    }
                    exceptions(s, src.substring(pos + 1, close));
                    pos = close + 1;
                } else if (c == '?') {
                    pos++;
                    n.optional = true;
                    if (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                        int pct = digits();
                        if (pct > 100) {
                            throw error("optional percentage above 100");
                        }
                        n.explicitRate = pct / 100.0;
                    }
                } else {
                    return;
                }
            }
        }

        private int digits() {
            int start = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
            return Integer.parseInt(src.substring(start, pos));
        }

        private void exceptions(Slot s, String body) throws DefinitionException {
            body = body.strip();
            if (body.isEmpty() || (body.charAt(0) != '-' && body.charAt(0) != '+')) {
                throw error("exceptions must start with '-' (exclude) or '+' (only these), e.g. C[-r]");
            }
            boolean including = false;
            StringBuilder tok = new StringBuilder();
            for (int i = 0; i <= body.length(); i++) {
                char c = i < body.length() ? body.charAt(i) : ' ';
                if (c == '-' || c == '+' || c == ',' || Character.isWhitespace(c)) {
                    if (!tok.isEmpty()) {
                        for (String p : resolve(s, tok.toString())) {
                            if (including) {
                                if (s.include == null) {
                                    s.include = new HashSet<>();
                                }
                                s.include.add(p);
                            } else {
                                s.exclude.add(p);
                            }
                        }
                        tok.setLength(0);
                    }
                    if (c == '-') {
                        including = false;
                    } else if (c == '+') {
                        including = true;
                    }
                } else {
                    tok.append(c);
                }
            }
        }

        /** An exception item is a phoneme of the slot's class, or a whole class. */
        private List<String> resolve(Slot s, String tok) throws DefinitionException {
            if (s.cls.members.contains(tok)) {
                return List.of(tok);
            }
            if (tok.length() == 1 && ctx.classes.containsKey(tok.charAt(0))) {
                return ctx.classes.get(tok.charAt(0)).list();
            }
            // Allow run-together items such as [-rl] when they tokenise unambiguously.
            List<String> parts = new ArrayList<>();
            int i = 0;
            outer:
            while (i < tok.length()) {
                for (String p : ctx.phonemesLongestFirst) {
                    if (s.cls.members.contains(p) && tok.startsWith(p, i)) {
                        parts.add(p);
                        i += p.length();
                        continue outer;
                    }
                }
                throw error("'" + tok + "' is not a phoneme of class " + s.cls.name + " nor a class");
            }
            return parts;
        }
    }

    static boolean isMark(int cp) {
        int t = Character.getType(cp);
        return t == Character.NON_SPACING_MARK || t == Character.ENCLOSING_MARK
                || t == Character.COMBINING_SPACING_MARK;
    }
}
