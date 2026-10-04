package com.craftstats.common.stats;

/** Effects applied to whatever a mob, player, item or projectile hits. Nulls/blank mean "off". */
public interface HitEffects {
    Integer fireOnHit();
    String  hitEffect();
    Integer hitEffectLevel();
    Integer hitEffectSeconds();
    Double  lifestealPercent();
    boolean lightningOnHit();
}
