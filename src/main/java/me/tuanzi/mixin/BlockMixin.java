package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "playerDestroy", at = @At("TAIL"))
    private void tuanzis_mod$applyBlockDestructionEnchantments(
        ServerLevel level,
        ServerPlayer player,
        BlockPos pos,
        BlockState state,
        @Nullable BlockEntity blockEntity,
        ItemStack destroyedWith,
        CallbackInfo ci
    ) {
        if (destroyedWith.isEmpty()) return;

        // 仅手持该锄破坏成熟作物时触发：仅作用于小麦、胡萝卜、马铃薯、甜菜根
        if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
            ItemStack extraCrop = null;
            if (state.is(Blocks.WHEAT)) {
                extraCrop = new ItemStack(Items.WHEAT);
            } else if (state.is(Blocks.CARROTS)) {
                extraCrop = new ItemStack(Items.CARROT);
            } else if (state.is(Blocks.POTATOES)) {
                extraCrop = new ItemStack(Items.POTATO);
            } else if (state.is(Blocks.BEETROOTS)) {
                extraCrop = new ItemStack(Items.BEETROOT);
            }

            if (extraCrop != null) {
                var enchRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var harvestHolder = enchRegistry.getOrThrow(ModEnchantments.BOUNTIFUL_HARVEST);
                int harvestLevel = EnchantmentHelper.getItemEnchantmentLevel(harvestHolder, destroyedWith);

                if (harvestLevel > 0) {
                    // 每级 10% 概率额外掉落 1 个同类作物，III级 30%
                    float chance = 0.10f * harvestLevel;
                    if (level.getRandom().nextFloat() < chance) {
                        Block.popResource(level, pos, extraCrop);
                        ModLog.debug(player, "【丰获】附魔生效（等级: %d，概率: %.0f%%）！额外掉落作物: %s", harvestLevel, chance * 100, extraCrop.getHoverName().getString());
                    }

                    // 额外掉落 3 * level 点经验值
                    int baseXp = 3 * harvestLevel;
                    var expAffinityHolder = enchRegistry.getOrThrow(ModEnchantments.EXPERIENCE_AFFINITY);
                    int expAffinityLevel = EnchantmentHelper.getItemEnchantmentLevel(expAffinityHolder, destroyedWith);
                    int finalXp = baseXp;
                    if (expAffinityLevel > 0) {
                        // 锄与经验亲和联动
                        float multiplier = 1.0f + 0.08f * expAffinityLevel;
                        finalXp = Math.round(baseXp * multiplier);
                        ModLog.debug(player, "【丰获 x 经验亲和】联动触发！基础经验: %d，经经验亲和（等级: %d）增幅后经验: %d", baseXp, expAffinityLevel, finalXp);
                    } else {
                        ModLog.debug(player, "【丰获】附魔掉落经验: %d 点（等级: %d）", finalXp, harvestLevel);
                    }

                    if (finalXp > 0 && level.getGameRules().get(GameRules.BLOCK_DROPS)) {
                        ExperienceOrb.award(level, Vec3.atCenterOf(pos), finalXp);
                    }
                }
            }
        }

        // 石心 (Stone Heart)：挖掘石头、深板岩、安山岩、闪长岩、花岗岩时，每级有5%/10%/15%概率掉落1个经验球，提供1-3点经验；不额外消耗耐久，不受时运影响;可与经验亲和联动。
        if (state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)) {
            var enchRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var stoneHeartHolder = enchRegistry.getOrThrow(ModEnchantments.STONE_HEART);
            int stoneHeartLevel = EnchantmentHelper.getItemEnchantmentLevel(stoneHeartHolder, destroyedWith);
            if (stoneHeartLevel > 0) {
                float chance = 0.05f * stoneHeartLevel; // 5%, 10%, 15%
                if (level.getRandom().nextFloat() < chance) {
                    int baseXp = Mth.nextInt(level.getRandom(), 1, 3);
                    var expAffinityHolder = enchRegistry.getOrThrow(ModEnchantments.EXPERIENCE_AFFINITY);
                    int expAffinityLevel = EnchantmentHelper.getItemEnchantmentLevel(expAffinityHolder, destroyedWith);
                    int finalXp = baseXp;
                    if (expAffinityLevel > 0) {
                        float multiplier = 1.0f + 0.08f * expAffinityLevel;
                        finalXp = Math.round(baseXp * multiplier);
                        ModLog.debug(player, "【石心 x 经验亲和】联动触发！开采原石掉落基础经验: %d，经经验亲和（等级: %d）增幅后经验: %d", baseXp, expAffinityLevel, finalXp);
                    } else {
                        ModLog.debug(player, "【石心】附魔生效（等级: %d，概率: %.0f%%）！开采原石掉落经验: %d 点", stoneHeartLevel, chance * 100, finalXp);
                    }

                    if (finalXp > 0 && level.getGameRules().get(GameRules.BLOCK_DROPS)) {
                        ExperienceOrb.award(level, Vec3.atCenterOf(pos), finalXp);
                    }
                }
            }
        }
    }
}
