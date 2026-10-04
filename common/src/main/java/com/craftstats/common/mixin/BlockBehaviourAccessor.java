package com.craftstats.common.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface BlockBehaviourAccessor {
    @Accessor("properties")
    BlockBehaviour.Properties craftstats$getBlockProperties();

    // Read the fields directly: the getters are overridden by CraftStats itself.
    @Accessor("friction")
    float craftstats$getFriction();

    @Accessor("explosionResistance")
    float craftstats$getExplosionResistance();
}
