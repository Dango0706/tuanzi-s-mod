package me.tuanzi.world.item.crafting;

import com.mojang.serialization.MapCodec;
import me.tuanzi.init.ModItems;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 画布扩容升级自定义配方：
 * 3×3 合成栏外圈 8 张纸围绕中间 1 张画布，升级尺寸 16→32→64→128→256（3D 画布上限 128）。
 * 合成时仅写入延迟扩容源 UUID（不破坏磁盘文件），在真正使用时居中扩展原体素数据。
 */
public class CanvasUpgradeRecipe extends CustomRecipe {
    public static final MapCodec<CanvasUpgradeRecipe> MAP_CODEC = MapCodec.unit(CanvasUpgradeRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasUpgradeRecipe> STREAM_CODEC =
            StreamCodec.unit(new CanvasUpgradeRecipe());
    public static final RecipeSerializer<CanvasUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public CanvasUpgradeRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        ItemStack center = input.getItem(1, 1);
        if (!center.is(ModItems.CANVAS)) {
            return false;
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x == 1 && y == 1) {
                    continue;
                }
                if (!input.getItem(x, y).is(Items.PAPER)) {
                    return false;
                }
            }
        }
        CanvasMetadata meta = CanvasMetadata.read(center);
        return !meta.corrupted() && CanvasType.canUpgrade(meta.size(), meta.type());
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack center = input.getItem(1, 1);
        if (!center.is(ModItems.CANVAS)) {
            return ItemStack.EMPTY;
        }
        CanvasMetadata oldMeta = CanvasMetadata.read(center);
        if (oldMeta.corrupted() || !CanvasType.canUpgrade(oldMeta.size(), oldMeta.type())) {
            return ItemStack.EMPTY;
        }
        int nextSize = CanvasType.nextSize(oldMeta.size(), oldMeta.type());
        ItemStack result = new ItemStack(ModItems.CANVAS);

        // 若原画布本身尚未物化，则继承它的升级源（若已有文件则以原 uuid 为源）
        UUID srcUuid = oldMeta.hasPendingUpgrade() ? oldMeta.upgradeSourceUuid() : oldMeta.uuid();
        int srcSize = oldMeta.hasPendingUpgrade() ? oldMeta.upgradeSourceSize() : oldMeta.size();

        CanvasMetadata upgraded = (srcUuid != null)
                ? oldMeta.withUpgradePending(UUID.randomUUID(), nextSize, srcUuid, srcSize)
                : new CanvasMetadata(
                        null, CanvasMetadata.CURRENT_FORMAT_VERSION, nextSize, oldMeta.type(),
                        oldMeta.ownerUuid(), oldMeta.ownerName(), oldMeta.permission(),
                        oldMeta.nonEmptyVoxels(), oldMeta.lastEditTime(), oldMeta.dataHash(),
                        false, 1, null, 0
                );
        CanvasMetadata.write(result, upgraded);
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
