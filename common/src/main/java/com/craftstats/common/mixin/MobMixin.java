package com.craftstats.common.mixin;

import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void craftstats$canDespawn(double distSq, CallbackInfoReturnable<Boolean> cir) {
        MobStats ms = StatRegistry.forEntity((Mob) (Object) this);
        if (ms != null && !ms.canDespawn) cir.setReturnValue(false);
    }
}
