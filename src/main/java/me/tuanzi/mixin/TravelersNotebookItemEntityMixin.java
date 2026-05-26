package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class TravelersNotebookItemEntityMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void tuanzis_mod$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity entity) {

        if (reason == Entity.RemovalReason.KILLED && !entity.level().isClientSide()) {
            ItemStack notebook = entity.getItem();
            if (notebook.is(ModItems.TRAVELERS_NOTEBOOK)) {
                CustomData customData = notebook.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    ListTag listTag = tag.getListOrEmpty("Items");
                    for (int i = 0; i < listTag.size(); i++) {
                        CompoundTag itemTag = listTag.getCompoundOrEmpty(i);
                        ItemStack insideStack = me.tuanzi.util.ItemStackNbtHelper.read(entity.level().registryAccess(), itemTag.getCompoundOrEmpty("Item"));
                        if (!insideStack.isEmpty()) {
                            ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), insideStack);
                            drop.setDefaultPickUpDelay();
                            entity.level().addFreshEntity(drop);
                        }
                    }
                }
            }
        }
    }
}
}
