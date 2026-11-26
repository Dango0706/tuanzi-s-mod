package me.tuanzi.mixin;

import me.tuanzi.item.StructureBlueprintItem;
import me.tuanzi.util.LitematicaParser;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在通过 {@link DataComponentPatch} 反序列化或构筑结构蓝图 {@link ItemStack} 时，
 * 自动将旧版未压缩 NBT 转为 GZIP 压缩载荷，避免旧存档内实体或背包同步触发 2MB 限制断连。
 */
@Mixin(ItemStack.class)
public class ItemStackBlueprintCompactMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V",
            at = @At("RETURN")
    )
    private void tuanzis_mod$compactLegacyBlueprintOnConstruct(
            Holder<Item> item,
            int count,
            DataComponentPatch components,
            CallbackInfo ci
    ) {
        if (count > 0 && item != null && item.isBound() && item.value() instanceof StructureBlueprintItem) {
            LitematicaParser.compactItemStackIfNeeded((ItemStack) (Object) this);
        }
    }
}
