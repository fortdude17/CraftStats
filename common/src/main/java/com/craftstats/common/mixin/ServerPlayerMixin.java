package com.craftstats.common.mixin;

import com.craftstats.common.logic.ItemHooks;
import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /**
     * Keep Inventory: the death drop is cancelled in LivingEntityMixin; here the items and
     * XP are carried over to the respawned player (vanilla only does that for the gamerule).
     */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void craftstats$keepInventory(ServerPlayer old, boolean keepEverything, CallbackInfo ci) {
        if (keepEverything) return;
        ServerPlayer self = (ServerPlayer) (Object) this;
        ItemHooks.restoreSoulbound(self);
        PlayerStats ps = StatRegistry.forPlayer(old);
        if (ps == null) return;
        if (ps.keepInventory) self.getInventory().replaceWith(old.getInventory());
        if (ps.keepInventory || ps.keepXp) {
            self.experienceLevel = old.experienceLevel;
            self.totalExperience = old.totalExperience;
            self.experienceProgress = old.experienceProgress;
        }
    }
}
