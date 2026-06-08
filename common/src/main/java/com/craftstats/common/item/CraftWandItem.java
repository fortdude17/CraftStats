package com.craftstats.common.item;

import com.craftstats.common.config.CraftStatsConfig;
import com.craftstats.common.stats.BlockStats;
import com.craftstats.common.stats.MobStats;
import com.craftstats.common.stats.StatRegistry;
import com.craftstats.common.util.JsonUtil;
import com.craftstats.common.util.ScreenOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class CraftWandItem extends Item {

    public CraftWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level  level  = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!canUse(player)) {
            if (level.isClientSide)
                player.sendSystemMessage(Component.literal("CraftStats: requires operator or creative mode.")
                        .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            net.minecraft.core.BlockPos clickedPos = ctx.getClickedPos();
            Block block = level.getBlockState(clickedPos).getBlock();
            if (player.isShiftKeyDown()) {

                ScreenOpener.openBlockEditor(block);
            } else {

                String posKey = StatRegistry.makePosKey(level.dimension(), clickedPos);
                ScreenOpener.openBlockEditorAt(posKey, block, clickedPos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!canUse(player))
            return InteractionResultHolder.pass(player.getItemInHand(hand));

        if (level.isClientSide) {

            ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offhand.getItem() instanceof SpawnEggItem) {
                String typeId = spawnEggEntityTypeId(offhand);
                if (typeId != null) {
                    ScreenOpener.openMobEditorById(typeId);
                    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), true);
                }
            }

            ScreenOpener.openPlayerEditor(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!canUse(player)) return InteractionResult.PASS;
        Level level = player.level();
        if (level.isClientSide) {
            if (target instanceof Player targetPlayer) {

                ScreenOpener.openPlayerEditor(targetPlayer);
            } else if (player.isShiftKeyDown()) {
                ScreenOpener.openRandomizeMob(target);
            } else {
                ScreenOpener.openMobEditor(target);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && player.level().isClientSide) {
            String json = JsonUtil.serializeMobStats(target);
            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(json);
            player.sendSystemMessage(Component.literal("Copied stats to clipboard.")
                    .withStyle(ChatFormatting.GREEN));
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                 List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Right-click block         › edit THIS block").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Shift+right-click block   › edit ALL of that type").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Right-click mob/NPC       › edit mob stats").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Shift+right-click mob     › randomize mob stats").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Right-click player        › edit player stats").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Offhand spawn egg         › edit that mob type").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Right-click (air)         › edit your stats").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Left-click mob            › copy stats JSON").withStyle(ChatFormatting.GRAY));

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return;

            if (mc.crosshairPickEntity instanceof LivingEntity le) {
                ResourceLocation typeId = EntityType.getKey(le.getType());
                MobStats ms = StatRegistry.getMob(typeId);
                lines.add(Component.empty());
                if (ms != null) {
                    lines.add(Component.literal("★ " + typeId.getPath() + ": custom stats active")
                            .withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.literal("  HP " + ms.maxHealth + "  Dmg " + ms.attackDamage)
                            .withStyle(ChatFormatting.WHITE));
                } else {
                    lines.add(Component.literal("Looking at: " + typeId.getPath())
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            } else if (mc.hitResult instanceof BlockHitResult bhr) {
                net.minecraft.core.BlockPos bhp = bhr.getBlockPos();
                Block block = mc.level.getBlockState(bhp).getBlock();
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

                String posKey = StatRegistry.makePosKey(mc.level.dimension(), bhp);
                BlockStats bsPos  = StatRegistry.getBlockAt(posKey);
                BlockStats bsType = StatRegistry.getBlock(blockId);
                if (bsPos != null) {
                    lines.add(Component.empty());
                    lines.add(Component.literal("★ THIS block: custom stats active")
                            .withStyle(ChatFormatting.AQUA));
                    lines.add(Component.literal("  Hardness " + bsPos.hardness + "  Light " + bsPos.lightEmission
                            + "  Slip " + bsPos.slipperiness).withStyle(ChatFormatting.WHITE));
                    if (bsType != null)
                        lines.add(Component.literal("  (type override also active)")
                                .withStyle(ChatFormatting.DARK_GRAY));
                } else if (bsType != null) {
                    lines.add(Component.empty());
                    lines.add(Component.literal("★ " + blockId.getPath() + ": type stats active")
                            .withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.literal("  Hardness " + bsType.hardness + "  Light " + bsType.lightEmission)
                            .withStyle(ChatFormatting.WHITE));
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean canUse(Player player) {
        CraftStatsConfig.ConfigData cfg = CraftStatsConfig.get();
        if (cfg.requireOp && player instanceof ServerPlayer sp) {
            return sp.hasPermissions(2);
        }
        if (player.isCreative()) return true;
        return cfg.allowSurvival;
    }

    private static String spawnEggEntityTypeId(ItemStack egg) {
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(egg.getItem());
        String path = itemKey.getPath();
        if (path.endsWith("_spawn_egg")) {
            String entityPath = path.substring(0, path.length() - "_spawn_egg".length());
            ResourceLocation entityKey = ResourceLocation.fromNamespaceAndPath(
                    itemKey.getNamespace(), entityPath);
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey)) {
                return entityKey.toString();
            }
        }
        return null;
    }
}
