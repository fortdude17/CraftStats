package com.craftstats.common.randomize;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;

import java.util.Locale;
import java.util.Random;

/**
 * Seeded randomization. Always pass the real vanilla values (see VanillaStats) as the
 * baseline: mild = within ±20%, wild = 0.25×–3×, chaos = anywhere between the bounds.
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

    public static MobStats randomizeMob(MobStats base, long seed) {
        Rolls r = new Rolls(seed);
        MobStats s = base.copy();
        s.maxHealth       = r.num(base.maxHealth, 1, 1024);
        s.attackDamage    = r.num(base.attackDamage, 0, 100);
        s.armor           = r.num(base.armor, 0, 30);
        s.knockbackResist = r.num(base.knockbackResist, 0, 1);
        s.moveSpeed       = r.num(base.moveSpeed, 0.05, 0.6);
        s.jumpForce       = r.num(base.jumpForce, 0.2, 1.5);
        s.followRange     = r.num(base.followRange, 4, 128);
        s.sizeScale       = r.num(base.sizeScale, 0.25, Math.min(16, CraftStatsConfig.get().maxScaleCap));
        if (r.intensity == Intensity.CHAOS) {
            s.immuneFire      = r.rng.nextInt(4) == 0;
            s.immuneFall      = r.rng.nextInt(4) == 0;
            s.immuneExplosion = r.rng.nextInt(4) == 0;
            s.burnsDaylight   = r.rng.nextInt(4) == 0;
            s.glowing         = r.rng.nextInt(4) == 0;
        }
        return s;
    }

    public static BlockStats randomizeBlock(BlockStats base, long seed) {
        Rolls r = new Rolls(seed);
        BlockStats s = base.copy();
        // Unbreakable blocks (hardness -1) stay unbreakable.
        if (base.hardness != null && base.hardness >= 0) s.hardness = r.numF(base.hardness, 0, 50);
        s.blastResistance = r.numF(base.blastResistance, 0, 1200);
        s.slipperiness    = r.numF(base.slipperiness, 0.4f, 1.0f);
        if (r.intensity != Intensity.MILD) s.lightEmission = r.rng.nextInt(16);
        return s;
    }

    public static ItemStats randomizeItem(ItemStats base, long seed) {
        Rolls r = new Rolls(seed);
        ItemStats s = base.copy();
        s.attackDamage = r.num(base.attackDamage, 1, 100);
        s.attackSpeed  = r.num(base.attackSpeed, 0.5, 16);
        if (base.maxDurability != null) s.maxDurability = (int) Math.round(r.num(base.maxDurability.doubleValue(), 1, 100_000));
        if (base.miningSpeed != null)   s.miningSpeed = r.numF(base.miningSpeed, 0.5f, 100f);
        return s;
    }

    public static PlayerStats randomizePlayer(PlayerStats base, long seed) {
        Rolls r = new Rolls(seed);
        PlayerStats s = base.copy();
        s.maxHealth     = r.num(base.maxHealth, 2, 200);
        s.baseDamage    = r.num(base.baseDamage, 0.5, 50);
        s.attackSpeed   = r.num(base.attackSpeed, 1, 20);
        s.walkSpeed     = r.num(base.walkSpeed, 0.03, 0.4);
        s.jumpForce     = r.num(base.jumpForce, 0.3, 1.5);
        s.stepHeight    = r.num(base.stepHeight, 0.5, 3);
        s.reachDistance = r.num(base.reachDistance, 2, 12);
        s.gravity       = r.num(base.gravity, 0.02, 0.16);
        return s;
    }

    /** One seeded sequence of rolls. */
    private static final class Rolls {
        final Random rng;
        final Intensity intensity = getIntensity();

        Rolls(long seed) { rng = new Random(seed); }

        Double num(Double base, double min, double max) {
            if (base == null) return null;
            double v = switch (intensity) {
                case MILD  -> base * (0.8 + rng.nextDouble() * 0.4);
                case WILD  -> base * Math.exp(Math.log(0.25) + rng.nextDouble() * (Math.log(3) - Math.log(0.25)));
                case CHAOS -> min + rng.nextDouble() * (max - min);
            };
            return round(Math.max(min, Math.min(max, v)));
        }

        Float numF(Float base, float min, float max) {
            Double d = num(base == null ? null : base.doubleValue(), min, max);
            return d == null ? null : d.floatValue();
        }

        private static double round(double v) {
            return Math.round(v * 1000.0) / 1000.0;
        }
    }
}
