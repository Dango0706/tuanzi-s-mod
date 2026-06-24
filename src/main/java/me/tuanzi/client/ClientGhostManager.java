package me.tuanzi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientGhostManager {
    private static final List<Display.BlockDisplay> activeGhosts = new ArrayList<>();
    private static ItemStack lastCheckedStack = ItemStack.EMPTY;
    private static BlockPos lastGhostPos = null;
    private static BlockPos lastOffset = null;
    private static int lastRotation = -1;
    private static boolean lastMirrored = false;
    private static boolean isHidden = false;
    private static int nextGhostId = -10000;

    // 客户端虚影的最大渲染方块数限制，默认 5000 以免爆显存
    private static final int MAX_GHOST_BLOCKS = 5000;

    public static void toggleVisibility() {
        isHidden = !isHidden;
        for (Display.BlockDisplay ghost : activeGhosts) {
            ghost.setInvisible(isHidden);
        }
    }

    public static boolean isHidden() {
        return isHidden;
    }

    public static void clearGhosts() {
        for (Display.BlockDisplay ghost : activeGhosts) {
            ghost.discard();
        }
        activeGhosts.clear();
        lastCheckedStack = ItemStack.EMPTY;
        lastGhostPos = null;
        lastOffset = null;
        lastRotation = -1;
        lastMirrored = false;
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            clearGhosts();
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        ItemStack targetStack = ItemStack.EMPTY;

        // 寻找手持结构蓝图
        if (mainHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
            targetStack = mainHand;
        } else if (offHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
            targetStack = offHand;
        }

        if (targetStack.isEmpty()) {
            clearGhosts();
            return;
        }

        CustomData customData = targetStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            clearGhosts();
            return;
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.getBooleanOr("GhostPlaced", false)) {
            clearGhosts();
            return;
        }

        BlockPos ghostPos = new BlockPos(tag.getIntOr("GhostX", 0), tag.getIntOr("GhostY", 0), tag.getIntOr("GhostZ", 0));
        BlockPos offset = new BlockPos(tag.getIntOr("OffsetX", 0), tag.getIntOr("OffsetY", 0), tag.getIntOr("OffsetZ", 0));
        int rotation = tag.getIntOr("Rotation", 0);
        boolean mirrored = tag.getBooleanOr("Mirrored", false);

        // 如果蓝图引用改变或位置与旋转参数变化，我们重新刷新/生成虚影
        if (!ItemStack.isSameItemSameComponents(targetStack, lastCheckedStack) ||
                !ghostPos.equals(lastGhostPos) ||
                !offset.equals(lastOffset) ||
                rotation != lastRotation ||
                mirrored != lastMirrored) {

            rebuildGhosts(mc.level, tag, ghostPos, offset, rotation, mirrored);

            lastCheckedStack = targetStack.copy();
            lastGhostPos = ghostPos;
            lastOffset = offset;
            lastRotation = rotation;
            lastMirrored = mirrored;
        }
    }

    private static void rebuildGhosts(ClientLevel level, CompoundTag blueprintTag, BlockPos basePos, BlockPos offset, int rotationIdx, boolean mirrored) {
        // 清理旧虚影
        for (Display.BlockDisplay ghost : activeGhosts) {
            ghost.discard();
        }
        activeGhosts.clear();

        if (!blueprintTag.contains("Width")) return;

        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        ListTag palette = blueprintTag.getListOrEmpty("Palette");
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (blocks.length == 0 || palette.isEmpty()) return;

        // 反序列化方块状态调色板
        BlockState[] blockStates = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag stateNbt = palette.getCompoundOrEmpty(i);
            blockStates[i] = BlockState.CODEC.parse(NbtOps.INSTANCE, stateNbt).result().orElse(Blocks.AIR.defaultBlockState());
        }

        // 旋转和镜像计算
        Rotation rot = Rotation.values()[rotationIdx % 4];
        Mirror mir = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;

        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int originalIdx = y * width * length + z * width + x;
                    if (originalIdx >= blocks.length) continue;

                    int paletteIdx = blocks[originalIdx];
                    if (paletteIdx < 0 || paletteIdx >= blockStates.length) continue;

                    BlockState state = blockStates[paletteIdx];
                    if (state.isAir() || state.is(Blocks.BARRIER)) continue;

                    // 1. 计算旋转坐标
                    int rx = x;
                    int rz = z;
                    int wRot = width;
                    int lRot = length;

                    if (rot == Rotation.CLOCKWISE_90) {
                        rx = length - 1 - z;
                        rz = x;
                        wRot = length;
                        lRot = width;
                    } else if (rot == Rotation.CLOCKWISE_180) {
                        rx = width - 1 - x;
                        rz = length - 1 - z;
                    } else if (rot == Rotation.COUNTERCLOCKWISE_90) {
                        rx = z;
                        rz = width - 1 - x;
                        wRot = length;
                        lRot = width;
                    }

                    // 2. 计算镜像坐标
                    if (mirrored) {
                        rx = wRot - 1 - rx;
                    }

                    // 3. 应用方块状态自身的旋转/镜像
                    BlockState transformedState = state.rotate(rot).mirror(mir);

                    // 4. 生成 BlockDisplay 实体
                    double finalX = basePos.getX() + offset.getX() + rx;
                    double finalY = basePos.getY() + offset.getY() + y;
                    double finalZ = basePos.getZ() + offset.getZ() + rz;

                    Display.BlockDisplay display = new Display.BlockDisplay(net.minecraft.world.entity.EntityTypes.BLOCK_DISPLAY, level);
                    display.setId(nextGhostId--);
                    display.setBlockState(transformedState);
                    display.setPos(finalX, finalY, finalZ);
                    display.addTag("blueprint_ghost");
                    display.setBrightnessOverride(new Brightness(15, 15));
                    display.setInvisible(isHidden);

                    level.addEntity(display);
                    activeGhosts.add(display);

                    count++;
                    if (count >= MAX_GHOST_BLOCKS) {
                        // 达到上限，截断并警告
                        return;
                    }
                }
            }
        }
    }
}
