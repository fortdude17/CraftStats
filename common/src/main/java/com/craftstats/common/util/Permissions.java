package com.craftstats.common.util;

import com.craftstats.common.config.CraftStatsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Who may use the editor. The server's decision is authoritative; the client only uses
 * {@link #mayEditClient} to decide whether to show buttons.
 *
 * <ul>
 *   <li>The single-player host and operators (level 2) may always edit.</li>
 *   <li>With {@code requireOp=false}, creative players may edit too, and with
 *       {@code allowSurvival=true} everyone may.</li>
 * </ul>
 */
public final class Permissions {

    private Permissions() {}

    public static boolean mayEdit(ServerPlayer player) {
        if (player == null) return false;
        if (Compat.isSingleplayerOwner(player) || Compat.isGameMaster(player)) return true;
        return mayEditWithoutOp(player);
    }

    /** Client-side approximation; {@code isLocalHost} is true when this client runs the world. */
    public static boolean mayEditClient(Player player, boolean isLocalHost) {
        if (player == null) return false;
        if (isLocalHost || Compat.isGameMasterClient(player)) return true;
        return mayEditWithoutOp(player);
    }

    private static boolean mayEditWithoutOp(Player player) {
        CraftStatsConfig.ConfigData cfg = CraftStatsConfig.get();
        if (cfg.requireOp) return false;
        return player.isCreative() || cfg.allowSurvival;
    }
}
