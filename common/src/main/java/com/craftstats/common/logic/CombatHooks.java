package com.craftstats.common.logic;

import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Damage rules for mobs and players: immunities, damage multipliers, and what happens after
 * a hit lands (on-hit effects, lifesteal, thorns, teleporting away). Called from
 * LivingEntityMixin on the server.
 */
public final class CombatHooks {

    private CombatHooks() {}

    /** True if the damage should be ignored completely. */
    public static boolean isImmune(LivingEntity self, DamageSource src) {
        // /kill and the void always work, so nobody gets stuck falling forever.
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps == null) return false;
            return ps.godMode
                    || (ps.drownImmune && src.is(DamageTypes.DROWN))
                    || (ps.fireImmune && src.is(DamageTypeTags.IS_FIRE))
                    || (ps.noFallDamage && src.is(DamageTypeTags.IS_FALL))
                    || (ps.noPoison && isPoison(self, src))
                    || (ps.noMagic && isMagic(src))
                    || (ps.explosionImmune && src.is(DamageTypeTags.IS_EXPLOSION))
                    || (ps.projectileImmune && src.is(DamageTypeTags.IS_PROJECTILE))
                    || (ps.freezeImmune && src.is(DamageTypeTags.IS_FREEZING))
                    || (ps.lightningImmune && src.is(DamageTypeTags.IS_LIGHTNING));
        }
        MobStats ms = StatRegistry.forEntity(self);
        if (ms == null) return false;
        return ms.invincible
                || (ms.immuneDrown && src.is(DamageTypes.DROWN))
                || (ms.immuneFire && src.is(DamageTypeTags.IS_FIRE))
                || (ms.immuneFall && src.is(DamageTypeTags.IS_FALL))
                || (ms.immuneExplosion && src.is(DamageTypeTags.IS_EXPLOSION))
                || (ms.immunePoison && isPoison(self, src))
                || (ms.immuneMagic && isMagic(src))
                || (ms.immuneProjectiles && src.is(DamageTypeTags.IS_PROJECTILE))
                || (ms.immuneFreezing && src.is(DamageTypeTags.IS_FREEZING))
                || (ms.immuneLightning && src.is(DamageTypeTags.IS_LIGHTNING))
                || (ms.immuneMelee && isMelee(src))
                || (ms.immuneCactus && (src.is(DamageTypes.CACTUS) || src.is(DamageTypes.SWEET_BERRY_BUSH) || src.is(DamageTypes.THORNS)))
                || (ms.immuneSuffocation && (src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.CRAMMING)));
    }

    private static boolean isPoison(LivingEntity self, DamageSource src) {
        return src.is(DamageTypes.MAGIC) && self.hasEffect(MobEffects.POISON);
    }

    private static boolean isMagic(DamageSource src) {
        return src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.WITHER)
                || src.is(DamageTypeTags.WITCH_RESISTANT_TO);
    }

    public static boolean isMelee(DamageSource src) {
        return src.getEntity() instanceof LivingEntity && src.getDirectEntity() == src.getEntity()
                && (src.is(DamageTypes.MOB_ATTACK) || src.is(DamageTypes.PLAYER_ATTACK) || src.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }

    /** Damage dealt/taken multipliers and projectile damage. */
    public static float modifyDamage(LivingEntity self, DamageSource src, float amount) {
        if (amount <= 0 || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        double dmg = amount;
        Entity attacker = src.getEntity();
        if (src.getDirectEntity() instanceof Projectile proj) {
            ProjectileStats p = StatRegistry.forProjectile(proj);
            if (p != null) {
                if (p.damageMultiplier != null) dmg *= p.damageMultiplier;
                if (p.extraDamage != null) dmg += p.extraDamage;
            }
        }
        WorldStats w = StatRegistry.world();
        if (attacker instanceof Player ap) {
            PlayerStats ps = StatRegistry.forPlayer(ap);
            if (ps != null) dmg *= ps.damageDealtMultiplier;
        } else if (attacker instanceof LivingEntity al) {
            MobStats ms = StatRegistry.forEntity(al);
            if (ms != null && ms.damageDealtMultiplier != null) dmg *= ms.damageDealtMultiplier;
            if (w != null && w.mobDamageMultiplier != null) dmg *= w.mobDamageMultiplier;
        }
        if (self instanceof Player sp) {
            PlayerStats ps = StatRegistry.forPlayer(sp);
            if (ps != null) dmg *= ps.damageTakenMultiplier;
            if (w != null && w.playerDamageTakenMultiplier != null) dmg *= w.playerDamageTakenMultiplier;
        } else {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms != null && ms.damageTakenMultiplier != null) dmg *= ms.damageTakenMultiplier;
        }
        return (float) Math.max(0, Math.min(Float.MAX_VALUE, dmg));
    }

    /** After damage was applied: on-hit effects, lifesteal, thorns, teleporting away. */
    public static void afterHurt(LivingEntity self, DamageSource src, float amount) {
        if (self.level().isClientSide()) return;
        Entity attacker = src.getEntity();
        if (isMelee(src) && attacker instanceof LivingEntity a && a != self) {
            HitEffects own = a instanceof Player p ? StatRegistry.forPlayer(p) : StatRegistry.forEntity(a);
            Effects.applyHitEffects(a, self, own, amount);
            ItemStats held = StatRegistry.forItem(a.getMainHandItem().getItem());
            Effects.applyHitEffects(a, self, held, amount);
            double thorns = thornsPercent(self);
            if (thorns > 0 && amount > 0)
                Compat.hurt(a, self.damageSources().thorns(self), (float) (amount * thorns / 100.0));
        }
        if (src.getDirectEntity() instanceof Projectile proj) {
            ProjectileStats p = StatRegistry.forProjectile(proj);
            if (p != null) {
                Effects.applyHitEffects(proj.getOwner() instanceof LivingEntity owner ? owner : null, self, p, amount);
                if (p.knockbackBonus != null && p.knockbackBonus > 0) {
                    Vec3 v = proj.getDeltaMovement();
                    self.knockback(p.knockbackBonus, -v.x, -v.z);
                }
            }
        }
        if (!(self instanceof Player) && self.isAlive()) {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms != null && ms.teleportWhenHurt) Effects.randomTeleport(self, 16);
        }
    }

    private static double thornsPercent(LivingEntity self) {
        if (self instanceof Player p) {
            PlayerStats ps = StatRegistry.forPlayer(p);
            return ps != null ? ps.thornsPercent : 0;
        }
        MobStats ms = StatRegistry.forEntity(self);
        return ms != null && ms.thornsPercent != null ? ms.thornsPercent : 0;
    }
}
