package com.craftstats.common.mixin;

import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Shadow private float exhaustionLevel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void craftstats$preTick(Player player, CallbackInfo ci) {
        PlayerStats ps = StatRegistry.getPlayer(player.getUUID());
        if (ps == null) return;

        if (this.foodLevel > ps.maxFoodLevel) {
            this.foodLevel = ps.maxFoodLevel;
            if (this.saturationLevel > ps.maxFoodLevel)
                this.saturationLevel = ps.maxFoodLevel;
        }

        if (ps.infiniteSprint) {
            this.exhaustionLevel = 0f;
            return;
        }

        if (ps.hungerDrainRate != 1.0 && this.exhaustionLevel > 0) {
            this.exhaustionLevel *= (float) ps.hungerDrainRate;
        }

        if (this.exhaustionLevel > (float) ps.exhaustionCap) {
            this.exhaustionLevel = (float) ps.exhaustionCap;
        }
    }
}
