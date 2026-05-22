package me.tuanzi.mixin;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Mixin(ServerPlayerGameMode.class)
public abstract class ChainMiningMixin {

    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;

    @Shadow public abstract boolean destroyBlock(BlockPos pos);

    private static final ThreadLocal<Boolean> tuanzis_mod$isVeinMining = ThreadLocal.withInitial(() -> false);
    private BlockState tuanzis_mod$firstBlockState;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void tuanzis_mod$beforeDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!tuanzis_mod$isVeinMining.get()) {
            tuanzis_mod$firstBlockState = this.level.getBlockState(pos);
        }
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void tuanzis_mod$afterDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !tuanzis_mod$isVeinMining.get()) {
            ItemStack tool = this.player.getMainHandItem();
            if (tool.isEmpty()) return;

            var enchantmentRegistry = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var chainMiningEnch = enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_MINING);
            int levelVal = EnchantmentHelper.getItemEnchantmentLevel(chainMiningEnch, tool);

            if (levelVal > 0 && Tuanzis_mod.isHoldingChainMiningKey(this.player)) {
                tuanzis_mod$isVeinMining.set(true);
                try {
                    int maxExtraBlocks = levelVal * 16 - 1;
                    BlockState targetState = tuanzis_mod$firstBlockState;
                    if (targetState == null || targetState.isAir()) return;

                    List<BlockPos> toBreak = tuanzis_mod$collectChainMiningBlocks(pos, targetState, maxExtraBlocks);
                    int extraBlocksBroke = 0;

                    for (BlockPos nextPos : toBreak) {
                        if (this.player.getMainHandItem().isEmpty()) {
                            break;
                        }
                        if (this.destroyBlock(nextPos)) {
                            extraBlocksBroke++;
                        }
                    }

                    if (extraBlocksBroke > 0 && !this.player.isCreative() && !this.player.isSpectator()) {
                        tuanzis_mod$applyHungerExhaustion(extraBlocksBroke);
                    }
                } finally {
                    tuanzis_mod$isVeinMining.set(false);
                    tuanzis_mod$firstBlockState = null;
                }
            }
        }
    }

    private List<BlockPos> tuanzis_mod$collectChainMiningBlocks(BlockPos origin, BlockState targetState, int maxCount) {
        List<BlockPos> result = new ArrayList<>();
        if (maxCount <= 0) return result;

        Queue<BlockPos> queue = new java.util.ArrayDeque<>();
        Set<BlockPos> visited = new java.util.HashSet<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && result.size() < maxCount) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && this.level.isInWorldBounds(neighbor)) {
                            BlockState state = this.level.getBlockState(neighbor);
                            if (state.is(targetState.getBlock())) {
                                if (this.player.getMainHandItem().canDestroyBlock(state, this.level, neighbor, this.player)) {
                                    visited.add(neighbor);
                                    queue.add(neighbor);
                                    result.add(neighbor);
                                    if (result.size() >= maxCount) {
                                        return result;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private void tuanzis_mod$applyHungerExhaustion(int extraBlocksBroke) {
        float addedExhaustion = extraBlocksBroke * 0.25f;
        var foodData = this.player.getFoodData();
        float currentExhaustion = ((FoodDataAccessor) foodData).getExhaustionLevel();

        float newExhaustion = currentExhaustion + addedExhaustion;
        float saturation = foodData.getSaturationLevel();
        int food = foodData.getFoodLevel();
        net.minecraft.world.Difficulty difficulty = this.level.getDifficulty();

        while (newExhaustion >= 4.0f) {
            newExhaustion -= 4.0f;
            if (saturation > 0.0f) {
                saturation = Math.max(saturation - 1.0f, 0.0f);
            } else if (difficulty != net.minecraft.world.Difficulty.PEACEFUL) {
                food = Math.max(food - 1, 0);
            }
        }

        foodData.setFoodLevel(food);
        foodData.setSaturation(saturation);
        ((FoodDataAccessor) foodData).setExhaustionLevel(newExhaustion);

        this.player.connection.send(new ClientboundSetHealthPacket(this.player.getHealth(), food, saturation));
    }
}
