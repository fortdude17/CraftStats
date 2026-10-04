package com.craftstats.common.mixin;

import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import com.craftstats.common.util.Compat;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    /** True while CraftStats (not the game mode) is what lets this player fly. */
    @Unique private boolean craftstats$grantedFlight;

    /**
     * No Clip. Vanilla resets noPhysics to isSpectator() at the start of every tick, so it is
     * set again right after that. Runs on both sides because movement is simulated on the
     * client.
     */
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void craftstats$noClip(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        PlayerStats ps = StatRegistry.forPlayer(self);
        boolean noClip = ps != null && ps.noClip;
        if (noClip) self.noPhysics = true;

        if (self.level().isClientSide()) return;
        // Without flight, a no-clip player would fall through the world.
        boolean fly = noClip || (ps != null && ps.canFly);
        var abilities = self.getAbilities();
        if (fly && !abilities.mayfly) {
            abilities.mayfly = true;
            craftstats$grantedFlight = true;
            self.onUpdateAbilities();
        } else if (!fly && craftstats$grantedFlight) {
            craftstats$grantedFlight = false;
            if (!self.isCreative() && !self.isSpectator()) {
                abilities.mayfly = false;
                abilities.flying = false;
                self.onUpdateAbilities();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void craftstats$permanentEffects(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide() || self.tickCount % 40 != 0) return;
        PlayerStats ps = StatRegistry.forPlayer(self);
        if (ps == null) return;
        if (ps.fxNightVision)  craftstats$grant(self, MobEffects.NIGHT_VISION, 0);
        if (ps.fxWaterBreath)  craftstats$grant(self, MobEffects.WATER_BREATHING, 0);
        if (ps.fxFireResist)   craftstats$grant(self, MobEffects.FIRE_RESISTANCE, 0);
        if (ps.fxRegen)        craftstats$grant(self, MobEffects.REGENERATION, 0);
        if (ps.fxGlowing)      craftstats$grant(self, MobEffects.GLOWING, 0);
        if (ps.fxInvisibility) craftstats$grant(self, MobEffects.INVISIBILITY, 0);
        if (ps.fxHaste > 0)    craftstats$grant(self, Compat.HASTE, ps.fxHaste - 1);
        if (ps.fxStrength > 0) craftstats$grant(self, Compat.STRENGTH, ps.fxStrength - 1);
        if (ps.fxSpeed > 0)    craftstats$grant(self, Compat.SPEED, ps.fxSpeed - 1);
        if (ps.fxJumpBoost > 0) craftstats$grant(self, Compat.JUMP_BOOST, ps.fxJumpBoost - 1);
        if (ps.fxResistance > 0) craftstats$grant(self, Compat.RESISTANCE, ps.fxResistance - 1);
        if (ps.fxSlowFalling)   craftstats$grant(self, MobEffects.SLOW_FALLING, 0);
        if (ps.fxDolphinsGrace) craftstats$grant(self, MobEffects.DOLPHINS_GRACE, 0);
        if (ps.fxConduitPower)  craftstats$grant(self, MobEffects.CONDUIT_POWER, 0);
        if (ps.fxSaturation)    craftstats$grant(self, MobEffects.SATURATION, 0);
    }

    private static void craftstats$grant(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance existing = entity.getEffect(effect);
        // Night vision flickers below 10 seconds, so keep a comfortable margin.
        if (existing == null || existing.getDuration() < 300 || existing.getAmplifier() != amplifier)
            entity.addEffect(new MobEffectInstance(effect, 400, Math.min(255, amplifier), true, false));
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void craftstats$oneHitKill(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return;
        PlayerStats ps = StatRegistry.forPlayer(self);
        if (ps == null || !ps.oneHitKill || !target.isAttackable()) return;
        Compat.hurt(target, self.damageSources().playerAttack(self), Float.MAX_VALUE);
        ci.cancel();
    }

    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F), require = 0)
    private float craftstats$critMultiplier(float vanilla) {
        PlayerStats ps = StatRegistry.forPlayer((Player) (Object) this);
        return ps != null ? (float) ps.critMultiplier : vanilla;
    }

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int craftstats$xpMultiplier(int amount) {
        PlayerStats ps = StatRegistry.forPlayer((Player) (Object) this);
        if (ps == null || ps.xpMultiplier == 1.0 || amount <= 0) return amount;
        return (int) Math.min(Integer.MAX_VALUE, Math.round(amount * Math.max(0, ps.xpMultiplier)));
    }

    /** Hunger drain rate and Infinite Sprint scale the exhaustion as it is added. */
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float craftstats$hungerRate(float exhaustion) {
        WorldStats w = StatRegistry.world();
        if (w != null && w.hungerMultiplier != null) exhaustion *= (float) Math.max(0, w.hungerMultiplier);
        PlayerStats ps = StatRegistry.forPlayer((Player) (Object) this);
        if (ps == null) return exhaustion;
        if (ps.infiniteSprint) return 0f;
        return (float) (exhaustion * Math.max(0, ps.hungerDrainRate));
    }
}
