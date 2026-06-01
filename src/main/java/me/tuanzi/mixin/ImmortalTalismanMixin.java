package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ImmortalTalismanMixin {

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void tuanzis_mod$onRestoreFrom(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        if (!keepEverything) {
            boolean hadTalisman = oldPlayer.getInventory().contains(stack -> stack.is(ModItems.IMMORTAL_TALISMAN));
            if (hadTalisman) {
                ServerPlayer newPlayer = (ServerPlayer) (Object) this;
                
                // 手动复制物品栏以避免共享引用，防止消耗最后一个圣符时导致 oldPlayer 的 check 失效
                for (int i = 0; i < oldPlayer.getInventory().getContainerSize(); i++) {
                    newPlayer.getInventory().setItem(i, oldPlayer.getInventory().getItem(i).copy());
                }
                newPlayer.getInventory().setSelectedSlot(oldPlayer.getInventory().getSelectedSlot());

                newPlayer.experienceLevel = oldPlayer.experienceLevel;
                newPlayer.totalExperience = oldPlayer.totalExperience;
                newPlayer.experienceProgress = oldPlayer.experienceProgress;
                newPlayer.setScore(oldPlayer.getScore());

                // 消耗一个不朽圣符
                for (int i = 0; i < newPlayer.getInventory().getContainerSize(); i++) {
                    ItemStack stack = newPlayer.getInventory().getItem(i);
                    if (stack.is(ModItems.IMMORTAL_TALISMAN)) {
                        stack.shrink(1);
                        me.tuanzi.util.ModLog.debug(newPlayer, null, "不朽圣符触发成功：扣除 1 个不朽圣符，保留了该玩家死亡前的所有物品与经验值（剩余不朽圣符: " + stack.getCount() + " 个）。");
                        break;
                    }
                }
            }
        }
    }
}
