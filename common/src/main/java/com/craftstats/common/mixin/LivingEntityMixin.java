package com.craftstats.common.mixin;

import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getMaxHealth", at = @At("HEAD"), cancellable = true)
    private void craftstats$getMaxHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.getPlayer(player.getUUID());
            if (ps != null) { cir.setReturnValue((float) ps.maxHealth); return; }
        }
        MobStats uuidStats = StatRegistry.getMobUuid(self.getUUID());
        if (uuidStats != null && uuidStats.maxHealth >= 0) { cir.setReturnValue((float) uuidStats.maxHealth); return; }
        ResourceLocation typeId = EntityType.getKey(self.getType());
        if (typeId == null) return;
        MobStats stats = StatRegistry.getMob(typeId);
        if (stats != null && stats.maxHealth >= 0) cir.setReturnValue((float) stats.maxHealth);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void craftstats$fallImmune(float dist, float mul,
                                        DamageSource src,
                                        CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.getPlayer(player.getUUID());
            if (ps != null && ps.noFallDamage) { cir.setReturnValue(false); return; }
        }
        MobStats ms = StatRegistry.getMobUuid(self.getUUID());
        if (ms == null) {
            ResourceLocation typeId = EntityType.getKey(self.getType());
            if (typeId == null) return;
            ms = StatRegistry.getMob(typeId);
        }
        if (ms != null && ms.immuneFall) cir.setReturnValue(false);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void craftstats$hurt(DamageSource src, float amount,
                                  CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (self instanceof Player player) {
            PlayerStats ps = StatRegistry.getPlayer(player.getUUID());
            if (ps == null) return;
            if (ps.godMode)   { cir.setReturnValue(false); return; }
            if (ps.drownImmune && isDrown(src))                          { cir.setReturnValue(false); return; }
            if (ps.fireImmune  && src.is(DamageTypeTags.IS_FIRE))        { cir.setReturnValue(false); return; }
            if (ps.noPoison    && isMagic(src))                          { cir.setReturnValue(false); return; }
            if (ps.noMagic     && src.is(DamageTypeTags.BYPASSES_ARMOR)
                    && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { cir.setReturnValue(false); return; }
        } else {
            MobStats ms = StatRegistry.getMobUuid(self.getUUID());
            if (ms == null) {
                ResourceLocation typeId = EntityType.getKey(self.getType());
                if (typeId == null) return;
                ms = StatRegistry.getMob(typeId);
            }
            if (ms == null) return;
            if (ms.invincible)                                           { cir.setReturnValue(false); return; }
            if (ms.immuneDrown     && isDrown(src))                      { cir.setReturnValue(false); return; }
            if (ms.immuneExplosion && src.is(DamageTypeTags.IS_EXPLOSION)){ cir.setReturnValue(false); return; }
            if (ms.immunePoison    && isMagic(src))                      { cir.setReturnValue(false); return; }
            if (ms.immuneMagic     && src.is(DamageTypeTags.BYPASSES_ARMOR)
                    && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { cir.setReturnValue(false); return; }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void craftstats$stepOnEffects(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide) return;

        if (!(self instanceof Player)) {
            ResourceLocation typeId = EntityType.getKey(self.getType());
            if (typeId != null) {
                MobStats ms = StatRegistry.getMobUuid(self.getUUID());
                if (ms == null) ms = StatRegistry.getMob(typeId);
                if (ms != null) {
                    self.setSilent(ms.silent);
                    self.setGlowingTag(ms.glowing);
                    if (ms.invincible && self instanceof Mob mob) {
                        if (mob.invulnerableTime < 10) mob.invulnerableTime = 10;
                    }

                    if (ms.burnsDaylight && self.tickCount % 40 == 0
                            && !self.isOnFire() && !self.isInWaterOrBubble()) {
                        if (self.level().isDay()
                                && !self.level().isRaining()
                                && self.level().canSeeSky(self.blockPosition())) {
                            self.igniteForSeconds(8);
                        }
                    }
                }
            }
        }

        if (!self.onGround()) return;
        BlockPos below = self.blockPosition().below();
        BlockStats bs  = resolveBlockStats(self, below);
        if (bs == null) return;

        DamageSource generic = self.damageSources().generic();

        if (bs.stepDamage > 0 && self.tickCount % 10 == 0)
            self.hurt(generic, bs.stepDamage);

        if (bs.speedModifier != 1.0f && bs.speedModifier > 0) {
            int amplifier = Math.round(bs.speedModifier * 10) - 10;
            if (amplifier >= 0) {
                self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, amplifier, false, false));
            } else {
                self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, -amplifier - 1, false, false));
            }
        }

        if (bs.levitate)
            self.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0, false, false));

        if (bs.glowOnStep)
            self.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));

        if (bs.freezeOnStep) {
            self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 9, false, false));
            self.setTicksFrozen(Math.min(self.getTicksFrozen() + 3, self.getTicksRequiredToFreeze()));
        }

        if (!bs.onStepPotion.isEmpty()) {
            try {
                ResourceLocation potionId = ResourceLocation.parse(bs.onStepPotion);
                BuiltInRegistries.MOB_EFFECT.getHolder(potionId).ifPresent(holder -> {
                    int dur = Math.max(25, bs.onStepPotionDuration + 20);
                    int amp = Math.max(0, bs.onStepPotionLevel - 1);
                    if (!self.hasEffect(holder) || self.getEffect(holder).getDuration() < 20)
                        self.addEffect(new MobEffectInstance(holder, dur, amp, false, false));
                });
            } catch (Exception ignored) {}
        }
    }

    private static BlockStats resolveBlockStats(LivingEntity entity, BlockPos pos) {

        if (entity.level() instanceof Level lv) {
            String posKey = StatRegistry.makePosKey(lv.dimension(), pos);
            BlockStats ps = StatRegistry.getBlockAt(posKey);
            if (ps != null) return ps;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK
                .getKey(entity.level().getBlockState(pos).getBlock());
        return StatRegistry.getBlock(blockId);
    }

    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$xpReward(ServerLevel level, Entity killer,
                                      CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof Player) return;
        MobStats ms = StatRegistry.getMobUuid(self.getUUID());
        if (ms == null) {
            ResourceLocation typeId = EntityType.getKey(self.getType());
            if (typeId == null) return;
            ms = StatRegistry.getMob(typeId);
        }
        if (ms != null && ms.xpReward > 0) cir.setReturnValue(ms.xpReward);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$keepInventory(ServerLevel level, DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof Player)) return;
        PlayerStats ps = StatRegistry.getPlayer(((Player) self).getUUID());
        if (ps != null && ps.keepInventory) ci.cancel();
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true, require = 0)
    private void craftstats$climbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        BlockPos pos = self.blockPosition();

        if (self.level() instanceof Level lv) {
            String posKey = StatRegistry.makePosKey(lv.dimension(), pos);
            BlockStats ps = StatRegistry.getBlockAt(posKey);
            if (ps != null && ps.climbable) { cir.setReturnValue(true); return; }
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
                self.level().getBlockState(pos).getBlock());
        BlockStats stats = StatRegistry.getBlock(blockId);
        if (stats != null && stats.climbable) cir.setReturnValue(true);
    }

    private static boolean isDrown(DamageSource src) {
        return "drown".equals(src.type().msgId());
    }

    private static boolean isMagic(DamageSource src) {
        return "magic".equals(src.type().msgId());
    }
}
