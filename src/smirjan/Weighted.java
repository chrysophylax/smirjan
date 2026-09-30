package smirjan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** An ordered list of items with (jittered) rank-derived or explicit weights. */
final class Weighted<T> {
    private final List<T> items;
    private final double[] weights;

    private Weighted(List<T> items, double[] weights) {
        this.items = List.copyOf(items);
        this.weights = weights;
    }

    /**
     * @param explicit per-item explicit weights, or null entries where none was given.
     *                 If any item has an explicit weight the list is weighted
     *                 explicitly (missing weights default to 1); otherwise the
     *                 distribution is applied by rank.
     */
    static <T> Weighted<T> build(List<T> items, List<Double> explicit, Settings s, String label) {
        int n = items.size();
        double[] w;
        boolean anyExplicit = explicit != null && explicit.stream().anyMatch(d -> d != null);
        if (anyExplicit) {
            w = new double[n];
            for (int i = 0; i < n; i++) {
                Double d = explicit.get(i);
                w[i] = d == null ? 1.0 : d;
            }
        } else {
            w = s.distribution.weights(n, s.distributionParams());
        }
        if (s.jitter > 0) {
            Random r = Rng.stream(s.seed, "weights:" + label);
            for (int i = 0; i < n; i++) {
                w[i] = Math.max(w[i] * Rng.jitter(r, s.jitter), 0.0);
            }
        }
        return new Weighted<>(items, w);
    }

    static <T> Weighted<T> uniform(List<T> items) {
        double[] w = new double[items.size()];
        java.util.Arrays.fill(w, 1.0);
        return new Weighted<>(items, w);
    }

    List<T> items() {
        return items;
    }

    double weight(int i) {
        return weights[i];
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    T pick(Random r) {
        return pick(r, t -> true);
    }

    /** Picks among the items accepted by {@code allowed}, keeping their relative weights; null if none. */
    T pick(Random r, Predicate<? super T> allowed) {
        double total = 0;
        for (int i = 0; i < items.size(); i++) {
            if (allowed.test(items.get(i))) {
                total += weights[i];
            }
        }
        if (total <= 0) {
            return null;
        }
        double x = r.nextDouble() * total;
        T last = null;
        for (int i = 0; i < items.size(); i++) {
            T t = items.get(i);
            if (!allowed.test(t)) {
                continue;
            }
            last = t;
            x -= weights[i];
            if (x < 0) {
                return t;
            }
        }
        return last;
    }

    /** Normalised probabilities, for diagnostics and tests. */
    List<Double> probabilities() {
        double total = 0;
        for (double w : weights) {
            total += w;
        }
        List<Double> out = new ArrayList<>();
        for (double w : weights) {
            out.add(total == 0 ? 0 : w / total);
        }
        return Collections.unmodifiableList(out);
    }
}
