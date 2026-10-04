package com.craftstats.common.mixin;

import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
}
