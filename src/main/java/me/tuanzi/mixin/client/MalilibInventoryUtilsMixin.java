package me.tuanzi.mixin.client;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.tuanzi.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = InventoryUtils.class)
public abstract class MalilibInventoryUtilsMixin {

    @Inject(method = "getInventoryItemCounts", at = @At("RETURN"), require = 0)
    private static void tuanzisMod$onGetInventoryItemCounts(Container container, CallbackInfoReturnable<Object2IntOpenHashMap<ItemType>> cir) {
        Object2IntOpenHashMap<ItemType> counts = cir.getReturnValue();
        if (counts == null || container == null) {
            return;
        }

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.GOLDEN_POCKET)) {
                // 1. 扣除口袋本体计数
                ItemType pocketType = new ItemType(stack, false, false);
                int currentCount = counts.getInt(pocketType);
                if (currentCount > 0) {
                    counts.addTo(pocketType, -stack.getCount());
                    if (counts.getInt(pocketType) <= 0) {
                        counts.removeInt(pocketType);
                    }
                }

                // 2. 统计黄金口袋内的所有存储物品
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    contents.nonEmptyItemCopyStream().forEach(inner -> {
                        if (!inner.isEmpty()) {
                            ItemType innerType = new ItemType(inner, false, false);
                            counts.addTo(innerType, inner.getCount() * stack.getCount());
                        }
                    });
                }
            }
        }
    }
}
