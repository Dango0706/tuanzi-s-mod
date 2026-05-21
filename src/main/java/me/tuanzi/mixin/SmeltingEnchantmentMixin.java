package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(BlockBehaviour.class)
public abstract class SmeltingEnchantmentMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void tuanzis_mod$applySmelting(BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        // 在 1.21.2 中，getParameter 返回 Object，需要强制转换
        Object toolObj = builder.getParameter(LootContextParams.TOOL);
        if (!(toolObj instanceof ItemStack tool) || tool.isEmpty()) return;

        ServerLevel level = builder.getLevel();
        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var smeltingEnch = enchantmentRegistry.getOrThrow(ModEnchantments.SMELTING);

        if (EnchantmentHelper.getItemEnchantmentLevel(smeltingEnch, tool) > 0) {
            List<ItemStack> originalDrops = cir.getReturnValue();
            List<ItemStack> newDrops = new ArrayList<>();
            boolean smeltedAny = false;

            for (ItemStack stack : originalDrops) {
                var recipeManager = level.getServer().getRecipeManager();
                // 1.21.2+ 使用 getRecipeFor 代替 getFirstMatch，输入为 RecipeInput
                var recipe = recipeManager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
                
                if (recipe.isPresent()) {
                    // 1.21.2+ 使用 assemble 获取结果
                    ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack)).copy();
                    // 保持堆叠数量，并乘以配方产出（通常是1）
                    result.setCount(stack.getCount() * result.getCount());
                    newDrops.add(result);
                    smeltedAny = true;
                } else {
                    newDrops.add(stack);
                }
            }

            if (smeltedAny) {
                cir.setReturnValue(newDrops);
                
                // 惩罚机制: 额外消耗 1 点耐久 (加上原本的 1 点共计 2 点)
                Object entityObj = builder.getParameter(LootContextParams.THIS_ENTITY);
                LivingEntity livingEntity = (entityObj instanceof LivingEntity le) ? le : null;
                
                // 尝试使用 1.21.2 常见的 hurtAndBreak
                if (livingEntity != null) {
                    tool.hurtAndBreak(1, livingEntity, EquipmentSlot.MAINHAND);
                } else {
                    // 如果没有实体，手动计算耐久消耗（考虑不坏附魔）
                    int damage = EnchantmentHelper.processDurabilityChange(level, tool, 1);
                    if (damage > 0) {
                        tool.setDamageValue(tool.getDamageValue() + damage);
                    }
                }
            }
        }
    }
}
