package com.craftstats.common.randomize;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.stats.StatAccess;
import com.craftstats.common.stats.StatCatalog;
import com.craftstats.common.stats.StatDef;
import com.craftstats.common.stats.StatSchema;
import com.craftstats.common.stats.TargetType;

import java.util.Locale;
import java.util.Random;

/**
 * Seeded randomization of every stat marked {@link StatDef#RANDOM}. Always pass the real
 * vanilla values (see VanillaStats) as the baseline: mild = within ±20%, wild = 0.25×–3×,
 * chaos = anywhere from a tenth to ten times the vanilla value.
 */
public final class RandomizeManager {

    public enum Intensity { MILD, WILD, CHAOS }

    private RandomizeManager() {}

    public static Intensity getIntensity() {
        return switch (CraftStatsConfig.get().randomizeIntensity.toLowerCase(Locale.ROOT)) {
            case "wild"  -> Intensity.WILD;
            case "chaos" -> Intensity.CHAOS;
            default      -> Intensity.MILD;
        };
    }

    public static long newSeed() {
        return new Random().nextLong() & 0xFFFFFFFFFFFFL;
    }

    public static boolean supports(TargetType type) {
        return StatCatalog.all(type).stream().anyMatch(d -> d.has(StatDef.RANDOM));
    }

    /** A randomized copy of {@code base}. Fields without a vanilla value use the stat's default. */
    public static <T> T randomize(TargetType type, T base, long seed) {
        Random rng = new Random(seed);
        Intensity intensity = getIntensity();
        T s = StatSchema.copyAny(base);
        for (StatDef def : StatCatalog.all(type)) {
            if (!def.has(StatDef.RANDOM)) continue;
            Object current = StatAccess.get(base, def.field());
            switch (def.kind()) {
                case NUMBER -> {
                    double v = current instanceof Number n ? n.doubleValue() : def.fallback();
                    if (Double.isNaN(v)) continue;
                    if (def.field().equals("hardness") && v < 0) continue; // unbreakable stays unbreakable
                    Class<?> t = StatAccess.typeOf(s, def.field());
                    double rolled = def.field().equals("lightEmission")
                            ? (intensity == Intensity.MILD ? v : rng.nextInt(16))
                            : roll(rng, intensity, v, def);
                    StatAccess.set(s, def.field(), StatAccess.coerce(t, round(rolled, StatAccess.isInteger(t))));
                }
                case TOGGLE -> {
                    if (intensity == Intensity.CHAOS) StatAccess.set(s, def.field(), rng.nextInt(4) == 0);
                }
                default -> {}
            }
        }
        StatAccess.sanitize(type, s);
        return s;
    }

    private static double roll(Random rng, Intensity intensity, double base, StatDef def) {
        double v = switch (intensity) {
            case MILD  -> base * (0.8 + rng.nextDouble() * 0.4);
            case WILD  -> base * Math.exp(Math.log(0.25) + rng.nextDouble() * (Math.log(3) - Math.log(0.25)));
            case CHAOS -> base == 0
                    ? def.min() + rng.nextDouble() * Math.min(10, def.max() - def.min())
                    : base * Math.exp(Math.log(0.1) + rng.nextDouble() * (Math.log(10) - Math.log(0.1)));
        };
        return Math.max(def.min(), Math.min(def.max(), v));
    }

    private static double round(double v, boolean integer) {
        return integer ? Math.round(v) : Math.round(v * 1000.0) / 1000.0;
    }
}
