package smirjan;

import java.util.ArrayList;
import java.util.List;

/** Splitting text into the phonemes a definition knows. */
final class Phonemes {
    private Phonemes() {}

    /** A piece of text: a defined phoneme, or one unknown character with any combining marks after it. */
    record Token(String text, boolean known) {}

    /** Splits {@code text}, matching the longest defined phoneme first. Callers decide where unknown pieces go. */
    static List<Token> tokens(String text, List<String> longestFirst) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        outer:
        while (i < text.length()) {
            for (String p : longestFirst) {
                if (text.startsWith(p, i)) {
                    out.add(new Token(p, true));
                    i += p.length();
                    continue outer;
                }
            }
            int end = i + Character.charCount(text.codePointAt(i));
            while (end < text.length() && Template.isMark(text.codePointAt(end))) {
                end += Character.charCount(text.codePointAt(end));
            }
            out.add(new Token(text.substring(i, end), false));
            i = end;
        }
        return out;
    }
}
