package com.craftstats.common;

import com.craftstats.common.command.CraftStatsCommands;
import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.item.ModItems;
import com.craftstats.common.network.CraftStatsNetwork;
import com.craftstats.common.preset.PresetManager;
import com.craftstats.common.registry.ModCreativeTab;
import com.craftstats.common.stats.*;
import com.craftstats.common.util.StatPersistence;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CraftStats {

    public static final String MOD_ID = "craftstats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        CraftStatsConfig.init();
        StatRegistry.init();
        ModItems.register();
        ModCreativeTab.register();
        CraftStatsNetwork.registerServerReceivers();
        PresetManager.init();

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                CraftStatsCommands.register(dispatcher));

        LifecycleEvent.SERVER_STARTED.register(server -> StatPersistence.init(server));
        LifecycleEvent.SERVER_STOPPING.register(server -> StatPersistence.save());

        EntityEvent.ADD.register((entity, level) -> {
            if (level.isClientSide()) return EventResult.pass();
            if (!(entity instanceof LivingEntity living) || living instanceof Player) return EventResult.pass();

            MobStats uuidStats = StatRegistry.getMobUuid(living.getUUID());
            if (uuidStats != null) {
                entity.getServer().execute(() -> CraftStatsNetwork.applyMobAttributesToEntity(living, uuidStats));
                return EventResult.pass();
            }
            ResourceLocation typeId = EntityType.getKey(living.getType());
            if (typeId == null) return EventResult.pass();
            MobStats stats = StatRegistry.getMob(typeId);
            if (stats != null) {

                entity.getServer().execute(() -> CraftStatsNetwork.applyMobAttributesToEntity(living, stats));
            }
            return EventResult.pass();
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!(player instanceof ServerPlayer sp)) return;
            sp.getServer().execute(() -> {

                PlayerStats ps = StatRegistry.getPlayer(sp.getUUID());
                if (ps != null) CraftStatsNetwork.applyAllPlayerAttributes(sp, ps);

                if (canAutoGive(sp) && !alreadyHasBook(sp)) {
                    sp.getInventory().add(new ItemStack(ModItems.PLAYER_STATS_BOOK.get()));
                }
            });
        });

        LOGGER.info("CraftStats initialised.");
    }

    private static boolean canAutoGive(ServerPlayer player) {
        if (!player.getServer().isDedicatedServer()) return true;
        return player.hasPermissions(2);
    }

    private static boolean alreadyHasBook(ServerPlayer player) {
        for (ItemStack s : player.getInventory().items)
            if (s.is(ModItems.PLAYER_STATS_BOOK.get())) return true;
        for (ItemStack s : player.getInventory().offhand)
            if (s.is(ModItems.PLAYER_STATS_BOOK.get())) return true;
        return false;
    }
}
