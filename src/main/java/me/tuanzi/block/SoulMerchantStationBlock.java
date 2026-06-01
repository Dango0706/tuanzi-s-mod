package me.tuanzi.block;

import me.tuanzi.block.entity.SoulMerchantStationBlockEntity;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.item.VillagerCageItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class SoulMerchantStationBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<SoulMerchantStationBlock> CODEC = simpleCodec(SoulMerchantStationBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),      // 阶梯底座第一层
        Block.box(2.0, 2.0, 2.0, 14.0, 3.0, 14.0),      // 阶梯底座第二层
        Block.box(2.0, 13.0, 2.0, 14.0, 14.0, 14.0),    // 阶梯顶盖第一层
        Block.box(3.0, 14.0, 3.0, 13.0, 15.0, 13.0),    // 阶梯顶盖第二层
        Block.box(5.0, 15.0, 5.0, 11.0, 16.0, 11.0),    // 顶端能量尖冠
        Block.box(2.0, 3.0, 2.0, 3.5, 13.0, 3.5),        // 纤细西北柱
        Block.box(12.5, 3.0, 2.0, 14.0, 13.0, 3.5),      // 纤细东北柱
        Block.box(2.0, 3.0, 12.5, 3.5, 13.0, 14.0),      // 纤细西南柱
        Block.box(12.5, 3.0, 12.5, 14.0, 13.0, 14.0),    // 纤细东南柱
        Block.box(6.0, 5.0, 6.0, 10.0, 6.0, 10.0)        // 中央悬浮融合台
    );

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public SoulMerchantStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoulMerchantStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.SOUL_MERCHANT_STATION_BLOCK_ENTITY, SoulMerchantStationBlockEntity::serverTick);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof SoulMerchantStationBlockEntity stationBe) {
            if (stationBe.hasVillager()) {
                if (player.isShiftKeyDown()) {
                    if (!level.isClientSide()) {
                        stationBe.rotateVillager();
                    }
                    return InteractionResult.SUCCESS;
                }
                if (!level.isClientSide()) {
                    stationBe.openTradingScreen(player);
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.translatable("message.tuanzis_mod.soul_merchant_station.no_villager"));
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos) instanceof SoulMerchantStationBlockEntity stationBe)) {
            return InteractionResult.PASS;
        }

        // 蹲下+右键旋转村民 (在不是空缚灵笼试图取出的情况下)
        if (player.isShiftKeyDown() && stationBe.hasVillager()) {
            boolean isExtracting = stack.is(ModItems.VILLAGER_CAGE) && !VillagerCageItem.isVillagerStored(stack);
            if (!isExtracting) {
                if (!level.isClientSide()) {
                    stationBe.rotateVillager();
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 1. 手持命名牌改名
        if (stack.getItem() instanceof NameTagItem) {
            if (stationBe.hasVillager()) {
                if (!level.isClientSide()) {
                    if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                        Component customName = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
                        stationBe.renameVillager(customName);
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // 2. 缚灵笼交互
        if (stack.is(ModItems.VILLAGER_CAGE)) {
            boolean cageHasVillager = VillagerCageItem.isVillagerStored(stack);

            if (cageHasVillager) {
                // 试图导入村民
                if (stationBe.hasVillager()) {
                    if (!level.isClientSide()) {
                        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.soul_merchant_station.already_has_villager"));
                    }
                    return InteractionResult.FAIL;
                }

                if (!level.isClientSide()) {
                    net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        net.minecraft.nbt.CompoundTag cageData = customData.copyTag();
                        net.minecraft.nbt.CompoundTag villagerTag = cageData.getCompoundOrEmpty("StoredVillager");
                        
                        stationBe.importVillager(villagerTag);

                        net.minecraft.nbt.CompoundTag vData = villagerTag.getCompoundOrEmpty("VillagerData");
                        String prof = vData.getStringOr("profession", "generic");
                        int lvl = vData.getIntOr("level", 1);
                        me.tuanzi.util.ModLog.debug(player.getName().getString(), pos.toString(), "灵笼贸易站村民导入成功：职业: " + prof + "，等级: " + lvl);

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }

                        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.soul_merchant_station.imported"));
                        level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }
                return InteractionResult.SUCCESS;
            } else {
                // 试图取出村民 (需要潜行)
                if (player.isShiftKeyDown()) {
                    if (!stationBe.hasVillager()) {
                        return InteractionResult.PASS;
                    }

                    if (!level.isClientSide()) {
                        net.minecraft.nbt.CompoundTag villagerTag = stationBe.exportVillager();

                        net.minecraft.nbt.CompoundTag vData = villagerTag.getCompoundOrEmpty("VillagerData");
                        String prof = vData.getStringOr("profession", "generic");
                        int lvl = vData.getIntOr("level", 1);
                        me.tuanzi.util.ModLog.debug(player.getName().getString(), pos.toString(), "灵笼贸易站村民取出成功：职业: " + prof + "，等级: " + lvl);

                        ItemStack filledCage = new ItemStack(ModItems.VILLAGER_CAGE);
                        net.minecraft.nbt.CompoundTag cageData = new net.minecraft.nbt.CompoundTag();
                        cageData.put("StoredVillager", villagerTag);
                        filledCage.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(cageData));
                        
                        filledCage.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                                new net.minecraft.world.item.component.CustomModelData(List.of(1.0f), List.of(), List.of(), List.of()));

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }

                        if (!player.getInventory().add(filledCage)) {
                            player.drop(filledCage, false);
                        }

                        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.soul_merchant_station.extracted"));
                        level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0f, 1.2f);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // 3. 有村民在，则点击进行常规交易
        if (stationBe.hasVillager()) {
            if (!level.isClientSide()) {
                stationBe.openTradingScreen(player);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // 破坏方块时，村民将被释放为世界实体，因此方块本身始终只掉落一个空的灵笼贸易站，防止村民复制
        return List.of(new ItemStack(this));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SoulMerchantStationBlockEntity stationBe) {
            // 如果里面有村民且是在服务端，执行释放逻辑
            if (stationBe.hasVillager() && !level.isClientSide()) {
                stationBe.releaseVillagerOnBreak(level, pos);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 0.8 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            level.addParticle(ParticleTypes.SCULK_SOUL, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
