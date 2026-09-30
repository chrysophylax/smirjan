package smirjan;

/** Global numeric settings that other parts of a definition are built from. */
final class Settings {
    String seed;
    boolean seedGiven;
    Distribution distribution = Distribution.GUSEIN_ZADE;
    double yuleB = 0.5;
    double yuleC = 0.9;
    double zipfS = 1.0;
    /** Relative amount of seeded noise applied to every weight. */
    double jitter = 0.1;
    /** Default probability that an optional element is present. */
    double randomRate = 0.3;

    Distribution.Params distributionParams() {
        return new Distribution.Params(yuleB, yuleC, zipfS);
    }
}
