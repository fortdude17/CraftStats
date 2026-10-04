package com.craftstats.common.item;

import com.craftstats.common.client.ClientHooks;
import com.craftstats.common.util.Permissions;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
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

/** Right-click anything to edit it. All editing happens client-side in a screen. */
public class CraftWandItem extends Item {

    public CraftWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (player == null) return InteractionResult.PASS;
        if (!mayUse(player)) return InteractionResult.FAIL;
        if (level.isClientSide()) {
            BlockPos pos = ctx.getClickedPos();
            if (player.isShiftKeyDown()) ClientHooks.openBlockEditor(level.getBlockState(pos).getBlock());
            else ClientHooks.openBlockEditorAt(level, pos);
        }
        return success(level);
    }

    //#if MC >= 1.21.2
    //$ @Override
    //$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$     if (hand != InteractionHand.MAIN_HAND || !mayUse(player)) return InteractionResult.PASS;
    //$     if (level.isClientSide()) openFromAir(player);
    //$     return success(level);
    //$ }
    //#else
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !mayUse(player)) return InteractionResultHolder.pass(stack);
        if (level.isClientSide()) openFromAir(player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    //#endif

    private static void openFromAir(Player player) {
        String eggType = spawnEggEntityType(player.getItemInHand(InteractionHand.OFF_HAND));
        if (eggType != null) ClientHooks.openMobTypeEditor(eggType);
        else ClientHooks.openPlayerEditor(player);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!mayUse(player)) return InteractionResult.PASS;
        if (player.level().isClientSide()) {
            if (target instanceof Player targetPlayer) ClientHooks.openPlayerEditor(targetPlayer);
            else if (player.isShiftKeyDown()) ClientHooks.openRandomizeMob(target);
            else ClientHooks.openMobEditor(target);
        }
        return success(player.level());
    }

    //#if MC >= 1.21.5
    //$ @Override
    //$ public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
    //$     if (Platform.getEnvironment() == Env.CLIENT) ClientHooks.appendWandTooltip(lines);
    //$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (Platform.getEnvironment() == Env.CLIENT) ClientHooks.appendWandTooltip(lines::add);
    }
    //#endif

    static InteractionResult success(Level level) {
        //#if MC >= 1.21.2
        //$ return InteractionResult.SUCCESS;
        //#else
        return InteractionResult.sidedSuccess(level.isClientSide());
        //#endif
    }

    private static boolean mayUse(Player player) {
        boolean allowed = player instanceof ServerPlayer sp ? Permissions.mayEdit(sp)
                : player.level().isClientSide() && ClientHooks.mayEdit();
        if (!allowed && player.level().isClientSide())
            ClientHooks.actionBar(Component.literal("CraftStats: you need operator permissions to use the Craft Wand.")
                    .withStyle(ChatFormatting.RED));
        return allowed;
    }

    /** "minecraft:zombie" for a zombie spawn egg, otherwise null. */
    private static String spawnEggEntityType(ItemStack egg) {
        if (egg.isEmpty()) return null;
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(egg.getItem());
        String path = itemKey.getPath();
        if (!path.endsWith("_spawn_egg")) return null;
        ResourceLocation entityKey = ResourceLocation.fromNamespaceAndPath(itemKey.getNamespace(),
                path.substring(0, path.length() - "_spawn_egg".length()));
        return BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey) ? entityKey.toString() : null;
    }
}
