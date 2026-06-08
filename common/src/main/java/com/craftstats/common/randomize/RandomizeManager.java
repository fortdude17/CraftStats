package com.craftstats.common.randomize;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.stats.*;

import java.util.Random;

public final class RandomizeManager {

    public enum Intensity { MILD, WILD, CHAOS }

    private RandomizeManager() {}

    public static Intensity getIntensity() {
        return switch (CraftStatsConfig.get().randomizeIntensity.toLowerCase()) {
            case "wild"  -> Intensity.WILD;
            case "chaos" -> Intensity.CHAOS;
            default      -> Intensity.MILD;
        };
    }

    public static MobStats randomizeMob(MobStats vanilla, long seed) {
        Random rng = new Random(seed);
        MobStats r = vanilla.copy();
        Intensity intensity = getIntensity();
        r.maxHealth       = randomizeDouble(rng, vanilla.maxHealth,       intensity, 1, Double.MAX_VALUE / 2);
        r.attackDamage    = randomizeDouble(rng, vanilla.attackDamage,    intensity, 0, Double.MAX_VALUE / 2);
        r.armor           = randomizeDouble(rng, vanilla.armor,           intensity, 0, 30);
        r.knockbackResist = randomizeDouble(rng, vanilla.knockbackResist, intensity, 0, 1);
        r.moveSpeed       = randomizeDouble(rng, vanilla.moveSpeed,       intensity, 0.01, 2.0);
        r.followRange     = randomizeDouble(rng, vanilla.followRange,     intensity, 1, 128);
        r.sizeScale       = randomizeDouble(rng, vanilla.sizeScale,       intensity, 0.1, 10);
        if (intensity == Intensity.CHAOS) {
            r.immuneFire       = rng.nextBoolean();
            r.immuneFall       = rng.nextBoolean();
            r.immuneDrown      = rng.nextBoolean();
            r.immuneExplosion  = rng.nextBoolean();
            r.burnsDaylight    = rng.nextBoolean();
            r.canDespawn       = rng.nextBoolean();
        }
        return r;
    }

    public static BlockStats randomizeBlock(BlockStats vanilla, long seed) {
        Random rng = new Random(seed);
        BlockStats r = vanilla.copy();
        Intensity intensity = getIntensity();
        r.hardness        = (float) randomizeDouble(rng, vanilla.hardness,       intensity, -1, 50);
        r.blastResistance = (float) randomizeDouble(rng, vanilla.blastResistance,intensity, 0,  3600000);
        r.slipperiness    = (float) randomizeDouble(rng, vanilla.slipperiness,   intensity, 0.1f, 2.0f);
        r.lightEmission   = (int)   randomizeDouble(rng, vanilla.lightEmission,  intensity, 0, 15);
        return r;
    }

    public static ItemStats randomizeItem(ItemStats vanilla, long seed) {
        Random rng = new Random(seed);
        ItemStats r = vanilla.copy();
        Intensity intensity = getIntensity();
        r.attackDamage  = randomizeDouble(rng, vanilla.attackDamage,  intensity, 0, 100);
        r.attackSpeed   = randomizeDouble(rng, vanilla.attackSpeed,   intensity, 0.5, 16);
        r.maxDurability = (int) randomizeDouble(rng, vanilla.maxDurability, intensity, 0, 100000);
        r.stackSize     = Math.min(64, (int) randomizeDouble(rng, vanilla.stackSize, intensity, 1, 64));
        return r;
    }

    public static PlayerStats randomizePlayer(PlayerStats vanilla, long seed) {
        Random rng = new Random(seed);
        PlayerStats r = vanilla.copy();
        Intensity intensity = getIntensity();
        r.maxHealth    = randomizeDouble(rng, vanilla.maxHealth,    intensity, 1, 1024);
        r.baseDamage   = randomizeDouble(rng, vanilla.baseDamage,   intensity, 0.5, 100);
        r.walkSpeed    = randomizeDouble(rng, vanilla.walkSpeed,    intensity, 0.01, 1.0);
        r.flySpeed     = randomizeDouble(rng, vanilla.flySpeed,     intensity, 0.01, 1.0);
        r.jumpForce    = randomizeDouble(rng, vanilla.jumpForce,    intensity, 0.1, 5.0);
        return r;
    }

    public static long newSeed() { return System.currentTimeMillis(); }

    private static double randomizeDouble(Random rng, double base, Intensity intensity,
                                          double min, double max) {
        double factor = switch (intensity) {
            case MILD  -> 1.0 + (rng.nextDouble() * 0.4 - 0.2);
            case WILD  -> 1.0 + (rng.nextDouble() * 4.0 - 2.0);
            case CHAOS -> rng.nextDouble() * (max - min) / base;
        };
        return Math.max(min, Math.min(max, base * factor));
    }
}
