package smirjan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A regex rewrite such as {@code ki > tʃi}, applied to the word's phonemes.
 * Syllable membership survives the rewrite: replaced text joins the syllable
 * of the first character it replaced, so stress and tone still land correctly.
 * Rewritten text is split back into known phonemes, which regain their class
 * (for mora weight) and their onset/nucleus/coda role.
 */
final class Filter {
    final String source;
    private final Pattern pattern;
    private final String replacement;
    private final List<String> phonemesLongestFirst;
    /** Each phoneme's first class in definition order. */
    private final Map<String, Character> classOf;
    private final Set<String> nuclear;

    Filter(String source, Pattern pattern, String replacement, List<String> phonemesLongestFirst,
           Map<String, Character> classOf, Set<String> nuclear) {
        this.source = source;
        this.pattern = pattern;
        this.replacement = replacement;
        this.phonemesLongestFirst = phonemesLongestFirst;
        this.classOf = classOf;
        this.nuclear = nuclear;
    }

    List<Seg> apply(List<Seg> segs) {
        return apply(segs, -1);
    }

    /**
     * With {@code boundary >= 0}, only rewrites matches that span that character
     * offset: the join of a compound, whose parts were already filtered.
     */
    List<Seg> apply(List<Seg> segs, int boundary) {
        StringBuilder plain = new StringBuilder();
        List<Integer> owner = new ArrayList<>();
        for (int i = 0; i < segs.size(); i++) {
            plain.append(segs.get(i).text());
            for (int k = 0; k < segs.get(i).text().length(); k++) {
                owner.add(i);
            }
        }
        if (segs.isEmpty()) {
            return segs;
        }
        Matcher m = pattern.matcher(plain);
        boolean any = false;
        while (!any && m.find()) {
            any = spans(m, boundary);
        }
        if (!any) {
            return segs;
        }
        m.reset();

        // Every output char is tagged with the original segment it came from (>= 0)
        // or with the replacement it belongs to (-1 - replacement number).
        StringBuilder sb = new StringBuilder();
        List<Integer> tag = new ArrayList<>();
        List<Seg> replacements = new ArrayList<>();
        int last = 0;
        while (m.find()) {
            if (!spans(m, boundary)) {
                continue; // left in place; appendReplacement copies it with the next gap
            }
            int before = sb.length();
            m.appendReplacement(sb, replacement);
            for (int j = last; j < m.start(); j++) {
                tag.add(owner.get(j));
            }
            int replLen = sb.length() - before - (m.start() - last);
            int anchor = m.start() < plain.length() ? owner.get(m.start()) : owner.get(plain.length() - 1);
            boolean nuclear = false;
            for (int j = m.start(); j < m.end(); j++) {
                nuclear |= segs.get(owner.get(j)).role() == Seg.Role.NUCLEUS;
            }
            Seg a = segs.get(anchor);
            replacements.add(new Seg("", a.syl(), nuclear ? Seg.Role.NUCLEUS : a.role(), null));
            for (int k = 0; k < replLen; k++) {
                tag.add(-replacements.size());
            }
            last = m.end();
        }
        m.appendTail(sb);
        for (int j = last; j < plain.length(); j++) {
            tag.add(owner.get(j));
        }

        List<Seg> out = new ArrayList<>();
        int i = 0;
        while (i < sb.length()) {
            int t = tag.get(i);
            int j = i;
            while (j < sb.length() && tag.get(j) == t) {
                j++;
            }
            String text = sb.substring(i, j);
            Seg o = t >= 0 ? segs.get(t) : replacements.get(-t - 1);
            if (t >= 0 && text.equals(o.text())) {
                out.add(o);
            } else {
                resegment(text, o, out);
            }
            i = j;
        }
        return out;
    }

    private static boolean spans(Matcher m, int boundary) {
        return boundary < 0 || (m.start() < boundary && m.end() > boundary);
    }

    /**
     * Splits rewritten text into known phonemes. Unknown characters join the
     * phoneme before them from the same rewrite (t + ʃ -> tʃ), and a stray length
     * mark or diacritic joins the phoneme before it (i + ː -> iː).
     */
    private void resegment(String text, Seg from, List<Seg> out) {
        int start = out.size();
        int i = 0;
        outer:
        while (i < text.length()) {
            String piece = null;
            for (String p : phonemesLongestFirst) {
                if (text.startsWith(p, i)) {
                    piece = p;
                    break;
                }
            }
            if (piece == null) {
                int end = i + Character.charCount(text.codePointAt(i));
                while (end < text.length() && Template.isMark(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end));
                }
                piece = text.substring(i, end);
                boolean attach = out.size() > start
                        || (!out.isEmpty() && out.getLast().syl() == from.syl() && isModifierOnly(piece));
                if (attach) {
                    Seg prev = out.removeLast();
                    String joined = prev.text() + piece;
                    out.add(new Seg(joined, prev.syl(), prev.role(), classOf.getOrDefault(joined, prev.cls())));
                    i = end;
                    continue outer;
                }
            }
            out.add(new Seg(piece, from.syl(), role(piece, from, out), classOf.get(piece)));
            i += piece.length();
        }
    }

    private Seg.Role role(String piece, Seg from, List<Seg> before) {
        if (nuclear.contains(piece)) {
            return Seg.Role.NUCLEUS;
        }
        for (Seg s : before) {
            if (s.syl() == from.syl() && s.role() == Seg.Role.NUCLEUS) {
                return Seg.Role.CODA;
            }
        }
        return Seg.Role.ONSET;
    }

    private static boolean isModifierOnly(String s) {
        return s.codePoints().allMatch(cp -> Template.isMark(cp)
                || Character.getType(cp) == Character.MODIFIER_LETTER);
    }
}
