package com.craftstats.common.logic;

import com.craftstats.common.mixin.AbstractArrowAccessor;
import com.craftstats.common.stats.ProjectileStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//#if MC >= 1.21.11
//$ import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//#else
import net.minecraft.world.entity.projectile.AbstractArrow;
//#endif

/** What projectile stats do: launch modifiers, homing, lifetime and on-hit effects. */
public final class ProjectileHooks {

    /** Saved on the entity so reloading a chunk doesn't speed an arrow up twice. */
    private static final String BOOSTED_TAG = "craftstats_boosted";

    private ProjectileHooks() {}

    /** When a projectile enters the world (server). */
    public static void onSpawn(Projectile p) {
        ProjectileStats s = StatRegistry.forProjectile(p);
        if (s == null || p.getTags().contains(BOOSTED_TAG)) return;
        p.addTag(BOOSTED_TAG);
        if (s.speedMultiplier != null && s.speedMultiplier != 1.0) {
            p.setDeltaMovement(p.getDeltaMovement().scale(Math.max(0.05, Math.min(10, s.speedMultiplier))));
            p.hurtMarked = true;
        }
        if (p instanceof AbstractArrow arrow) {
            if (s.alwaysCrit) arrow.setCritArrow(true);
            if (s.piercing != null && s.piercing > 0)
                ((AbstractArrowAccessor) arrow).craftstats$setPierceLevel((byte) Math.min(127, s.piercing));
            if (s.noPickup) arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }
    }

    /** Every tick (server): lifetime and homing. */
    public static void tick(Projectile p) {
        ProjectileStats s = StatRegistry.forProjectile(p);
        if (s == null) return;
        if (s.lifetimeSeconds != null && s.lifetimeSeconds > 0 && p.tickCount > s.lifetimeSeconds * 20) {
            p.discard();
            return;
        }
        if (s.homing && p.tickCount > 3) home(p);
    }

    private static void home(Projectile p) {
        Vec3 v = p.getDeltaMovement();
        double speed = v.length();
        if (speed < 0.05) return; // stuck in a block
        Entity owner = p.getOwner();
        AABB area = p.getBoundingBox().inflate(16);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != owner && !(e instanceof Player pl && (pl.isCreative() || pl.isSpectator())))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(p.position());
            double dist = to.length();
            if (dist < 1.0E-3) continue;
            double facing = to.normalize().dot(v.normalize());
            if (facing < 0.2) continue; // only things roughly ahead
            double score = dist * (2 - facing);
            if (score < bestScore) { bestScore = score; best = e; }
        }
        if (best == null) return;
        Vec3 want = best.getBoundingBox().getCenter().subtract(p.position()).normalize().scale(speed);
        p.setDeltaMovement(v.scale(0.75).add(want.scale(0.25)).normalize().scale(speed));
        p.hurtMarked = true;
    }

    /** When a projectile hits anything (server): explosions, lightning, spawns, ender-pearl teleport. */
    public static void onHit(Projectile p, HitResult hit) {
        if (!(p.level() instanceof ServerLevel level) || hit.getType() == HitResult.Type.MISS) return;
        ProjectileStats s = StatRegistry.forProjectile(p);
        if (s == null) return;
        Vec3 at = hit.getLocation();
        if (s.lightningOnHit) Effects.lightning(level, at);
        Effects.spawn(level, s.spawnOnHit, 1, at);
        if (s.teleportShooter && p.getOwner() instanceof LivingEntity owner && owner.level() == level) {
            owner.teleportTo(at.x, at.y, at.z);
            owner.resetFallDistance();
        }
        if (s.explodeOnHit != null && s.explodeOnHit > 0) {
            Effects.explode(level, p, at, s.explodeOnHit);
            p.discard();
        }
    }
}
