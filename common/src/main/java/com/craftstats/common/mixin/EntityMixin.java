package com.craftstats.common.mixin;

import com.craftstats.common.stats.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    private void craftstats$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && ps.fireImmune) cir.setReturnValue(false);
        } else if (self instanceof LivingEntity living) {
            MobStats ms = StatRegistry.forEntity(living);
            if (ms != null && ms.immuneFire) cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSilent", at = @At("HEAD"), cancellable = true)
    private void craftstats$silent(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living) {
            MobStats ms = StatRegistry.forEntity(living);
            if (ms != null && ms.silent) cir.setReturnValue(true);
        }
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void craftstats$glowing(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living) {
            MobStats ms = StatRegistry.forEntity(living);
            if (ms != null && ms.glowing) cir.setReturnValue(true);
        }
    }

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void craftstats$invisible(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living) {
            MobStats ms = StatRegistry.forEntity(living);
            if (ms != null && ms.invisible) cir.setReturnValue(true);
        }
    }

    @Inject(method = "isNoGravity", at = @At("HEAD"), cancellable = true)
    private void craftstats$noGravity(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Projectile) {
            ProjectileStats p = StatRegistry.forProjectile(self);
            if (p != null && p.noGravity) cir.setReturnValue(true);
        } else if (self instanceof LivingEntity living && !(self instanceof Player)) {
            MobStats ms = StatRegistry.forEntity(living);
            if (ms != null && ms.noGravity) cir.setReturnValue(true);
        }
    }

    /** Projectile gravity and the world-wide gravity multiplier. Runs on both sides. */
    @Inject(method = "getGravity", at = @At("RETURN"), cancellable = true)
    private void craftstats$gravity(CallbackInfoReturnable<Double> cir) {
        WorldStats w = StatRegistry.world();
        Entity self = (Entity) (Object) this;
        double g = cir.getReturnValueD();
        double out = g;
        if (self instanceof Projectile) {
            ProjectileStats p = StatRegistry.forProjectile(self);
            if (p != null && p.gravityMultiplier != null) out *= p.gravityMultiplier;
        }
        if (w != null && w.gravityMultiplier != null) out *= w.gravityMultiplier;
        if (out != g) cir.setReturnValue(out);
    }
}
