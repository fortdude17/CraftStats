package com.craftstats.common.network;

import com.craftstats.common.CraftStats;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.JsonUtil;
import com.craftstats.common.util.StatPersistence;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class CraftStatsNetwork {

    public static final ResourceLocation APPLY_MOB_STATS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "apply_mob_stats");
    public static final ResourceLocation APPLY_BLOCK_STATS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "apply_block_stats");
    public static final ResourceLocation APPLY_ITEM_STATS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "apply_item_stats");
    public static final ResourceLocation APPLY_PLAYER_STATS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "apply_player_stats");
    public static final ResourceLocation RESET_TARGET =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "reset_target");
    public static final ResourceLocation APPLY_BLOCK_POS_STATS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "apply_block_pos_stats");
    public static final ResourceLocation RESET_BLOCK_POS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "reset_block_pos");
    public static final ResourceLocation RESET_ALL =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "reset_all");
    public static final ResourceLocation APPLY_MOB_UUID_STATS =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "apply_mob_uuid_stats");
    public static final ResourceLocation RESET_MOB_UUID =
            ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, "reset_mob_uuid");

    public static void registerServerReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_MOB_STATS,
                (buf, ctx) -> {
                    String typeId = buf.readUtf();
                    String json   = buf.readUtf();
                    ctx.queue(() -> {
                        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
                        if (!hasPermission(player)) return;
                        ResourceLocation rl = ResourceLocation.parse(typeId);
                        MobStats stats = JsonUtil.mobStatsFromJson(json);
                        StatRegistry.setMob(rl, stats);

                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_BLOCK_STATS,
                (buf, ctx) -> {
                    String blockId = buf.readUtf();
                    String json    = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        StatRegistry.setBlock(ResourceLocation.parse(blockId),
                                JsonUtil.blockStatsFromJson(json));
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_ITEM_STATS,
                (buf, ctx) -> {
                    String itemId = buf.readUtf();
                    String json   = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        StatRegistry.setItem(ResourceLocation.parse(itemId),
                                JsonUtil.itemStatsFromJson(json));
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_PLAYER_STATS,
                (buf, ctx) -> {
                    String uuidStr = buf.readUtf();
                    String json    = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        UUID uuid = UUID.fromString(uuidStr);
                        PlayerStats stats = JsonUtil.playerStatsFromJson(json);
                        StatRegistry.setPlayer(uuid, stats);
                        ServerPlayer target = ((ServerPlayer) ctx.getPlayer()).getServer()
                                .getPlayerList().getPlayer(uuid);
                        if (target != null) applyAllPlayerAttributes(target, stats);
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESET_TARGET,
                (buf, ctx) -> {
                    String targetType = buf.readUtf();
                    String targetId   = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        switch (targetType) {
                            case "mob"    -> StatRegistry.removeMob(ResourceLocation.parse(targetId));
                            case "block"  -> StatRegistry.removeBlock(ResourceLocation.parse(targetId));
                            case "item"   -> StatRegistry.removeItem(ResourceLocation.parse(targetId));
                            case "player" -> StatRegistry.removePlayer(UUID.fromString(targetId));
                        }
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_BLOCK_POS_STATS,
                (buf, ctx) -> {
                    String posKey = buf.readUtf();
                    String json   = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        StatRegistry.setBlockAt(posKey, JsonUtil.blockStatsFromJson(json));
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESET_BLOCK_POS,
                (buf, ctx) -> {
                    String posKey = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        StatRegistry.removeBlockAt(posKey);
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_MOB_UUID_STATS,
                (buf, ctx) -> {
                    String uuidStr = buf.readUtf();
                    String json    = buf.readUtf();
                    ctx.queue(() -> {
                        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
                        if (!hasPermission(player)) return;
                        UUID uuid  = UUID.fromString(uuidStr);
                        MobStats stats = JsonUtil.mobStatsFromJson(json);
                        StatRegistry.setMobUuid(uuid, stats);
                        player.getServer().getAllLevels().forEach(level ->
                            level.getAllEntities().forEach(e -> {
                                if (e instanceof LivingEntity living && living.getUUID().equals(uuid))
                                    applyMobAttributesToEntity(living, stats);
                            })
                        );
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESET_MOB_UUID,
                (buf, ctx) -> {
                    String uuidStr = buf.readUtf();
                    ctx.queue(() -> {
                        if (!hasPermission((ServerPlayer) ctx.getPlayer())) return;
                        StatRegistry.removeMobUuid(UUID.fromString(uuidStr));
                        StatPersistence.save();
                    });
                });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESET_ALL,
                (buf, ctx) -> {
                    ctx.queue(() -> {
                        ServerPlayer player = (ServerPlayer) ctx.getPlayer();
                        if (!hasPermission(player)) return;
                        StatRegistry.init();
                        StatPersistence.save();

                        PlayerStats defaults = new PlayerStats();
                        player.getServer().getPlayerList().getPlayers()
                                .forEach(p -> applyAllPlayerAttributes(p, defaults));
                    });
                });
    }

    public static void registerClientReceivers() {}

    public static void sendApplyMobStats(String entityTypeId, MobStats stats) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(entityTypeId);
        buf.writeUtf(JsonUtil.toCompactJson(stats));
        NetworkManager.sendToServer(APPLY_MOB_STATS, buf);
    }

    public static void sendApplyBlockStats(String blockId, BlockStats stats) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(blockId);
        buf.writeUtf(JsonUtil.toCompactJson(stats));
        NetworkManager.sendToServer(APPLY_BLOCK_STATS, buf);
    }

    public static void sendApplyItemStats(String itemId, ItemStats stats) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(itemId);
        buf.writeUtf(JsonUtil.toCompactJson(stats));
        NetworkManager.sendToServer(APPLY_ITEM_STATS, buf);
    }

    public static void sendApplyPlayerStats(UUID playerUuid, PlayerStats stats) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(playerUuid.toString());
        buf.writeUtf(JsonUtil.toCompactJson(stats));
        NetworkManager.sendToServer(APPLY_PLAYER_STATS, buf);
    }

    public static void sendReset(String targetType, String targetId) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(targetType);
        buf.writeUtf(targetId);
        NetworkManager.sendToServer(RESET_TARGET, buf);
    }

    public static void sendApplyBlockPosStats(String posKey, BlockStats stats) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(posKey);
        buf.writeUtf(JsonUtil.toCompactJson(stats));
        NetworkManager.sendToServer(APPLY_BLOCK_POS_STATS, buf);
    }

    public static void sendResetBlockPos(String posKey) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(posKey);
        NetworkManager.sendToServer(RESET_BLOCK_POS, buf);
    }

    public static void sendApplyMobUuidStats(UUID entityUuid, MobStats stats) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(entityUuid.toString());
        buf.writeUtf(JsonUtil.toCompactJson(stats));
        NetworkManager.sendToServer(APPLY_MOB_UUID_STATS, buf);
    }

    public static void sendResetMobUuid(UUID entityUuid) {
        RegistryFriendlyByteBuf buf = buf();
        buf.writeUtf(entityUuid.toString());
        NetworkManager.sendToServer(RESET_MOB_UUID, buf);
    }

    public static void sendResetAll() {
        NetworkManager.sendToServer(RESET_ALL, buf());
    }

    private static RegistryFriendlyByteBuf buf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static boolean hasPermission(ServerPlayer player) {
        if (player == null) return false;

        if (!player.getServer().isDedicatedServer()) return true;
        return player.hasPermissions(2) || player.isCreative();
    }

    private static void applyMobStatsToLoadedEntities(ServerPlayer requestingPlayer,
                                                       ResourceLocation typeId, MobStats stats) {
        requestingPlayer.getServer().getAllLevels().forEach(level ->
                level.getAllEntities().forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        if (typeId.equals(EntityType.getKey(living.getType())))
                            applyMobAttributesToEntity(living, stats);
                    }
                })
        );
    }

    public static void applyMobAttributesToEntity(LivingEntity entity, MobStats stats) {

        if (stats.maxHealth >= 0)        setAttr(entity, Attributes.MAX_HEALTH,          stats.maxHealth);
        if (stats.attackDamage >= 0)     setAttr(entity, Attributes.ATTACK_DAMAGE,       stats.attackDamage);
        if (stats.armor >= 0)            setAttr(entity, Attributes.ARMOR,               stats.armor);
        if (stats.knockbackResist >= 0)  setAttr(entity, Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResist);
        if (stats.moveSpeed >= 0)        setAttr(entity, Attributes.MOVEMENT_SPEED,      stats.moveSpeed);
        if (stats.followRange >= 0)      setAttr(entity, Attributes.FOLLOW_RANGE,        stats.followRange);
        if (stats.sizeScale >= 0)        setAttr(entity, Attributes.SCALE,               stats.sizeScale);
        if (entity.getHealth() > entity.getMaxHealth())
            entity.setHealth(entity.getMaxHealth());
    }

    public static void applyAllPlayerAttributes(ServerPlayer target, PlayerStats stats) {

        setAttr(target, Attributes.MAX_HEALTH,                    stats.maxHealth);
        setAttr(target, Attributes.ATTACK_DAMAGE,                 stats.baseDamage);
        setAttr(target, Attributes.ATTACK_SPEED,                  stats.attackSpeed);
        setAttr(target, Attributes.MOVEMENT_SPEED,                stats.walkSpeed);
        setAttr(target, Attributes.FLYING_SPEED,                  stats.flySpeed);
        setAttr(target, Attributes.STEP_HEIGHT,                   stats.stepHeight);
        setAttr(target, Attributes.JUMP_STRENGTH,                 stats.jumpForce);
        setAttr(target, Attributes.ENTITY_INTERACTION_RANGE,      stats.reachDistance);
        setAttr(target, Attributes.BLOCK_INTERACTION_RANGE,       stats.reachDistance);
        setAttr(target, Attributes.OXYGEN_BONUS, stats.drownImmune ? 10_000.0 : 0.0);

        setAttr(target, Attributes.ARMOR,                         stats.armor);
        setAttr(target, Attributes.ARMOR_TOUGHNESS,               stats.armorToughness);
        setAttr(target, Attributes.KNOCKBACK_RESISTANCE,          stats.knockbackResistance);
        setAttr(target, Attributes.LUCK,                          stats.luck);
        setAttr(target, Attributes.ATTACK_KNOCKBACK,              stats.attackKnockback);

        setAttr(target, Attributes.GRAVITY,                       stats.gravity);
        setAttr(target, Attributes.FALL_DAMAGE_MULTIPLIER,        stats.fallDamageMultiplier);
        setAttr(target, Attributes.SAFE_FALL_DISTANCE,            stats.safeFallDistance);
        setAttr(target, Attributes.BURNING_TIME,                  stats.burningTime);
        setAttr(target, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, stats.explosionKbResistance);
        setAttr(target, Attributes.MINING_EFFICIENCY,             stats.miningEfficiency);
        setAttr(target, Attributes.MOVEMENT_EFFICIENCY,           stats.movementEfficiency);
        setAttr(target, Attributes.SNEAKING_SPEED,                stats.sneakingSpeed);
        setAttr(target, Attributes.SUBMERGED_MINING_SPEED,        stats.submergedMiningSpeed);
        setAttr(target, Attributes.SWEEPING_DAMAGE_RATIO,         stats.sweepingDamageRatio);
        setAttr(target, Attributes.WATER_MOVEMENT_EFFICIENCY,     stats.waterMovementEfficiency);
        setAttr(target, Attributes.MAX_ABSORPTION,                stats.maxAbsorption);

        if (target.getHealth() > target.getMaxHealth())
            target.setHealth(target.getMaxHealth());
    }

    private static void setAttr(LivingEntity entity,
                                  net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                  double value) {
        var instance = entity.getAttribute(attr);
        if (instance != null) instance.setBaseValue(value);
    }
}
