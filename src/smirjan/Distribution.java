package smirjan;

/**
 * Rank-frequency laws. Rank r runs from 1 (most frequent) to n (least).
 * Weights are relative; they need not sum to one.
 */
enum Distribution {
    /**
     * Borodovsky &amp; Gusein-Zade (1989): F(r) = (ln(n + 1) - ln r) / n.
     * Parameter-free; depends only on the inventory size.
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
        for (int i = 0; i < n; i++) {
            int r = i + 1;
            w[i] = switch (this) {
                case GUSEIN_ZADE -> (Math.log(n + 1) - Math.log(r)) / n;
                case YULE -> Math.pow(r, -p.yuleB()) * Math.pow(p.yuleC(), r);
                case ZIPF -> 1.0 / Math.pow(r, p.zipfS());
                case FLAT -> 1.0;
            };
        }
        return w;
    }

    record Params(double yuleB, double yuleC, double zipfS) {}
}
