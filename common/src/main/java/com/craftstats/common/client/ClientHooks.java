package com.craftstats.common.client;

import com.craftstats.common.CraftStats;
import com.craftstats.common.gui.CraftStatsScreen;
import com.craftstats.common.gui.RandomizeScreen;
import com.craftstats.common.gui.editor.TargetEntries;
import com.craftstats.common.item.ModItems;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.Compat;
import com.craftstats.common.util.Permissions;
import com.craftstats.common.util.StatPersistence;
import com.google.gson.JsonParser;
import dev.architectury.event.EventResult;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Client-only code. Never call these from logic that can run on a dedicated server. */
public final class ClientHooks {

    private ClientHooks() {}

    /** True when this client hosts the world (single player or LAN host). */
    public static boolean isLocalHost() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    public static boolean mayEdit() {
        return Permissions.mayEditClient(Minecraft.getInstance().player, isLocalHost());
    }

    // ---- sync ------------------------------------------------------------------------------

    private static int lastRenderHash;

    public static void onSync(byte[] data) {
        // The local host shares its registry with the integrated server already.
        if (!isLocalHost()) {
            try {
                StatPersistence.restore(JsonParser.parseString(CraftStatsNetwork.decodeSnapshot(data)).getAsJsonObject());
            } catch (Exception e) {
                CraftStats.LOGGER.error("CraftStats: could not read stats sent by the server", e);
            }
        }
        // Invisible blocks and light levels only show after the chunks are drawn again.
        int hash = renderHash();
        if (hash != lastRenderHash) {
            lastRenderHash = hash;
            if (Minecraft.getInstance().level != null) Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    private static int renderHash() {
        int h = 1;
        for (var e : StatRegistry.allBlocks().entrySet())
            if (e.getValue().invisible || e.getValue().lightEmission != null)
                h = 31 * h + (e.getKey().hashCode() ^ (e.getValue().invisible ? 1 : 0) ^ java.util.Objects.hashCode(e.getValue().lightEmission));
        return h;
    }

    public static void onDisconnect() {
        if (!isLocalHost()) StatRegistry.clear();
        TargetEntries.invalidate();
    }

    // ---- opening screens ---------------------------------------------------------------------

    public static void openBlockEditor(Block block) {
        Minecraft.getInstance().setScreen(new CraftStatsScreen(TargetType.BLOCK, BuiltInRegistries.BLOCK.getKey(block).toString()));
    }

    public static void openBlockEditorAt(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        Minecraft.getInstance().setScreen(new CraftStatsScreen(StatRegistry.makePosKey(level.dimension(), pos), block, pos));
    }

    public static void openMobEditor(LivingEntity entity) {
        Minecraft.getInstance().setScreen(new CraftStatsScreen(entity));
    }

    public static void openMobTypeEditor(String entityTypeId) {
        Minecraft.getInstance().setScreen(new CraftStatsScreen(TargetType.MOB, entityTypeId));
    }

    public static void openPlayerEditor(Player player) {
        Minecraft.getInstance().setScreen(new CraftStatsScreen(TargetType.PLAYER, player.getUUID().toString()));
    }

    public static void openRandomizeMob(LivingEntity entity) {
        if (!com.craftstats.common.config.CraftStatsConfig.get().enableRandomize) {
            actionBar(Component.literal("Randomize is disabled in the CraftStats config.").withStyle(ChatFormatting.RED));
            return;
        }
        String id = EntityType.getKey(entity.getType()).toString();
        CraftStatsScreen editor = new CraftStatsScreen(TargetType.MOB, id);
        Minecraft.getInstance().setScreen(new RandomizeScreen(editor, TargetType.MOB, entity.getType().getDescription().getString(),
                VanillaStats.mob(entity.getType()), editor::loadStats));
    }

    // ---- Craft Wand left-click: copy stats ---------------------------------------------------

    public static EventResult onAttackEntity(Player player, Level level, Entity target, InteractionHand hand,
                                             EntityHitResult hit) {
        if (!level.isClientSide() || !player.getItemInHand(hand).is(ModItems.CRAFT_WAND.get())
                || !(target instanceof LivingEntity living)) return EventResult.pass();
        if (living instanceof Player targetPlayer) {
            PlayerStats ps = StatRegistry.forPlayer(targetPlayer);
            Minecraft.getInstance().keyboardHandler.setClipboard(StatSchema.toPrettyJson(ps != null ? ps : new PlayerStats()));
        } else {
            MobStats stats = StatRegistry.forEntity(living);
            Minecraft.getInstance().keyboardHandler.setClipboard(
                    StatSchema.toPrettyJson(stats != null ? stats : VanillaStats.mob(living.getType())));
        }
        actionBar(Component.literal("Copied " + target.getName().getString()
                + "'s stats to the clipboard.").withStyle(ChatFormatting.GREEN));
        return EventResult.interruptFalse(); // don't actually hit the mob with the wand
    }

    public static void actionBar(Component message) {
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    public static void chat(Component message) {
        Minecraft.getInstance().gui.getChat().addMessage(message);
    }

    // ---- tooltips ----------------------------------------------------------------------------

    public static void appendWandTooltip(Consumer<Component> lines) {
        lines.accept(Component.literal("Right-click block: edit this block").withStyle(ChatFormatting.AQUA));
        lines.accept(Component.literal("Sneak + right-click block: edit the block type").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Right-click mob: edit that mob").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Sneak + right-click mob: randomize its type").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Right-click player: edit their stats").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Spawn egg in off-hand + right-click: edit that mob type").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Right-click air: edit your own stats").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Left-click mob: copy its stats as JSON").withStyle(ChatFormatting.GRAY));

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.crosshairPickEntity instanceof LivingEntity le && !(le instanceof Player)) {
            ResourceLocation typeId = EntityType.getKey(le.getType());
            MobStats ms = StatRegistry.forEntity(le);
            lines.accept(Component.empty());
            lines.accept(ms != null
                    ? Component.literal("★ " + typeId.getPath() + ": custom stats active").withStyle(ChatFormatting.YELLOW)
                    : Component.literal("Looking at: " + typeId.getPath()).withStyle(ChatFormatting.DARK_GRAY));
        } else if (mc.hitResult instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            Block block = mc.level.getBlockState(pos).getBlock();
            BlockStats here = StatRegistry.getBlockAt(StatRegistry.makePosKey(mc.level.dimension(), pos));
            BlockStats type = StatRegistry.forBlock(block);
            if (here != null || type != null) {
                lines.accept(Component.empty());
                if (here != null) lines.accept(Component.literal("★ This block has custom stats").withStyle(ChatFormatting.AQUA));
                if (type != null) lines.accept(Component.literal("★ " + BuiltInRegistries.BLOCK.getKey(block).getPath()
                        + ": type stats active").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    public static void appendBookTooltip(Consumer<Component> lines) {
        lines.accept(Component.literal("Right-click to edit your stats").withStyle(ChatFormatting.GRAY));
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        PlayerStats ps = StatRegistry.forPlayer(player);
        lines.accept(Component.empty());
        if (ps == null) {
            lines.accept(Component.literal("No overrides: vanilla stats").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.accept(Component.literal("Current overrides:").withStyle(ChatFormatting.YELLOW));
        lines.accept(Component.literal("  HP " + fmt(ps.maxHealth) + "  Dmg " + fmt(ps.baseDamage)
                + "  Speed " + fmt(ps.walkSpeed)).withStyle(ChatFormatting.WHITE));
        for (String flag : List.of(ps.godMode ? "God Mode" : "", ps.noFallDamage ? "No Fall Damage" : "",
                ps.keepInventory ? "Keep Inventory" : "", ps.fireImmune ? "Fire Immune" : "", ps.noClip ? "No Clip" : ""))
            if (!flag.isEmpty()) lines.accept(Component.literal("  ★ " + flag).withStyle(ChatFormatting.GOLD));
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Players in the tab list, as "uuid | name". */
    public static List<String> onlinePlayers() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return List.of();
        return connection.getOnlinePlayers().stream()
                .map(info -> Compat.profileId(info.getProfile()) + " | " + Compat.profileName(info.getProfile()))
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(s -> s))
                .toList();
    }

    public static UUID localPlayerId() {
        Player p = Minecraft.getInstance().player;
        return p != null ? p.getUUID() : null;
    }
}
