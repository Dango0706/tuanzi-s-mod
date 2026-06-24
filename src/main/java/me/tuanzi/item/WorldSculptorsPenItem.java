package me.tuanzi.item;

import me.tuanzi.util.WorldSculptorsPenManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorldSculptorsPenItem extends Item {

    public enum FillMode {
        FULL_REPLACE("tuanzis_mod.mode.full_replace"),
        AIR_ONLY("tuanzis_mod.mode.air_only"),
        SEMI_REPLACE("tuanzis_mod.mode.semi_replace"),
        REPLACE_AIR_ONLY("tuanzis_mod.mode.replace_air_only");

        public final String translationKey;
        FillMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component getDisplayName() {
            return Component.translatable(translationKey);
        }
    }

    public static FillMode getMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            int modeVal = tag.getIntOr("FillMode", 0);
            if (modeVal >= 0 && modeVal < FillMode.values().length) {
                return FillMode.values()[modeVal];
            }
        }
        return FillMode.FULL_REPLACE;
    }

    public static void toggleMode(ItemStack stack, Player player) {
        FillMode current = getMode(stack);
        FillMode next = FillMode.values()[(current.ordinal() + 1) % FillMode.values().length];

        CompoundTag tag = new CompoundTag();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt("FillMode", next.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        player.sendSystemMessage(Component.literal("§6")
            .append(Component.translatable("message.tuanzis_mod.world_sculptors_pen.mode_toggled", next.getDisplayName())));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    public WorldSculptorsPenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player.isSecondaryUseActive()) { // Shift + 右键
            // 1. 如果在60秒内有可撤销记录，优先执行撤销
            if (hasActiveUndoRecord(stack)) {
                if (tryUndo(stack, player, level)) {
                    return InteractionResult.SUCCESS;
                }
            }

            // 2. 否则进行方块的选定
            if (!level.isClientSide()) {
                BlockState state = level.getBlockState(pos);
                Item blockItem = state.getBlock().asItem();
                if (blockItem != net.minecraft.world.item.Items.AIR) {
                    CompoundTag tag = new CompoundTag();
                    CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        tag = customData.copyTag();
                    }
                    String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    tag.putString("SelectedBlock", blockId);

                    // 重新选择填充类型时，清除已记录的第一个点坐标
                    tag.remove("pos1_x");
                    tag.remove("pos1_y");
                    tag.remove("pos1_z");
                    tag.remove("pos1_dim");

                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                    player.sendSystemMessage(Component.literal("§a塑世之笔已选定填充方块: " + state.getBlock().getName().getString()));
                    me.tuanzi.util.ModLog.debug(player, null, "塑世之笔选定方块类型: " + blockId);
                } else {
                    player.sendSystemMessage(Component.literal("§c此方块无法被选定为填充方块！"));
                }
            }
            return InteractionResult.SUCCESS;
        } else {
            // 普通右键点击方块（两点框选放置）
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            // 读取已选定的方块ID
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            FillMode mode = getMode(stack);
            Block targetBlock = Blocks.AIR;
            String selectedBlockId = "minecraft:air";

            if (mode != FillMode.AIR_ONLY) {
                if (customData == null || !customData.copyTag().getString("SelectedBlock").isPresent()) {
                    player.sendSystemMessage(Component.literal("§c请先按住 Shift + 右键点击方块以选定填充类型！"));
                    return InteractionResult.SUCCESS;
                }
                CompoundTag tag = customData.copyTag();
                selectedBlockId = tag.getStringOr("SelectedBlock", "");
                targetBlock = BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(selectedBlockId));
                if (targetBlock == null || targetBlock == Blocks.AIR) {
                    player.sendSystemMessage(Component.literal("§c选定的填充方块无效，请重新选定！"));
                    return InteractionResult.SUCCESS;
                }
            }

            CompoundTag tag = (customData != null) ? customData.copyTag() : new CompoundTag();

            // 检查是否已经记录第一个点
            if (!tag.getInt("pos1_x").isPresent()) {
                // 记录第一个点
                tag.putInt("pos1_x", pos.getX());
                tag.putInt("pos1_y", pos.getY());
                tag.putInt("pos1_z", pos.getZ());
                tag.putString("pos1_dim", level.dimension().identifier().toString());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                player.sendSystemMessage(Component.literal("§e已记录起点: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"));
                me.tuanzi.util.ModLog.debug(player, null, "塑世之笔记录起点: " + pos);
                return InteractionResult.SUCCESS;
            } else {
                // 已有第一个点，进行第二次点击
                int x1 = tag.getIntOr("pos1_x", 0);
                int y1 = tag.getIntOr("pos1_y", 0);
                int z1 = tag.getIntOr("pos1_z", 0);
                String dimStr = tag.getStringOr("pos1_dim", "");
                BlockPos pos1 = new BlockPos(x1, y1, z1);

                // 检查维度匹配
                if (!dimStr.equals(level.dimension().identifier().toString())) {
                    // 维度不同，重置为第一个点
                    tag.putInt("pos1_x", pos.getX());
                    tag.putInt("pos1_y", pos.getY());
                    tag.putInt("pos1_z", pos.getZ());
                    tag.putString("pos1_dim", level.dimension().identifier().toString());
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                    player.sendSystemMessage(Component.literal("§e维度不匹配，已重新记录起点: [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"));
                    return InteractionResult.SUCCESS;
                }

                BlockPos pos2 = pos;

                // 距离计算与限制（欧氏距离 <= 64）
                double distSqr = pos1.distSqr(pos2);
                if (distSqr > 64.0 * 64.0) {
                    player.sendSystemMessage(Component.literal("§c单次填充最大直线距离限制为 64 格！已重置。"));
                    // 清空第一个点以便重新开始
                    tag.remove("pos1_x");
                    tag.remove("pos1_y");
                    tag.remove("pos1_z");
                    tag.remove("pos1_dim");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    return InteractionResult.SUCCESS;
                }

                // 计算体积
                int minX = Math.min(pos1.getX(), pos2.getX());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                int minY = Math.min(pos1.getY(), pos2.getY());
                int maxY = Math.max(pos1.getY(), pos2.getY());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                int sizeX = maxX - minX + 1;
                int sizeY = maxY - minY + 1;
                int sizeZ = maxZ - minZ + 1;
                int volume = sizeX * sizeY * sizeZ;

                if (volume > 2048) {
                    player.sendSystemMessage(Component.literal("§c单次填充最大体积限制为 2048 个方块（当前区域为 " + volume + " 格）！已重置。"));
                    // 清空第一个点
                    tag.remove("pos1_x");
                    tag.remove("pos1_y");
                    tag.remove("pos1_z");
                    tag.remove("pos1_dim");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    return InteractionResult.SUCCESS;
                }

                // 扫描长方体区域，统计真正需要更改的格子并记录原状态
                List<WorldSculptorsPenManager.BlockStateRecord> records = new ArrayList<>();
                Item targetItem = (targetBlock != null) ? targetBlock.asItem() : net.minecraft.world.item.Items.AIR;

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos targetPos = new BlockPos(x, y, z);
                            BlockState state = level.getBlockState(targetPos);

                            // 当硬度大于等于 0 (排除基岩、传送门等不可破坏方块) 时，才可记录填充
                            if (state.getDestroySpeed(level, targetPos) < 0.0F) {
                                continue;
                            }

                            boolean matches = false;
                            if (mode == FillMode.AIR_ONLY) {
                                matches = !state.isAir();
                            } else if (mode == FillMode.REPLACE_AIR_ONLY) {
                                matches = state.isAir() && !state.is(targetBlock);
                            } else if (mode == FillMode.SEMI_REPLACE) {
                                if (!state.is(targetBlock) && (state.isAir() || state.getCollisionShape(level, targetPos).isEmpty())) {
                                    matches = true;
                                }
                            } else {
                                matches = !state.is(targetBlock);
                            }

                            if (matches) {
                                records.add(new WorldSculptorsPenManager.BlockStateRecord(targetPos, state));
                            }
                        }
                    }
                }

                int N = records.size();
                if (N == 0) {
                    player.sendSystemMessage(Component.literal("§e该区域内无可被填充的方块！已重置起点。"));
                    tag.remove("pos1_x");
                    tag.remove("pos1_y");
                    tag.remove("pos1_z");
                    tag.remove("pos1_dim");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    return InteractionResult.SUCCESS;
                }

                // 校验玩家背包及潜影盒内选中方块的存量是否足够 (填充空气模式不消耗背包物品)
                if (mode != FillMode.AIR_ONLY) {
                    int available = getTargetItemCount(player, targetItem);
                    if (available < N) {
                        player.sendSystemMessage(Component.literal("§c背包中方块数量不足！需要 " + N + " 个，缺少 " + (N - available) + " 个。"));
                        return InteractionResult.SUCCESS;
                    }
                }

                // 校验笔的耐久度是否足够
                int remainingDurability = stack.getMaxDamage() - stack.getDamageValue();
                int durabilityCost = Math.max(1, N);
                if (remainingDurability < durabilityCost) {
                    player.sendSystemMessage(Component.literal("§c塑世之笔耐久度不足！需要 " + durabilityCost + " 点，剩余 " + remainingDurability + " 点。"));
                    return InteractionResult.SUCCESS;
                }

                // 执行扣除 (填充空气模式不扣除背包物品)
                if (mode != FillMode.AIR_ONLY) {
                    deductTargetItems(player, targetItem, N);
                }
                stack.setDamageValue(stack.getDamageValue() + durabilityCost);

                // 填充方块
                for (WorldSculptorsPenManager.BlockStateRecord r : records) {
                    level.setBlock(r.pos, targetBlock.defaultBlockState(), Block.UPDATE_ALL);
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.GLOW, 
                            r.pos.getX() + 0.5, r.pos.getY() + 0.5, r.pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.05);
                    }
                }

                // 注册撤销记录
                UUID recordId = UUID.randomUUID();
                WorldSculptorsPenManager.addRecord(recordId, level.dimension(), records, selectedBlockId, N);

                // 保存最后填充 ID
                tag.putString("LastFillId", recordId.toString());

                // 清理点 1 数据，让玩家能直接进行下一次填充
                tag.remove("pos1_x");
                tag.remove("pos1_y");
                tag.remove("pos1_z");
                tag.remove("pos1_dim");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                // 播放华丽奢华的紫水晶与信标音效
                level.playSound(null, pos2.getX(), pos2.getY(), pos2.getZ(), 
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, pos2.getX(), pos2.getY(), pos2.getZ(), 
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);

                player.sendSystemMessage(Component.literal("§a填充成功！填充了 " + N + " 个方块，消耗耐久 " + durabilityCost + " 点。"));
                me.tuanzi.util.ModLog.debug(player, null, "塑世之笔填充成功: " + pos1 + " 到 " + pos2 + "，方块数 " + N);
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) { // Shift + 右键空气
            // 1. 如果在60秒内有可撤销记录，优先执行撤销
            if (hasActiveUndoRecord(stack)) {
                if (tryUndo(stack, player, level)) {
                    return InteractionResult.SUCCESS;
                }
            }
            // 2. 否则，清除已经记录的第一个起点
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.getInt("pos1_x").isPresent()) {
                    if (!level.isClientSide()) {
                        tag.remove("pos1_x");
                        tag.remove("pos1_y");
                        tag.remove("pos1_z");
                        tag.remove("pos1_dim");
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        player.sendSystemMessage(Component.literal("§e已取消记录的第一个位置。"));
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        // 当手持该笔移动或静止时，在笔尖位置喷射淡蓝色发光粒子（用 GLOW 代替 GLOW_SQUID_INK，降低几率并偏离视线中心以防遮挡第一人称）
        if (slot != null && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && owner instanceof Player player) {
            if (level.getRandom().nextFloat() < 0.2f) {
                Vec3 pos = player.getEyePosition(1.0F).add(player.getLookAngle().scale(1.2)).add(0, -0.35, 0);
                level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);

        FillMode mode = getMode(stack);
        tooltipComponents.accept(Component.literal("§b当前填充模式: ").append(mode.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)));

        ItemStack selected = getSelectedBlockStack(stack);
        if (mode != FillMode.AIR_ONLY) {
            if (!selected.isEmpty()) {
                tooltipComponents.accept(Component.literal("§a选定填充方块: ").append(Component.empty().withStyle(ChatFormatting.GOLD).append(selected.getHoverName())));
            } else {
                tooltipComponents.accept(Component.literal("§7选定填充方块: 未选定"));
            }
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.getInt("pos1_x").isPresent()) {
                int x = tag.getIntOr("pos1_x", 0);
                int y = tag.getIntOr("pos1_y", 0);
                int z = tag.getIntOr("pos1_z", 0);
                tooltipComponents.accept(Component.literal("§e已记录起点: [" + x + ", " + y + ", " + z + "]"));
            }
        }

        if (hasActiveUndoRecord(stack)) {
            tooltipComponents.accept(Component.literal("§d[可撤销] 潜行 + 右键撤销本次填充"));
        }

        tooltipComponents.accept(Component.literal("§8限制: 最大填充直线距离 64 格，最大填充体积 2048"));
        tooltipComponents.accept(Component.literal("§8按住 Shift + 右键点击方块重新选定方块类型"));
        tooltipComponents.accept(Component.literal("§8按住 Shift + 左键循环切换填充模式"));
    }

    // ──────────────────────────────────────────────
    // 撤销及退回核心逻辑
    // ──────────────────────────────────────────────
    private static boolean hasActiveUndoRecord(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        CompoundTag tag = customData.copyTag();
        if (!tag.getString("LastFillId").isPresent()) return false;
        try {
            UUID fillId = UUID.fromString(tag.getStringOr("LastFillId", ""));
            return WorldSculptorsPenManager.getRecord(fillId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean tryUndo(ItemStack stack, Player player, Level level) {
        if (level.isClientSide()) return true;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;

        CompoundTag tag = customData.copyTag();
        if (!tag.getString("LastFillId").isPresent()) return false;

        String fillIdStr = tag.getStringOr("LastFillId", "");
        UUID fillId;
        try {
            fillId = UUID.fromString(fillIdStr);
        } catch (IllegalArgumentException e) {
            return false;
        }

        WorldSculptorsPenManager.FillRecord record = WorldSculptorsPenManager.getRecord(fillId);
        if (record == null) {
            player.sendSystemMessage(Component.literal("§c无可撤销的填充记录或记录已过期！"));
            return false;
        }

        if (!record.dimension.equals(level.dimension())) {
            player.sendSystemMessage(Component.literal("§c维度不匹配，无法在不同维度撤销填充！"));
            return false;
        }

        Block targetBlock = BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(record.blockId));
        Item targetItem = (targetBlock != null) ? targetBlock.asItem() : net.minecraft.world.item.Items.AIR;

        int restoredCount = 0;
        // 遍历所有被更改的块，且只恢复当前依然是填充方块的方块 (包括空气方块)
        if (targetBlock != null) {
            for (WorldSculptorsPenManager.BlockStateRecord bRecord : record.blockRecords) {
                BlockState currentState = level.getBlockState(bRecord.pos);
                if (currentState.is(targetBlock)) {
                    level.setBlock(bRecord.pos, bRecord.oldState, Block.UPDATE_ALL);
                    restoredCount++;
                }
            }
        }

        // 仅在实际还原了方块的情况下，退回扣除的方块与耐久
        if (restoredCount > 0) {
            giveTargetItems(player, targetItem, restoredCount);

            // 返还笔 of 耐久
            int currentDamage = stack.getDamageValue();
            stack.setDamageValue(Math.max(0, currentDamage - restoredCount));

            player.sendSystemMessage(Component.literal("§a已成功撤销填充！恢复了 " + restoredCount + " 个方块，物品及耐久度已返还。"));
            me.tuanzi.util.ModLog.debug(player, null, "塑世之笔成功撤销: 还原了 " + restoredCount + " 个方块");
        } else {
            player.sendSystemMessage(Component.literal("§e填充的方块已全部被修改或挖掉，无可退回内容！"));
        }

        // 移除这只笔和全局的最后一次填充 ID
        tag.remove("LastFillId");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        WorldSculptorsPenManager.removeRecord(fillId);

        // 播放信标关闭的音效
        level.playSound(null, player.getX(), player.getY(), player.getZ(), 
            net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

        return true;
    }

    // ──────────────────────────────────────────────
    // 容器组件 (潜影盒) 及背包智能存取逻辑
    // ──────────────────────────────────────────────
    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem 
            && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
    }

    public static int getTargetItemCount(Player player, Item targetItem) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(targetItem)) {
                total += stack.getCount();
            } else if (isShulkerBox(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    net.minecraft.core.NonNullList<ItemStack> shulkerInv = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
                    contents.copyInto(shulkerInv);
                    for (ItemStack innerStack : shulkerInv) {
                        if (!innerStack.isEmpty() && innerStack.is(targetItem)) {
                            total += innerStack.getCount();
                        }
                    }
                }
            }
        }
        return total;
    }

    public static void deductTargetItems(Player player, Item targetItem, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(targetItem)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            } else if (isShulkerBox(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    net.minecraft.core.NonNullList<ItemStack> shulkerInv = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
                    contents.copyInto(shulkerInv);
                    boolean changed = false;
                    for (int j = 0; j < shulkerInv.size(); j++) {
                        ItemStack innerStack = shulkerInv.get(j);
                        if (!innerStack.isEmpty() && innerStack.is(targetItem)) {
                            int take = Math.min(innerStack.getCount(), remaining);
                            innerStack.shrink(take);
                            remaining -= take;
                            changed = true;
                            if (remaining <= 0) break;
                        }
                    }
                    if (changed) {
                        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(shulkerInv));
                    }
                }
            }
            if (remaining <= 0) break;
        }
    }

    public static void giveTargetItems(Player player, Item targetItem, int amount) {
        if (targetItem == net.minecraft.world.item.Items.AIR) return;
        int remaining = amount;

        // 1. 尝试先返还到背包里已有且未满的潜影盒中
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (isShulkerBox(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                net.minecraft.core.NonNullList<ItemStack> shulkerInv = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
                if (contents != null) {
                    contents.copyInto(shulkerInv);
                }

                boolean changed = false;
                // A. 堆叠在已有槽位
                for (int j = 0; j < shulkerInv.size(); j++) {
                    ItemStack innerStack = shulkerInv.get(j);
                    if (!innerStack.isEmpty() && innerStack.is(targetItem)) {
                        int space = innerStack.getMaxStackSize() - innerStack.getCount();
                        if (space > 0) {
                            int add = Math.min(space, remaining);
                            innerStack.grow(add);
                            remaining -= add;
                            changed = true;
                            if (remaining <= 0) break;
                        }
                    }
                }

                // B. 放入空槽位
                if (remaining > 0) {
                    for (int j = 0; j < shulkerInv.size(); j++) {
                        ItemStack innerStack = shulkerInv.get(j);
                        if (innerStack.isEmpty()) {
                            int add = Math.min(targetItem.getDefaultMaxStackSize(), remaining);
                            shulkerInv.set(j, new ItemStack(targetItem, add));
                            remaining -= add;
                            changed = true;
                            if (remaining <= 0) break;
                        }
                    }
                }

                if (changed) {
                    stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(shulkerInv));
                }
                if (remaining <= 0) break;
            }
        }

        // 2. 将最终还有剩余的方块直接放入玩家普通背包，装不下则自动在脚下丢出
        if (remaining > 0) {
            ItemStack extraStack = new ItemStack(targetItem, remaining);
            player.getInventory().placeItemBackInInventory(extraStack);
        }
    }

    public static ItemStack getSelectedBlockStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return ItemStack.EMPTY;
        CompoundTag tag = customData.copyTag();
        if (!tag.getString("SelectedBlock").isPresent()) return ItemStack.EMPTY;
        String idStr = tag.getStringOr("SelectedBlock", "");
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(idStr));
        if (block == null || block == Blocks.AIR) return ItemStack.EMPTY;
        return new ItemStack(block.asItem());
    }
}
