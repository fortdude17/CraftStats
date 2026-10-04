package com.craftstats.common.mixin;

import com.craftstats.common.logic.BlockHooks;
import com.craftstats.common.logic.CombatHooks;
import com.craftstats.common.logic.ItemHooks;
import com.craftstats.common.logic.MobBehaviour;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected boolean dead;
    @Unique private boolean craftstats$rollingExtraLoot;

    //#if MC >= 1.21.2
    //$ @Shadow protected abstract void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKill);
    //#else
    @Shadow protected abstract void dropFromLootTable(DamageSource source, boolean playerKill);
    //#endif

    // ---- damage --------------------------------------------------------------------------

    //#if MC >= 1.21.2
    //$ @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    //$ private void craftstats$immunity(ServerLevel level, DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void craftstats$immunity(DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (CombatHooks.isImmune((LivingEntity) (Object) this, src)) cir.setReturnValue(false);
    }

    //#if MC >= 1.21.2
    //$ @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    //#else
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    //#endif
    private float craftstats$damageMultipliers(float amount, @Local(argsOnly = true) DamageSource src) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return amount;
        return CombatHooks.modifyDamage(self, src, amount);
    }

    //#if MC >= 1.21.2
    //$ @Inject(method = "hurtServer", at = @At("RETURN"))
    //$ private void craftstats$afterHurt(ServerLevel level, DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "hurt", at = @At("RETURN"))
    private void craftstats$afterHurt(DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (!cir.getReturnValueZ()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;
        // Custom invulnerability window after a player takes damage (vanilla: 20 ticks).
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && ps.invincibilityFrames != 20) player.invulnerableTime = Math.max(0, ps.invincibilityFrames);
        }
        CombatHooks.afterHurt(self, src, amount);
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void craftstats$effectImmunity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && ps.negativeEffectImmune && effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                cir.setReturnValue(false);
        } else {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms != null && ms.immuneEffects) cir.setReturnValue(false);
        }
    }

    //#if MC >= 1.21.11
    //$ @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    //$ private void craftstats$fallImmune(double distance, float multiplier, DamageSource src, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void craftstats$fallImmune(float distance, float multiplier, DamageSource src, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && (ps.noFallDamage || ps.godMode)) cir.setReturnValue(false);
        } else {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms != null && (ms.immuneFall || ms.invincible)) cir.setReturnValue(false);
        }
    }

    /** World-wide fall damage multiplier (the float argument after the distance). */
    //#if MC >= 1.21.11
    //$ @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    //#else
    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    //#endif
    private float craftstats$worldFallDamage(float multiplier) {
        WorldStats w = StatRegistry.world();
        return w != null && w.fallDamageMultiplier != null ? (float) (multiplier * w.fallDamageMultiplier) : multiplier;
    }

    // ---- per-tick ------------------------------------------------------------------------

    @Inject(method = "tick", at = @At("TAIL"))
    private void craftstats$tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            // Player movement is simulated by the player's own client.
            if (self instanceof Player p && p.isLocalPlayer() && StatRegistry.hasBlockOverrides()) BlockHooks.entityTick(self);
            return;
        }

        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null) MobBehaviour.tickPlayer(player, ps);
        } else {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms != null) {
                if (ms.burnsDaylight && self.tickCount % 40 == 0 && !self.isOnFire() && !self.isInWater()
                        && Compat.isDaytime(self.level()) && !self.level().isRaining()
                        && self.level().canSeeSky(self.blockPosition()))
                    self.igniteForSeconds(8);
                MobBehaviour.tick(self, ms);
            }
        }
        if (StatRegistry.hasBlockOverrides()) BlockHooks.entityTick(self);
    }

    // ---- death & drops -------------------------------------------------------------------

    @Inject(method = "die", at = @At("HEAD"))
    private void craftstats$death(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && !self.isRemoved() && !this.dead) MobBehaviour.onDeath(self);
    }

    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void craftstats$xpReward(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && ps.keepXp) cir.setReturnValue(0); // kept on respawn instead (ServerPlayerMixin)
            return;
        }
        MobStats ms = StatRegistry.forEntity(self);
        if (ms != null && ms.xpReward != null) cir.setReturnValue(Math.max(0, ms.xpReward));
    }

    /**
     * Keep Inventory and soulbound items for players (given back in ServerPlayerMixin);
     * No Drops for mobs.
     */
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void craftstats$deathLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && ps.keepInventory) { ci.cancel(); return; }
            ItemHooks.stashSoulbound(player);
            return;
        }
        MobStats ms = StatRegistry.forEntity(self);
        if (ms != null && ms.noDrops) ci.cancel();
    }

    //#if MC >= 1.21.2
    //$ @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("TAIL"))
    //$ private void craftstats$extraLoot(ServerLevel level, DamageSource source, boolean playerKill, CallbackInfo ci) {
    //#else
    @Inject(method = "dropFromLootTable", at = @At("TAIL"))
    private void craftstats$extraLoot(DamageSource source, boolean playerKill, CallbackInfo ci) {
    //#endif
        if (craftstats$rollingExtraLoot) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) return;
        MobStats ms = StatRegistry.forEntity(self);
        if (ms == null || ms.extraLootRolls == null || ms.extraLootRolls <= 0) return;
        craftstats$rollingExtraLoot = true;
        try {
            for (int i = 0; i < Math.min(32, ms.extraLootRolls); i++)
                //#if MC >= 1.21.2
                //$ dropFromLootTable(level, source, playerKill);
                //#else
                dropFromLootTable(source, playerKill);
                //#endif
        } finally {
            craftstats$rollingExtraLoot = false;
        }
    }

    // ---- misc ----------------------------------------------------------------------------

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void craftstats$noPush(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) return;
        MobStats ms = StatRegistry.forEntity(self);
        if (ms != null && ms.noPush) cir.setReturnValue(false);
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void craftstats$climbable(CallbackInfoReturnable<Boolean> cir) {
        if (!StatRegistry.hasBlockOverrides()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isSpectator()) return;
        BlockPos pos = self.blockPosition();
        BlockStats s = StatRegistry.forBlockAt(self.level().getBlockState(pos).getBlock(), self.level(), pos);
        if (s != null && s.climbable) cir.setReturnValue(true);
    }
}
