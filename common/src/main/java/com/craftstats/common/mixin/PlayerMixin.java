package com.craftstats.common.mixin;

import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void craftstats$tick(CallbackInfo ci) {
        Player self = (Player)(Object)this;
        if (self.level().isClientSide()) return;
        PlayerStats ps = StatRegistry.getPlayer(self.getUUID());
        self.noPhysics = (ps != null && ps.noClip);
        if (ps == null) return;

        if (self.tickCount % 60 == 0) craftstats$applyPermanentEffects(self, ps);
    }

    private static void craftstats$applyPermanentEffects(Player self, PlayerStats ps) {
        int dur = 200;
        if (ps.fxNightVision)  craftstats$grant(self, MobEffects.NIGHT_VISION,   dur, 0);
        if (ps.fxWaterBreath)  craftstats$grant(self, MobEffects.WATER_BREATHING, dur, 0);
        if (ps.fxFireResist)   craftstats$grant(self, MobEffects.FIRE_RESISTANCE, dur, 0);
        if (ps.fxRegen)        craftstats$grant(self, MobEffects.REGENERATION,    dur, 0);
        if (ps.fxGlowing)      craftstats$grant(self, MobEffects.GLOWING,         dur, 0);
        if (ps.fxInvisibility) craftstats$grant(self, MobEffects.INVISIBILITY,    dur, 0);
        if (ps.fxHaste > 0)    craftstats$grant(self, MobEffects.DIG_SPEED,       dur, ps.fxHaste - 1);
        if (ps.fxStrength > 0) craftstats$grant(self, MobEffects.DAMAGE_BOOST,    dur, ps.fxStrength - 1);
        if (ps.fxSpeed > 0)    craftstats$grant(self, MobEffects.MOVEMENT_SPEED,  dur, ps.fxSpeed - 1);
    }

    private static void craftstats$grant(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
            int duration, int amp) {
        var existing = entity.getEffect(effect);

        if (existing == null || existing.getDuration() < 80)
            entity.addEffect(new MobEffectInstance(effect, duration, amp, true, false));
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void craftstats$oneHitKill(Entity target, CallbackInfo ci) {
        Player self = (Player)(Object)this;
        PlayerStats ps = StatRegistry.getPlayer(self.getUUID());
        if (ps == null || !ps.oneHitKill) return;
        target.hurt(self.damageSources().playerAttack(self), 9_999f);
        ci.cancel();
    }

    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5f), require = 0)
    private float craftstats$critMultiplier(float vanilla) {
        Player self = (Player)(Object)this;
        PlayerStats ps = StatRegistry.getPlayer(self.getUUID());
        return (ps != null) ? (float) ps.critMultiplier : vanilla;
    }

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true, require = 0)
    private int craftstats$xpMultiplier(int amount) {
        Player self = (Player)(Object)this;
        PlayerStats ps = StatRegistry.getPlayer(self.getUUID());
        if (ps != null && ps.xpMultiplier != 1.0) return (int)(amount * ps.xpMultiplier);
        return amount;
    }

}
