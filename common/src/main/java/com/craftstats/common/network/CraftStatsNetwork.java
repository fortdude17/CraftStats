package com.craftstats.common.network;

import com.craftstats.common.CraftStats;
import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import com.craftstats.common.util.Permissions;
import com.craftstats.common.util.StatPersistence;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Client → server: apply / reset / reset-all requests (validated and permission-checked).
 * Server → client: a compressed snapshot of every override, sent on join and after each change.
 */
public final class CraftStatsNetwork {

    public static final ResourceLocation APPLY     = id("apply");
    public static final ResourceLocation RESET     = id("reset");
    public static final ResourceLocation RESET_ALL = id("reset_all");
    public static final ResourceLocation SYNC      = id("sync");

    private static final int MAX_SYNC_BYTES = 4 * 1024 * 1024;
    private static final Pattern POS_KEY = Pattern.compile("^([a-z0-9_.-]+:[a-z0-9_./-]+)\\|(-?\\d+)\\|(-?\\d+)\\|(-?\\d+)$");

    /** What a request targets. */
    public enum Kind {
        MOB(TargetType.MOB), MOB_INSTANCE(TargetType.MOB), BLOCK(TargetType.BLOCK), BLOCK_POS(TargetType.BLOCK),
        ITEM(TargetType.ITEM), PLAYER(TargetType.PLAYER), PROJECTILE(TargetType.PROJECTILE),
        ENCHANTMENT(TargetType.ENCHANTMENT), WORLD(TargetType.WORLD);

        /** The kind used for a whole target type (not a single mob or block position). */
        public static Kind of(TargetType type) {
            return switch (type) {
                case MOB -> MOB;
                case BLOCK -> BLOCK;
                case ITEM -> ITEM;
                case PLAYER -> PLAYER;
                case PROJECTILE -> PROJECTILE;
                case ENCHANTMENT -> ENCHANTMENT;
                case WORLD -> WORLD;
            };
        }

        public final TargetType type;
        Kind(TargetType type) { this.type = type; }
    }

    private CraftStatsNetwork() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CraftStats.MOD_ID, path);
    }

    // ---- registration --------------------------------------------------------------------

    public static void registerCommon() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY, (buf, ctx) -> {
            Kind kind = buf.readEnum(Kind.class);
            String key = buf.readUtf(512);
            String json = buf.readUtf();
            ctx.queue(() -> handleApply((ServerPlayer) ctx.getPlayer(), kind, key, json));
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESET, (buf, ctx) -> {
            Kind kind = buf.readEnum(Kind.class);
            String key = buf.readUtf(512);
            ctx.queue(() -> handleReset((ServerPlayer) ctx.getPlayer(), kind, key));
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESET_ALL, (buf, ctx) ->
                ctx.queue(() -> handleResetAll((ServerPlayer) ctx.getPlayer())));

        // On a dedicated server the S2C type must be registered here; clients register it
        // together with their receiver in registerClient().
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(SYNC);
    }

    public static void registerClient(Consumer<byte[]> onSync) {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC, (buf, ctx) -> {
            byte[] data = buf.readByteArray(MAX_SYNC_BYTES);
            ctx.queue(() -> onSync.accept(data));
        });
    }

    // ---- client → server -------------------------------------------------------------------

    public static void sendApply(Kind kind, String key, Object stats) {
        RegistryFriendlyByteBuf buf = clientBuf();
        buf.writeEnum(kind);
        buf.writeUtf(key, 512);
        buf.writeUtf(StatSchema.toJson(stats));
        NetworkManager.sendToServer(APPLY, buf);
    }

    public static void sendReset(Kind kind, String key) {
        RegistryFriendlyByteBuf buf = clientBuf();
        buf.writeEnum(kind);
        buf.writeUtf(key, 512);
        NetworkManager.sendToServer(RESET, buf);
    }

    public static void sendResetAll() {
        NetworkManager.sendToServer(RESET_ALL, clientBuf());
    }

    private static RegistryFriendlyByteBuf clientBuf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    // ---- server handlers -------------------------------------------------------------------

    private static void handleApply(ServerPlayer player, Kind kind, String key, String json) {
        MinecraftServer server = Compat.server(player);
        if (server == null || !checkAccess(player, kind)) return;
        try {
            Object parsed = StatSchema.parse(kind.type, json);
            StatAccess.sanitize(kind.type, parsed);
            switch (kind) {
                case MOB -> {
                    ResourceLocation id = registered(BuiltInRegistries.ENTITY_TYPE, key);
                    if (blacklisted(player, id.toString())) return;
                    StatRegistry.setMob(id, (MobStats) parsed);
                    StatApplier.refreshMobs(server, id);
                }
                case MOB_INSTANCE -> {
                    UUID uuid = UUID.fromString(key);
                    Entity entity = findEntity(server, uuid);
                    if (entity != null && blacklisted(player, EntityType.getKey(entity.getType()).toString())) return;
                    StatRegistry.setMobInstance(uuid, (MobStats) parsed);
                    StatApplier.refreshMob(server, uuid);
                }
                case BLOCK -> {
                    ResourceLocation id = registered(BuiltInRegistries.BLOCK, key);
                    if (blacklisted(player, id.toString())) return;
                    StatRegistry.setBlock(id, (BlockStats) parsed);
                }
                case BLOCK_POS -> {
                    BlockStats stats = (BlockStats) parsed;
                    stats.block = blockAt(server, key);
                    if (blacklisted(player, stats.block)) return;
                    StatRegistry.setBlockAt(key, stats);
                }
                case ITEM -> {
                    ResourceLocation id = registered(BuiltInRegistries.ITEM, key);
                    if (blacklisted(player, id.toString())) return;
                    StatRegistry.setItem(id, (ItemStats) parsed);
                }
                case PLAYER -> {
                    UUID uuid = UUID.fromString(key);
                    StatRegistry.setPlayer(uuid, (PlayerStats) parsed);
                    StatApplier.refreshPlayer(server, uuid);
                }
                case PROJECTILE -> {
                    ResourceLocation id = registered(BuiltInRegistries.ENTITY_TYPE, key);
                    if (blacklisted(player, id.toString())) return;
                    StatRegistry.setProjectile(id, (ProjectileStats) parsed);
                }
                case ENCHANTMENT -> {
                    ResourceLocation id = registered(Compat.enchantments(server.registryAccess()), key);
                    if (blacklisted(player, id.toString())) return;
                    StatRegistry.setEnchantment(id, (EnchantmentStats) parsed);
                }
                case WORLD -> {
                    StatRegistry.setWorld((WorldStats) parsed);
                    StatApplier.refreshMobs(server, null);
                    StatApplier.refreshAllPlayers(server);
                }
            }
        } catch (Exception e) {
            deny(player, "Rejected invalid " + kind.type.displayName().toLowerCase() + " data: " + e.getMessage());
            return;
        }
        changed(server);
    }

    private static void handleReset(ServerPlayer player, Kind kind, String key) {
        MinecraftServer server = Compat.server(player);
        if (server == null || !checkAccess(player, kind)) return;
        try {
            switch (kind) {
                case MOB -> {
                    ResourceLocation id = ResourceLocation.parse(key);
                    StatRegistry.removeMob(id);
                    StatApplier.refreshMobs(server, id);
                }
                case MOB_INSTANCE -> {
                    UUID uuid = UUID.fromString(key);
                    StatRegistry.removeMobInstance(uuid);
                    StatApplier.refreshMob(server, uuid);
                }
                case BLOCK     -> StatRegistry.removeBlock(ResourceLocation.parse(key));
                case BLOCK_POS -> StatRegistry.removeBlockAt(key);
                case ITEM      -> StatRegistry.removeItem(ResourceLocation.parse(key));
                case PLAYER -> {
                    UUID uuid = UUID.fromString(key);
                    StatRegistry.removePlayer(uuid);
                    StatApplier.refreshPlayer(server, uuid);
                }
                case PROJECTILE  -> StatRegistry.removeProjectile(ResourceLocation.parse(key));
                case ENCHANTMENT -> StatRegistry.removeEnchantment(ResourceLocation.parse(key));
                case WORLD -> {
                    StatRegistry.removeWorld();
                    StatApplier.refreshMobs(server, null);
                    StatApplier.refreshAllPlayers(server);
                }
            }
        } catch (Exception e) {
            deny(player, "Invalid reset request: " + e.getMessage());
            return;
        }
        changed(server);
    }

    private static void handleResetAll(ServerPlayer player) {
        MinecraftServer server = Compat.server(player);
        if (server == null) return;
        if (!Permissions.mayEdit(player)) { deny(player, "You don't have permission to use CraftStats."); return; }
        resetAll(server);
    }

    /** Removes every override in the world and restores loaded mobs and online players. */
    public static void resetAll(MinecraftServer server) {
        StatRegistry.clear();
        StatApplier.refreshMobs(server, null);
        StatApplier.refreshAllPlayers(server);
        changed(server);
    }

    /** Persist, then push the new state to every client. */
    public static void changed(MinecraftServer server) {
        StatPersistence.save();
        broadcastSync(server);
    }

    // ---- server → client -------------------------------------------------------------------

    public static void broadcastSync(MinecraftServer server) {
        byte[] data = encodeSnapshot();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendSync(p, data);
    }

    public static void sendSync(ServerPlayer player) {
        sendSync(player, encodeSnapshot());
    }

    private static void sendSync(ServerPlayer player, byte[] data) {
        if (!NetworkManager.canPlayerReceive(player, SYNC)) return; // client without CraftStats
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        buf.writeByteArray(data);
        NetworkManager.sendToPlayer(player, SYNC, buf);
    }

    private static byte[] encodeSnapshot() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(StatPersistence.snapshot().toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    public static String decodeSnapshot(byte[] data) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ---- validation --------------------------------------------------------------------------

    private static boolean checkAccess(ServerPlayer player, Kind kind) {
        if (player == null) return false;
        if (!Permissions.mayEdit(player)) {
            deny(player, "You don't have permission to use CraftStats.");
            return false;
        }
        if (!CraftStatsConfig.get().isEnabled(kind.type)) {
            deny(player, "The " + kind.type.displayName().toLowerCase() + " editor is disabled on this server.");
            return false;
        }
        return true;
    }

    private static boolean blacklisted(ServerPlayer player, String id) {
        if (!CraftStatsConfig.get().isBlacklisted(id)) return false;
        deny(player, id + " is blacklisted in the CraftStats config.");
        return true;
    }

    private static <T> ResourceLocation registered(Registry<T> registry, String key) {
        ResourceLocation id = ResourceLocation.tryParse(key);
        if (id == null || !registry.containsKey(id)) throw new IllegalArgumentException("unknown id '" + key + "'");
        return id;
    }

    /** Validates a position key and returns the id of the block currently there. */
    private static String blockAt(MinecraftServer server, String key) {
        Matcher m = POS_KEY.matcher(key);
        if (!m.matches()) throw new IllegalArgumentException("bad block position '" + key + "'");
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(m.group(1)));
        ServerLevel level = server.getLevel(dim);
        if (level == null) throw new IllegalArgumentException("unknown dimension " + m.group(1));
        BlockPos pos = new BlockPos(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }

    private static void deny(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("CraftStats: " + message).withStyle(ChatFormatting.RED));
    }
}
