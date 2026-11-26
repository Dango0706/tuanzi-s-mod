package me.tuanzi.mixin;

import me.tuanzi.util.LitematicaParser;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在掉落物实体写入同步物品栈前，确保结构蓝图已完成 GZIP 压缩，
 * 防止 {@code clientbound/minecraft:set_entity_data} 触发 2MB NBT 解码异常。
 */
@Mixin(ItemEntity.class)
public class BlueprintItemEntityMixin {

    @Inject(method = "setItem", at = @At("HEAD"))
    private void tuanzis_mod$compactBlueprintBeforeEntitySync(ItemStack itemStack, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide()) {
            LitematicaParser.compactItemStackIfNeeded(itemStack);
        }
    }
}
