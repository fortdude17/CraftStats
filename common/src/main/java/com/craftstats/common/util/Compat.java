package com.craftstats.common.util;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
//#if MC >= 1.21.11
//$ import net.minecraft.server.permissions.Permissions;
//$ import net.minecraft.server.players.NameAndId;
//#endif

import java.util.Optional;

/** Small wrappers around Minecraft APIs that differ between the supported versions. */
public final class Compat {

    private Compat() {}

    //#if MC >= 1.21.5
    //$ public static final Holder<MobEffect> SPEED    = MobEffects.SPEED;
    //$ public static final Holder<MobEffect> SLOWNESS = MobEffects.SLOWNESS;
    //$ public static final Holder<MobEffect> HASTE    = MobEffects.HASTE;
    //$ public static final Holder<MobEffect> STRENGTH = MobEffects.STRENGTH;
    //#else
    public static final Holder<MobEffect> SPEED    = MobEffects.MOVEMENT_SPEED;
    public static final Holder<MobEffect> SLOWNESS = MobEffects.MOVEMENT_SLOWDOWN;
    public static final Holder<MobEffect> HASTE    = MobEffects.DIG_SPEED;
    public static final Holder<MobEffect> STRENGTH = MobEffects.DAMAGE_BOOST;
    //#endif

    public static Optional<Holder<MobEffect>> effect(ResourceLocation id) {
        //#if MC >= 1.21.2
        //$ MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(id);
        //#else
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        //#endif
        return effect == null ? Optional.empty() : Optional.of(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
    }

    public static <T> T registryValue(net.minecraft.core.Registry<T> registry, ResourceLocation id) {
        //#if MC >= 1.21.2
        //$ return registry.getValue(id);
        //#else
        return registry.get(id);
        //#endif
    }

    public static ResourceLocation keyId(ResourceKey<?> key) {
        //#if MC >= 1.21.11
        //$ return key.identifier();
        //#else
        return key.location();
        //#endif
    }

    public static Registry<Enchantment> enchantments(RegistryAccess access) {
        //#if MC >= 1.21.2
        //$ return access.lookupOrThrow(Registries.ENCHANTMENT);
        //#else
        return access.registryOrThrow(Registries.ENCHANTMENT);
        //#endif
    }

    /** A new entity that has not been added to the level yet (null if the type can't be created). */
    public static Entity createEntity(net.minecraft.world.entity.EntityType<?> type, Level level) {
        //#if MC >= 1.21.2
        //$ return type.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        //#else
        return type.create(level);
        //#endif
    }

    public static MinecraftServer server(Entity entity) {
        return entity.level().getServer();
    }

    /** Operator permission level 2 (game master). */
    public static boolean isGameMaster(ServerPlayer player) {
        //#if MC >= 1.21.11
        //$ return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //#else
        return player.hasPermissions(2);
        //#endif
    }

    /** Client-side guess of operator status (the server re-checks every request). */
    public static boolean isGameMasterClient(Player player) {
        //#if MC >= 1.21.11
        //$ return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //#else
        return player.hasPermissions(2);
        //#endif
    }

    public static boolean isSingleplayerOwner(ServerPlayer player) {
        MinecraftServer server = server(player);
        if (server == null) return false;
        //#if MC >= 1.21.11
        //$ return server.isSingleplayerOwner(new NameAndId(player.getGameProfile()));
        //#else
        return server.isSingleplayerOwner(player.getGameProfile());
        //#endif
    }

    public static boolean isDaytime(Level level) {
        //#if MC >= 1.21.11
        //$ return level.isBrightOutside();
        //#else
        return level.isDay();
        //#endif
    }

    /** Server-side damage that works on every supported version. */
    public static void hurt(Entity target, DamageSource source, float amount) {
        //#if MC >= 1.21.2
        //$ if (target.level() instanceof ServerLevel level) target.hurtServer(level, source, amount);
        //#else
        target.hurt(source, amount);
        //#endif
    }

    public static String profileName(com.mojang.authlib.GameProfile profile) {
        //#if MC >= 1.21.9
        //$ return profile.name();
        //#else
        return profile.getName();
        //#endif
    }

    public static java.util.UUID profileId(com.mojang.authlib.GameProfile profile) {
        //#if MC >= 1.21.9
        //$ return profile.id();
        //#else
        return profile.getId();
        //#endif
    }
}
