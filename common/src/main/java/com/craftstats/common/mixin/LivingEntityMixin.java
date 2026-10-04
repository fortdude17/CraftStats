package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // ---- damage --------------------------------------------------------------------------

    //#if MC >= 1.21.2
    //$ @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    //$ private void craftstats$blockDamage(ServerLevel level, DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void craftstats$blockDamage(DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        LivingEntity self = (LivingEntity) (Object) this;
        // /kill and the void always work, so nobody gets stuck falling forever.
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps == null) return;
            if (ps.godMode
                    || (ps.drownImmune && src.is(DamageTypes.DROWN))
                    || (ps.fireImmune && src.is(DamageTypeTags.IS_FIRE))
                    || (ps.noFallDamage && src.is(DamageTypeTags.IS_FALL))
                    || (ps.noPoison && craftstats$isPoison(self, src))
                    || (ps.noMagic && craftstats$isMagic(src)))
                cir.setReturnValue(false);
        } else {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms == null) return;
            if (ms.invincible
                    || (ms.immuneDrown && src.is(DamageTypes.DROWN))
                    || (ms.immuneFire && src.is(DamageTypeTags.IS_FIRE))
                    || (ms.immuneFall && src.is(DamageTypeTags.IS_FALL))
                    || (ms.immuneExplosion && src.is(DamageTypeTags.IS_EXPLOSION))
                    || (ms.immunePoison && craftstats$isPoison(self, src))
                    || (ms.immuneMagic && craftstats$isMagic(src)))
                cir.setReturnValue(false);
        }
    }

    /** Custom invulnerability window after a player takes damage (vanilla: 20 ticks). */
    //#if MC >= 1.21.2
    //$ @Inject(method = "hurtServer", at = @At("RETURN"))
    //$ private void craftstats$invulnerability(ServerLevel level, DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "hurt", at = @At("RETURN"))
    private void craftstats$invulnerability(DamageSource src, float amount, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        if (!cir.getReturnValueZ() || !((Object) this instanceof Player player)) return;
        PlayerStats ps = StatRegistry.forPlayer(player);
        if (ps != null && ps.invincibilityFrames != 20) player.invulnerableTime = Math.max(0, ps.invincibilityFrames);
    }

    private static boolean craftstats$isPoison(LivingEntity self, DamageSource src) {
        return src.is(DamageTypes.MAGIC) && self.hasEffect(MobEffects.POISON);
    }

    private static boolean craftstats$isMagic(DamageSource src) {
        return src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.WITHER)
                || src.is(DamageTypeTags.WITCH_RESISTANT_TO);
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

    // ---- per-tick effects --------------------------------------------------------------

    @Inject(method = "tick", at = @At("TAIL"))
    private void craftstats$tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        if (!(self instanceof Player)) {
            MobStats ms = StatRegistry.forEntity(self);
            if (ms != null && ms.burnsDaylight && self.tickCount % 40 == 0 && !self.isOnFire() && !self.isInWater()
                    && Compat.isDaytime(self.level()) && !self.level().isRaining()
                    && self.level().canSeeSky(self.blockPosition())) {
                self.igniteForSeconds(8);
            }
        }

        if (!StatRegistry.hasBlockOverrides() || !self.onGround()) return;
        BlockPos below = self.blockPosition().below();
        BlockStats bs = StatRegistry.forBlockAt(self.level().getBlockState(below).getBlock(), self.level(), below);
        if (bs == null || !bs.hasStepEffects()) return;

        if (bs.stepDamage > 0 && self.tickCount % 10 == 0)
            Compat.hurt(self, self.damageSources().generic(), bs.stepDamage);

        if (bs.speedModifier > 0 && bs.speedModifier != 1.0f) {
            int amplifier = Math.round(bs.speedModifier * 10) - 10;
            if (amplifier > 0)
                self.addEffect(new MobEffectInstance(Compat.SPEED, 25, amplifier - 1, false, false));
            else if (amplifier < 0)
                self.addEffect(new MobEffectInstance(Compat.SLOWNESS, 25, Math.min(9, -amplifier - 1), false, false));
        }
        if (bs.levitate)
            self.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0, false, false));
        if (bs.glowOnStep)
            self.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
        if (bs.freezeOnStep) {
            self.addEffect(new MobEffectInstance(Compat.SLOWNESS, 25, 3, false, false));
            self.setTicksFrozen(Math.min(self.getTicksFrozen() + 3, self.getTicksRequiredToFreeze() + 10));
        }
        if (!bs.onStepPotion.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(bs.onStepPotion.trim());
            if (id != null) Compat.effect(id).ifPresent(holder -> {
                MobEffectInstance current = self.getEffect(holder);
                if (current == null || current.getDuration() < 20)
                    self.addEffect(new MobEffectInstance(holder, Math.max(25, bs.onStepPotionDuration),
                            Math.max(0, bs.onStepPotionLevel - 1), false, true));
            });
        }
    }

    // ---- drops & misc ------------------------------------------------------------------

    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void craftstats$xpReward(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
        MobStats ms = StatRegistry.forEntity((LivingEntity) (Object) this);
        if (ms != null && ms.xpReward != null) cir.setReturnValue(Math.max(0, ms.xpReward));
    }

    /** Keep Inventory: nothing is dropped; the inventory is copied over on respawn (ServerPlayerMixin). */
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void craftstats$keepInventory(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            PlayerStats ps = StatRegistry.forPlayer(player);
            if (ps != null && ps.keepInventory) ci.cancel();
        }
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
