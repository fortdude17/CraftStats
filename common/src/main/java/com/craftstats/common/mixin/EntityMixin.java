package com.craftstats.common.mixin;

import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
        if (!(self instanceof LivingEntity)) return;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.getPlayer(player.getUUID());
            if (ps != null && ps.fireImmune) cir.setReturnValue(false);
            return;
        }
        LivingEntity living = (LivingEntity) self;
        ResourceLocation typeId = EntityType.getKey(living.getType());
        if (typeId == null) return;
        MobStats stats = StatRegistry.getMob(typeId);
        if (stats != null && stats.immuneFire) cir.setReturnValue(false);
    }
}
