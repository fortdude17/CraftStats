package com.craftstats.common.item;

import com.craftstats.common.stats.PlayerStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.ScreenOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class PlayerStatsBookItem extends Item {

    public PlayerStatsBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            ScreenOpener.openPlayerEditor(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                 List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Right-click to edit your stats").withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                PlayerStats ps = StatRegistry.getPlayer(mc.player.getUUID());
                if (ps != null) {
                    lines.add(Component.literal("Current overrides:").withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.literal("  HP: " + ps.maxHealth
                            + "  Dmg: " + ps.baseDamage
                            + "  Speed: " + ps.walkSpeed)
                            .withStyle(ChatFormatting.WHITE));
                    if (ps.godMode)        lines.add(Component.literal("  ★ God Mode").withStyle(ChatFormatting.GOLD));
                    if (ps.noFallDamage)   lines.add(Component.literal("  ★ No Fall Damage").withStyle(ChatFormatting.AQUA));
                    if (ps.keepInventory)  lines.add(Component.literal("  ★ Keep Inventory").withStyle(ChatFormatting.GREEN));
                    if (ps.fireImmune)     lines.add(Component.literal("  ★ Fire Immune").withStyle(ChatFormatting.RED));
                    if (ps.infiniteItems)  lines.add(Component.literal("  ★ Infinite Items").withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    lines.add(Component.literal("No active overrides — vanilla stats")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        } catch (Exception ignored) {}
    }
}
