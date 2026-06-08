package com.craftstats.common.mixin;

import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$canDespawn(double distSq, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob)(Object)this;
        if (self.level().isClientSide()) return;
        MobStats ms = StatRegistry.getMobUuid(self.getUUID());
        if (ms == null) {
            ResourceLocation typeId = EntityType.getKey(self.getType());
            if (typeId == null) return;
            ms = StatRegistry.getMob(typeId);
        }
        if (ms != null && !ms.canDespawn) cir.setReturnValue(false);
    }
}
