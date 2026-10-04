package com.craftstats.common;

import com.craftstats.common.command.CraftStatsCommands;
import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.item.ModItems;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.registry.ModCreativeTab;
import com.craftstats.common.stats.StatApplier;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.Permissions;
import com.craftstats.common.util.StatPersistence;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CraftStats {

    public static final String MOD_ID = "craftstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String BOOK_GIVEN_TAG = "craftstats_book_given";

    private CraftStats() {}

    public static void init() {
        CraftStatsConfig.init();
        ModItems.register();
        ModCreativeTab.register();
        CraftStatsNetwork.registerCommon();
        PresetManager.init();

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                CraftStatsCommands.register(dispatcher));

        // Load before the levels so mobs in spawn chunks get their overrides.
        LifecycleEvent.SERVER_STARTING.register(StatPersistence::init);
        LifecycleEvent.SERVER_STARTED.register(server -> StatApplier.refreshMobs(server, null));
        LifecycleEvent.SERVER_STOPPING.register(server -> StatPersistence.save());
        LifecycleEvent.SERVER_STOPPED.register(server -> StatPersistence.shutdown());

        EntityEvent.ADD.register((entity, level) -> {
            if (!level.isClientSide() && entity instanceof LivingEntity living && !(living instanceof Player)
                    && StatRegistry.forEntity(living) != null) {
                MinecraftServer server = level.getServer();
                if (server != null) server.execute(() -> StatApplier.applyMob(living));
            }
            return EventResult.pass();
        });

        // Left-clicking with the wand copies stats (client side) instead of attacking.
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) ->
                !level.isClientSide() && player.getItemInHand(hand).is(ModItems.CRAFT_WAND.get())
                        ? EventResult.interruptFalse() : EventResult.pass());

        PlayerEvent.PLAYER_JOIN.register(player -> {
            StatApplier.applyPlayer(player);
            CraftStatsNetwork.sendSync(player);
            giveBookOnce(player);
        });
        // A respawned player is a new entity; transient modifiers have to be re-applied.
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> StatApplier.applyPlayer(player));

        LOGGER.info("CraftStats initialised.");
    }

    /** Hand the player-stats book to editors once, the first time they join a world. */
    private static void giveBookOnce(ServerPlayer player) {
        if (player.getTags().contains(BOOK_GIVEN_TAG) || !Permissions.mayEdit(player)) return;
        player.addTag(BOOK_GIVEN_TAG);
        if (!player.getInventory().contains(s -> s.is(ModItems.PLAYER_STATS_BOOK.get())))
            player.getInventory().add(new ItemStack(ModItems.PLAYER_STATS_BOOK.get()));
    }
}
