package com.craftstats.common.mixin;

import com.craftstats.common.logic.ItemHooks;
import com.craftstats.common.logic.ProjectileHooks;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void craftstats$tick(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide()) return;
        if (ItemHooks.isThrown(self)) ItemHooks.thrownTick(self);
        if (!StatRegistry.allProjectiles().isEmpty()) ProjectileHooks.tick(self);
    }

    @Inject(method = "onHit", at = @At("HEAD"))
    private void craftstats$onHit(HitResult hit, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!self.level().isClientSide() && !StatRegistry.allProjectiles().isEmpty()) ProjectileHooks.onHit(self, hit);
    }
}
