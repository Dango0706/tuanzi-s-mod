package me.tuanzi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.client.renderer.GhostSubmitNodeCollector;
import me.tuanzi.util.LitematicaParser;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ClientGhostManager {

    private static final List<GhostBlock> parsedGhostBlocks = new ArrayList<>();
    private static final Map<BlockState, BlockModelRenderState> cachedModelStates = new HashMap<>();
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static CompoundTag activeBlueprintTag = null;
    private static BlockPos basePos = null;
    private static BlockPos offset = BlockPos.ZERO;
    private static int rotationIdx = 0;
    private static boolean mirrored = false;
    private static boolean isHidden = false;
    private static int maxLayer = 0;
    private static boolean singleLayerMode = false;
    private static boolean smartAssistance = true;
    private static boolean boundToHeldItem = false;
    private static int structureWidth = 0;
    private static int structureHeight = 0;
    private static int structureLength = 0;
    private static ItemStack lastCheckedStack = ItemStack.EMPTY;
    private static boolean loggedOnce = false;

    public static void toggleVisibility() {
        isHidden = !isHidden;
        ModLog.debug("[ClientGhostManager] 切换虚影可见性: isHidden=" + isHidden);
    }

    public static boolean isHidden() {
        return isHidden;
    }

    public static void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public static boolean isSmartAssistance() {
        return smartAssistance;
    }

    public static void setSmartAssistance(boolean enabled) {
        smartAssistance = enabled;
        ModLog.debug("[ClientGhostManager] 智能建造纠错辅助: " + smartAssistance);
    }

    public static void setLayerSlicing(int layer, boolean singleLayer) {
        int maxAllowed = Math.max(0, structureHeight - 1);
        maxLayer = Math.clamp(layer, 0, maxAllowed);
        singleLayerMode = singleLayer;
        ModLog.debug("[ClientGhostManager] 切层更新: maxLayer=" + maxLayer + "/" + maxAllowed + ", singleLayerMode=" + singleLayerMode);
    }

    public static int getMaxLayer() {
        return maxLayer;
    }

    public static boolean isSingleLayerMode() {
        return singleLayerMode;
    }

    public static BlockPos getBasePos() {
        return basePos;
    }

    public static BlockPos getOffset() {
        return offset;
    }

    public static int getRotationIdx() {
        return rotationIdx;
    }

    public static boolean isMirrored() {
        return mirrored;
    }

    public static int getStructureWidth() {
        return structureWidth;
    }

    public static int getStructureHeight() {
        return structureHeight;
    }

    public static int getStructureLength() {
        return structureLength;
    }

    public static CompoundTag getActiveBlueprintTag() {
        return activeBlueprintTag;
    }

    public static boolean hasActiveGhost() {
        return basePos != null && !parsedGhostBlocks.isEmpty();
    }

    public static boolean isBoundToHeldItem() {
        return boundToHeldItem;
    }

    public static void clearGhosts() {
        if (basePos != null || !parsedGhostBlocks.isEmpty()) {
            ModLog.debug("[ClientGhostManager] 已清除世界蓝图虚影");
        }
        loggedOnce = false;
        parsedGhostBlocks.clear();
        cachedModelStates.clear();
        activeBlueprintTag = null;
        basePos = null;
        offset = BlockPos.ZERO;
        rotationIdx = 0;
        mirrored = false;
        boundToHeldItem = false;
        structureWidth = 0;
        structureHeight = 0;
        structureLength = 0;
        maxLayer = 0;
        singleLayerMode = false;
        lastCheckedStack = ItemStack.EMPTY;
    }

    public static void setGhost(CompoundTag blueprintTag, BlockPos base, BlockPos off, int rot, boolean mir) {
        setGhostInternal(blueprintTag, base, off, rot, mir, false);
    }

    private static void setGhostInternal(CompoundTag blueprintTag, BlockPos base, BlockPos off, int rot, boolean mir, boolean fromHeldItem) {
        loggedOnce = false;
        if (blueprintTag == null || !blueprintTag.contains("Width")) {
            clearGhosts();
            return;
        }

        activeBlueprintTag = blueprintTag.copy();
        basePos = base;
        offset = off != null ? off : BlockPos.ZERO;
        rotationIdx = (rot % 4 + 4) % 4;
        mirrored = mir;
        isHidden = false;
        boundToHeldItem = fromHeldItem;

        rebuildGhostBlocks();
        maxLayer = Math.max(0, structureHeight - 1);
        singleLayerMode = false;
        ModLog.debug("[ClientGhostManager] 激活蓝图虚影: basePos=" + basePos + ", offset=" + offset
                + ", rot=" + rotationIdx + ", mir=" + mirrored + ", blocks=" + parsedGhostBlocks.size()
                + ", size=" + structureWidth + "x" + structureHeight + "x" + structureLength
                + ", boundToHeldItem=" + boundToHeldItem);
    }

    public static void updateTransform(BlockPos newOffset, int newRot, boolean newMir) {
        offset = newOffset != null ? newOffset : BlockPos.ZERO;
        int normalizedRot = (newRot % 4 + 4) % 4;
        if (rotationIdx != normalizedRot || mirrored != newMir) {
            rotationIdx = normalizedRot;
            mirrored = newMir;
            rebuildGhostBlocks();
        }
        if (structureHeight > 0) {
            maxLayer = Math.clamp(maxLayer, 0, structureHeight - 1);
        }
        ModLog.debug("[ClientGhostManager] 更新虚影变换: offset=" + offset + ", rot=" + rotationIdx
                + ", mir=" + mirrored + ", size=" + structureWidth + "x" + structureHeight + "x" + structureLength);
    }

    private static void rebuildGhostBlocks() {
        parsedGhostBlocks.clear();
        cachedModelStates.clear();
        if (activeBlueprintTag == null) return;

        int width = activeBlueprintTag.getIntOr("Width", 0);
        int height = activeBlueprintTag.getIntOr("Height", 0);
        int length = activeBlueprintTag.getIntOr("Length", 0);
        ListTag palette = activeBlueprintTag.getListOrEmpty("Palette");
        int[] blocks = activeBlueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (blocks.length == 0 || palette.isEmpty() || width <= 0 || height <= 0 || length <= 0) return;

        Rotation rot = Rotation.values()[rotationIdx % 4];
        Mirror mir = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;

        if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
            structureWidth = length;
            structureLength = width;
        } else {
            structureWidth = width;
            structureLength = length;
        }
        structureHeight = height;

        // 反序列化方块调色板（兼容 StringTag 默认状态、{id, properties} 与 {Name, Properties}）
        BlockState[] blockStates = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            blockStates[i] = LitematicaParser.parseBlockStateFromTag(palette.get(i));
        }

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int originalIdx = y * width * length + z * width + x;
                    if (originalIdx >= blocks.length) continue;

                    int paletteIdx = blocks[originalIdx];
                    if (paletteIdx < 0 || paletteIdx >= blockStates.length) continue;

                    BlockState state = blockStates[paletteIdx];
                    if (state.isAir() || state.is(Blocks.BARRIER)) continue;

                    // 计算旋转
                    int rx = x;
                    int rz = z;
                    int wRot = width;

                    if (rot == Rotation.CLOCKWISE_90) {
                        rx = length - 1 - z;
                        rz = x;
                        wRot = length;
                    } else if (rot == Rotation.CLOCKWISE_180) {
                        rx = width - 1 - x;
                        rz = length - 1 - z;
                    } else if (rot == Rotation.COUNTERCLOCKWISE_90) {
                        rx = z;
                        rz = width - 1 - x;
                        wRot = length;
                    }

                    // 计算镜像
                    if (mirrored) {
                        rx = wRot - 1 - rx;
                    }

                    BlockState transformedState = state.rotate(rot).mirror(mir);
                    parsedGhostBlocks.add(new GhostBlock(rx, y, rz, transformedState));
                }
            }
        }
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        ItemStack targetStack = ItemStack.EMPTY;

        if (mainHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
            targetStack = mainHand;
        } else if (offHand.getItem() instanceof me.tuanzi.item.StructureBlueprintItem) {
            targetStack = offHand;
        }

        // 如果玩家当前手持结构蓝图，自动同步其放置/清除与姿态变化
        if (!targetStack.isEmpty()) {
            CustomData customData = targetStack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.getBooleanOr("GhostPlaced", false)) {
                    BlockPos ghostPos = new BlockPos(tag.getIntOr("GhostX", 0), tag.getIntOr("GhostY", 0), tag.getIntOr("GhostZ", 0));
                    BlockPos off = new BlockPos(tag.getIntOr("OffsetX", 0), tag.getIntOr("OffsetY", 0), tag.getIntOr("OffsetZ", 0));
                    int rot = tag.getIntOr("Rotation", 0);
                    boolean mir = tag.getBooleanOr("Mirrored", false);

                    if (!boundToHeldItem || basePos == null || !ghostPos.equals(basePos) || activeBlueprintTag == null) {
                        setGhostInternal(tag, ghostPos, off, rot, mir, true);
                        lastCheckedStack = targetStack.copy();
                    } else if (!ItemStack.isSameItemSameComponents(targetStack, lastCheckedStack)
                            || !off.equals(offset) || rot != rotationIdx || mir != mirrored) {
                        updateTransform(off, rot, mir);
                        lastCheckedStack = targetStack.copy();
                    }
                } else if (boundToHeldItem && hasActiveGhost()) {
                    // 手持蓝图执行了右键二次点击或 Shift+右键清除虚影
                    clearGhosts();
                }
            } else if (boundToHeldItem && hasActiveGhost()) {
                clearGhosts();
            }
        }
    }

    public static void renderWorldGhosts(LevelRenderContext context) {
        if (isHidden || basePos == null || parsedGhostBlocks.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Vec3 camPos = (context.levelState() != null && context.levelState().cameraRenderState != null && context.levelState().cameraRenderState.pos != null)
                ? context.levelState().cameraRenderState.pos
                : mc.gameRenderer.mainCamera().position();
        SubmitNodeCollector collector = context.submitNodeCollector();
        PoseStack poseStack = context.poseStack() != null ? context.poseStack() : new PoseStack();
        if (!loggedOnce) {
            ModLog.debug("[ClientGhostManager] renderWorldGhosts 运行中: basePos=" + basePos + ", blocks=" + parsedGhostBlocks.size());
            loggedOnce = true;
        }

        // 1. 结构三维边界框线 (青色线框)
        int rootX = basePos.getX() + offset.getX();
        int rootY = basePos.getY() + offset.getY();
        int rootZ = basePos.getZ() + offset.getZ();

        double distSqToCam = (rootX + structureWidth / 2.0 - camPos.x) * (rootX + structureWidth / 2.0 - camPos.x)
                + (rootY + structureHeight / 2.0 - camPos.y) * (rootY + structureHeight / 2.0 - camPos.y)
                + (rootZ + structureLength / 2.0 - camPos.z) * (rootZ + structureLength / 2.0 - camPos.z);

        if (distSqToCam > 160.0 * 160.0) {
            return; // 视距过远剔除
        }

        poseStack.pushPose();
        poseStack.translate(rootX - camPos.x, rootY - camPos.y, rootZ - camPos.z);
        collector.submitShapeOutline(
                poseStack,
                Shapes.box(0, 0, 0, structureWidth, structureHeight, structureLength),
                RenderTypes.lines(),
                0xFF00E5FF,
                2.0F,
                true
        );
        poseStack.popPose();

        // 2. 收集与绘制方块虚影
        List<GhostBlock> toRenderBlocks = new ArrayList<>();
        List<BlockPos> mismatchPositions = new ArrayList<>();

        for (GhostBlock gb : parsedGhostBlocks) {
            // 切层过滤
            if (singleLayerMode) {
                if (gb.relY != maxLayer) continue;
            } else {
                if (gb.relY > maxLayer) continue;
            }

            int worldX = rootX + gb.relX;
            int worldY = rootY + gb.relY;
            int worldZ = rootZ + gb.relZ;
            BlockPos worldPos = new BlockPos(worldX, worldY, worldZ);

            double dSq = worldPos.distToCenterSqr(camPos);
            if (dSq > 96.0 * 96.0) continue; // 单块视距裁剪

            if (smartAssistance) {
                BlockState worldState = level.getBlockState(worldPos);
                if (worldState.equals(gb.state)) {
                    // 正确方块已放置，隐去虚影
                    continue;
                } else if (!worldState.isAir()) {
                    // 错位放置方块，加入红框告警列表
                    mismatchPositions.add(worldPos);
                    continue;
                }
            }

            toRenderBlocks.add(gb);
        }

        // 绘制错位方块的红框提示
        for (BlockPos mismatchPos : mismatchPositions) {
            poseStack.pushPose();
            poseStack.translate(mismatchPos.getX() - camPos.x, mismatchPos.getY() - camPos.y, mismatchPos.getZ() - camPos.z);
            collector.submitShapeOutline(
                    poseStack,
                    Shapes.block(),
                    RenderTypes.lines(),
                    0xFFFF3333,
                    2.5F,
                    true
            );
            poseStack.popPose();
        }

        // 绘制半透明 0.4 透明度方块虚影（按 BlockState 缓存模型状态，零冗余重烘焙）
        if (!toRenderBlocks.isEmpty()) {
            GhostSubmitNodeCollector ghostCollector = new GhostSubmitNodeCollector(collector, 0.4F);

            for (GhostBlock gb : toRenderBlocks) {
                BlockModelRenderState cachedState = cachedModelStates.computeIfAbsent(gb.state, st -> {
                    BlockModelRenderState rs = new BlockModelRenderState();
                    mc.getModelManager().getBlockModelSet().get(st).update(rs, st, DISPLAY_CONTEXT, 42L);
                    return rs;
                });

                int worldX = rootX + gb.relX;
                int worldY = rootY + gb.relY;
                int worldZ = rootZ + gb.relZ;

                poseStack.pushPose();
                poseStack.translate(worldX - camPos.x, worldY - camPos.y, worldZ - camPos.z);
                cachedState.submit(poseStack, ghostCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    public static BlockState parseBlockStateFromTag(net.minecraft.nbt.Tag tag) {
        return LitematicaParser.parseBlockStateFromTag(tag);
    }

    public static BlockState parseBlockStateFromNbt(CompoundTag tag) {
        return LitematicaParser.parseBlockStateFromNbt(tag);
    }

    public record GhostBlock(int relX, int relY, int relZ, BlockState state) {
    }
}
