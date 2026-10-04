package com.craftstats.common.item;

import com.craftstats.common.client.ClientHooks;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
//#if MC >= 1.21.5
//$ import net.minecraft.world.item.component.TooltipDisplay;
//$ import java.util.function.Consumer;
//#else
import java.util.List;
//#endif
//#if MC < 1.21.2
import net.minecraft.world.InteractionResultHolder;
//#endif

/** Opens the player stats editor for yourself. */
public class PlayerStatsBookItem extends Item {

    public PlayerStatsBookItem(Properties props) {
        super(props);
    }

    //#if MC >= 1.21.2
    //$ @Override
    //$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$     if (level.isClientSide()) ClientHooks.openPlayerEditor(player);
    //$     return InteractionResult.SUCCESS;
    //$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) ClientHooks.openPlayerEditor(player);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    //#endif

    //#if MC >= 1.21.5
    //$ @Override
    //$ public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
    //$     if (Platform.getEnvironment() == Env.CLIENT) ClientHooks.appendBookTooltip(lines);
    //$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (Platform.getEnvironment() == Env.CLIENT) ClientHooks.appendBookTooltip(lines::add);
    }
    //#endif
}
