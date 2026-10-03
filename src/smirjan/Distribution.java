package smirjan;

/**
 * Rank-frequency laws. Rank r runs from 1 (most frequent) to n (least).
 * Weights are relative; they need not sum to one.
 */
enum Distribution {
    /**
     * Gusein-Zade (1988): the expected r-th largest coordinate of a point drawn
     * uniformly from the simplex, F(r) = (1/r + 1/(r+1) + ... + 1/n) / n.
     * Parameter-free; depends only on the inventory size, and sums to one.
     * The paper's approximation (ln(n + 1) - ln r) / n is not used: it sums to
     * less than one (see proofs/GuseinZade.v).
     */
    GUSEIN_ZADE,
    /**
     * Yule (1924), as applied to phonemes by Tambovtsev &amp; Martindale (2007):
     * F(r) = a * r^-b * c^r. The constant a cancels out on normalisation.
     */
    YULE,
    /** Zipf: F(r) = 1 / r^s. */
    ZIPF,
    /** Every item equally likely. */
    FLAT;

    static Distribution parse(String s) {
        return switch (s.toLowerCase().replace('_', '-')) {
            case "gusein-zade", "guseinzade", "gz" -> GUSEIN_ZADE;
            case "yule" -> YULE;
            case "zipf" -> ZIPF;
            case "flat", "uniform", "none" -> FLAT;
            default -> throw new IllegalArgumentException(
                    "unknown distribution '" + s + "' (expected gusein-zade, yule, zipf or flat)");
        };
    }

    double[] weights(int n, Params p) {
        double[] w = new double[n];
        // Tail sums 1/r + ... + 1/n, accumulated from the rarest rank upwards.
        double tail = 0;
        double[] tails = new double[n];
        for (int r = n; r >= 1; r--) {
            tail += 1.0 / r;
            tails[r - 1] = tail;
        }
        for (int i = 0; i < n; i++) {
            int r = i + 1;
            w[i] = switch (this) {
                case GUSEIN_ZADE -> tails[i] / n;
                case YULE -> Math.pow(r, -p.yuleB()) * Math.pow(p.yuleC(), r);
                case ZIPF -> 1.0 / Math.pow(r, p.zipfS());
                case FLAT -> 1.0;
            };
        }
        return w;
    }

    record Params(double yuleB, double yuleC, double zipfS) {}
}
