package com.craftstats.common.mixin;

import com.craftstats.common.logic.MobBehaviour;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import com.craftstats.common.util.Compat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC >= 1.21.2
//$ import net.minecraft.server.level.ServerLevel;
//#endif

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void craftstats$canDespawn(double distSq, CallbackInfoReturnable<Boolean> cir) {
        WorldStats w = StatRegistry.world();
        MobStats ms = StatRegistry.forEntity((Mob) (Object) this);
        if ((ms != null && !ms.canDespawn) || (w != null && w.mobsNeverDespawn)) cir.setReturnValue(false);
    }

    @Inject(method = "isNoAi", at = @At("HEAD"), cancellable = true)
    private void craftstats$noAi(CallbackInfoReturnable<Boolean> cir) {
        MobStats ms = StatRegistry.forEntity((Mob) (Object) this);
        if (ms != null && ms.noAi) cir.setReturnValue(true);
    }

    /** Peaceful mobs (and "Mobs Ignore Players") never pick a player as their target. */
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity craftstats$ignorePlayers(LivingEntity target) {
        return target instanceof Player && MobBehaviour.ignoresPlayers((Mob) (Object) this) ? null : target;
    }

    @Inject(method = "canPickUpLoot", at = @At("HEAD"), cancellable = true)
    private void craftstats$pickUpLoot(CallbackInfoReturnable<Boolean> cir) {
        MobStats ms = StatRegistry.forEntity((Mob) (Object) this);
        if (ms == null) return;
        if (MobStats.LOOT_YES.equals(ms.pickUpLoot)) cir.setReturnValue(true);
        else if (MobStats.LOOT_NO.equals(ms.pickUpLoot)) cir.setReturnValue(false);
    }

    /**
     * Passive mobs made hostile have no attack damage attribute, and vanilla would crash
     * reading it. Hit with the configured damage (or 2) instead.
     */
    //#if MC >= 1.21.2
    //$ @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    //$ private void craftstats$attackWithoutAttribute(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void craftstats$attackWithoutAttribute(Entity target, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        Mob self = (Mob) (Object) this;
        if (self.getAttribute(Attributes.ATTACK_DAMAGE) != null) return;
        MobStats ms = StatRegistry.forEntity(self);
        float damage = ms != null && ms.attackDamage != null ? ms.attackDamage.floatValue() : 2f;
        boolean hit = Compat.hurtAndCheck(target, self.damageSources().mobAttack(self), damage);
        if (hit) self.setLastHurtMob(target);
        cir.setReturnValue(hit);
    }
}
