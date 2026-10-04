package com.craftstats.common.mixin;

import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 1.21.2
//$ import net.minecraft.server.level.ServerPlayer;
//#else
import net.minecraft.world.entity.player.Player;
//#endif

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;

    @Unique private int craftstats$regenThreshold = 18;

    //#if MC >= 1.21.2
    //$ @Inject(method = "tick", at = @At("HEAD"))
    //$ private void craftstats$preTick(ServerPlayer player, CallbackInfo ci) {
    //#else
    @Inject(method = "tick", at = @At("HEAD"))
    private void craftstats$preTick(Player player, CallbackInfo ci) {
    //#endif
        PlayerStats ps = StatRegistry.forPlayer(player);
        craftstats$regenThreshold = ps != null ? ps.regenThreshold : 18;
        if (ps == null) return;
        int max = Math.max(0, ps.maxFoodLevel);
        if (foodLevel > max) foodLevel = max;
        if (saturationLevel > max) saturationLevel = max;
    }

    /** Natural regeneration starts at this food level (vanilla 18). */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 18), require = 0)
    private int craftstats$regenThreshold(int vanilla) {
        return craftstats$regenThreshold;
    }
}
