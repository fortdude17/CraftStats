package com.craftstats.common.mixin;

import com.craftstats.common.logic.Effects;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.ItemStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.stats.WorldStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Never Despawns (items), the world's item despawn time and "Deletes Dropped Items" blocks. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow private int age;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void craftstats$tick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || self.isRemoved()) return;
        if (this.age == -32768) return; // already unlimited
        ItemStats s = StatRegistry.forItem(self.getItem().getItem());
        if (s != null && s.neverDespawns) {
            self.setUnlimitedLifetime();
            return;
        }
        WorldStats w = StatRegistry.world();
        if (w != null && w.itemDespawnSeconds != null) {
            int limit = Math.max(10, w.itemDespawnSeconds) * 20;
            if (self.tickCount >= limit) { self.discard(); ci.cancel(); return; }
            if (limit > 6000 && this.age > 5900) this.age = 5900; // keep vanilla from removing it early
        }
        if (self.tickCount % 10 == 0 && StatRegistry.hasBlockOverrides() && self.onGround()) {
            BlockPos below = Effects.below(self);
            BlockStats b = StatRegistry.forBlockAt(self.level().getBlockState(below).getBlock(), self.level(), below);
            if (b != null && b.itemVoid) { self.discard(); ci.cancel(); }
        }
    }
}
