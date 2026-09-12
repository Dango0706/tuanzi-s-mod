package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Unique
    private static final int CODEX_REFRESH_BUTTON_ID = 3;

    @Shadow
    @Final
    private Container enchantSlots;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private DataSlot enchantmentSeed;

    @Shadow
    public abstract void slotsChanged(Container container);

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$handleCodexRefresh(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (buttonId == CODEX_REFRESH_BUTTON_ID) {
            // 校验副手是否持有智慧法典
            if (!player.getOffhandItem().is(ModItems.CODEX_OF_ENCHANTING)) {
                cir.setReturnValue(false);
                return;
            }

            // 校验是否有可附魔物品
            ItemStack itemStack = this.enchantSlots.getItem(0);
            if (itemStack.isEmpty() || !itemStack.isEnchantable()) {
                cir.setReturnValue(false);
                return;
            }

            // 校验经验等级（创造模式免经验）
            if (player.experienceLevel < 1 && !player.hasInfiniteMaterials()) {
                cir.setReturnValue(false);
                return;
            }

            // 扣除 1 级经验
            if (!player.hasInfiniteMaterials()) {
                player.giveExperienceLevels(-1);
            }

            // 随机生成新的附魔种子并同步
            int newSeed = player.getRandom().nextInt();
            ((PlayerAccessor) player).tuanzis_mod$setEnchantmentSeed(newSeed);
            this.enchantmentSeed.set(newSeed);

            // 重新计算槽位附魔选项
            this.slotsChanged(this.enchantSlots);

            // 播放音效与输出调试日志
            this.access.execute((level, pos) -> {
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 1.2F);
            });

            ModLog.debug(player, null, "使用智慧法典刷新了附魔选项，新附魔种子: " + newSeed);
            cir.setReturnValue(true);
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "getEnchantmentList",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;"
        )
    )
    private java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> tuanzis_mod$restrictAncientScrolls(
        net.minecraft.util.RandomSource random, ItemStack itemStack, int enchantmentCost, java.util.stream.Stream<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> source
    ) {
        // 强制需求附魔台等级 30 级及以上才可出现
        if (enchantmentCost < 30) {
            source = source.filter(holder -> !holder.is(me.tuanzi.init.ModEnchantments.ANCIENT_SCROLL));
        }

        java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance> result = new java.util.ArrayList<>(
            net.minecraft.world.item.enchantment.EnchantmentHelper.selectEnchantment(random, itemStack, enchantmentCost, source)
        );

        // 附魔台中最高仅能获取一级的古卷附魔
        for (int i = 0; i < result.size(); i++) {
            net.minecraft.world.item.enchantment.EnchantmentInstance instance = result.get(i);
            if (instance.enchantment().is(me.tuanzi.init.ModEnchantments.ANCIENT_SCROLL)) {
                if (instance.level() > 1) {
                    result.set(i, new net.minecraft.world.item.enchantment.EnchantmentInstance(instance.enchantment(), 1));
                }
                ModLog.debug("EnchantmentMenu", "getEnchantmentList", "附魔台生成古卷附魔: " + instance.enchantment().getRegisteredName() + " (等级锁定为 1 级)");
            }
        }

        return result;
    }
}
