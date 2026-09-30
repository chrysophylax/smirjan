package smirjan;

/**
 * One segment of a word under construction.
 *
 * @param text the phoneme as written
 * @param syl  index of the syllable it belongs to
 * @param role its position within that syllable
 * @param cls  the class it was drawn from, or null for literals and substitutions
 */
record Seg(String text, int syl, Role role, Character cls) {
    enum Role { ONSET, NUCLEUS, CODA }
}
