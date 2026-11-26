package me.tuanzi.block.cannon;

import me.tuanzi.util.LitematicaParser;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创造模式管理员蓝图直接放置器：
 * 将蓝图中的方块（含空气覆盖与方块实体）和实体直接放置到虚影对应的世界位置。
 */
public final class BlueprintDirectPlacer {

    private static final int DIRECT_PLACE_FLAGS =
            Block.UPDATE_CLIENTS
                    | Block.UPDATE_KNOWN_SHAPE
                    | Block.UPDATE_SUPPRESS_DROPS
                    | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private static final int CLEAR_BLOCK_ENTITY_FLAGS =
            Block.UPDATE_INVISIBLE
                    | Block.UPDATE_KNOWN_SHAPE
                    | Block.UPDATE_SUPPRESS_DROPS
                    | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                    | Block.UPDATE_SKIP_ON_PLACE;

    private BlueprintDirectPlacer() {
    }

    public record PlaceResult(int placedBlocks, int clearedAirBlocks, int spawnedEntities) {
    }

    /**
     * 将蓝图建筑直接放置到指定的基准坐标与变换姿态下。
     * 蓝图边界框内的空气格（除 STRUCTURE_VOID 外）会清空对应世界方块，实现完全覆盖。
     */
    public static PlaceResult placeBlueprint(
            ServerLevel level,
            CompoundTag blueprintTag,
            BlockPos basePos,
            BlockPos offset,
            int rotationIdx,
            boolean mirrored
    ) {
        blueprintTag = me.tuanzi.util.LitematicaParser.expandBlueprintTag(blueprintTag);
        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (!BlueprintBuildPlanner.isValidDimensionsAndBlocks(width, height, length, blocks.length)) {
            return new PlaceResult(0, 0, 0);
        }
        long layerSize = (long) width * (long) length;
        int effectiveLength = (int) Math.min((long) blocks.length, layerSize * (long) height);

        int normalizedRot = Math.floorMod(rotationIdx, 4);
        Rotation rot = Rotation.values()[normalizedRot];
        Mirror mir = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;

        ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
        BlockState[] decodedPalette = new BlockState[paletteNbt.size()];
        for (int i = 0; i < paletteNbt.size(); i++) {
            Tag stateTag = paletteNbt.get(i);
            BlockState raw = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag)
                    .result()
                    .orElseGet(() -> LitematicaParser.parseBlockStateFromTag(stateTag));
            decodedPalette[i] = raw.rotate(rot).mirror(mir);
        }

        Map<BlockPos, CompoundTag> teMap = new HashMap<>();
        ListTag tileEntities = blueprintTag.getListOrEmpty("BlockEntities");
        for (int i = 0; i < tileEntities.size(); i++) {
            CompoundTag te = tileEntities.getCompoundOrEmpty(i);
            int[] tpos = te.getIntArray("pos").orElse(new int[0]);
            if (tpos.length == 3) {
                teMap.put(new BlockPos(tpos[0], tpos[1], tpos[2]), te.getCompoundOrEmpty("nbt").copy());
            }
        }

        int placedBlocks = 0;
        int clearedAirBlocks = 0;

        for (int idx = 0; idx < effectiveLength; idx++) {
            int y = (int) ((long) idx / layerSize);
            int rem = (int) ((long) idx % layerSize);
            int z = rem / width;
            int x = rem % width;

            if (y >= height) {
                break;
            }

            int paletteIdx = blocks[idx];
            if (paletteIdx < 0 || paletteIdx >= decodedPalette.length) {
                continue;
            }

            BlockState targetState = decodedPalette[paletteIdx];
            if (targetState.is(Blocks.STRUCTURE_VOID)) {
                continue;
            }

            BlockPos targetPos = BlueprintBuildPlanner.transformLocalPos(
                    x, y, z, width, length, rot, mirrored, basePos, offset
            );
            if (!level.isInValidBounds(targetPos)) {
                continue;
            }

            if (targetState.isAir()) {
                if (!level.getBlockState(targetPos).isAir()) {
                    level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), DIRECT_PLACE_FLAGS);
                    clearedAirBlocks++;
                }
                continue;
            }

            CompoundTag teData = teMap.get(new BlockPos(x, y, z));
            if (teData != null) {
                // 先以无副作用占位清理目标格既有方块实体，避免同方块状态被跳过或旧容器内容残留
                level.setBlock(targetPos, Blocks.BARRIER.defaultBlockState(), CLEAR_BLOCK_ENTITY_FLAGS);
            }

            level.setBlock(targetPos, targetState, DIRECT_PLACE_FLAGS);
            placedBlocks++;

            if (teData != null) {
                BlockEntity be = level.getBlockEntity(targetPos);
                if (be != null) {
                    CompoundTag nbt = teData.copy();
                    nbt.putInt("x", targetPos.getX());
                    nbt.putInt("y", targetPos.getY());
                    nbt.putInt("z", targetPos.getZ());
                    be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt));
                    be.setChanged();
                    level.sendBlockUpdated(targetPos, targetState, targetState, Block.UPDATE_ALL);
                }
            }
        }

        int spawnedEntities = placeBlueprintEntities(
                level, blueprintTag, width, height, length, basePos, offset, normalizedRot, mirrored
        );

        BlockPos centerPos = basePos.offset(offset).offset(width / 2, height / 2, length / 2);
        level.playSound(null, centerPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.2f);

        ModLog.debug("BlueprintDirectPlacer: 直接放置完成 basePos=" + basePos
                + ", offset=" + offset
                + ", rot=" + normalizedRot
                + ", mir=" + mirrored
                + ", placedBlocks=" + placedBlocks
                + ", clearedAir=" + clearedAirBlocks
                + ", spawnedEntities=" + spawnedEntities);

        return new PlaceResult(placedBlocks, clearedAirBlocks, spawnedEntities);
    }

    private static int placeBlueprintEntities(
            ServerLevel level,
            CompoundTag blueprintTag,
            int width,
            int height,
            int length,
            BlockPos basePos,
            BlockPos offset,
            int rotationIdx,
            boolean mirrored
    ) {
        ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");
        if (entitiesTag.isEmpty()) {
            return 0;
        }

        int spawnedCount = 0;
        for (int i = 0; i < entitiesTag.size(); i++) {
            CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(i);
            String id = entityNbt.getStringOr("id", "");
            if (BlueprintEntityHelper.isIgnoredEntity(id)) {
                continue;
            }

            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(id)).orElse(null);
            if (entityType == null) {
                continue;
            }

            BlueprintEntityHelper.TransformedEntity transformed = BlueprintEntityHelper.transformEntity(
                    entityNbt, width, height, length, basePos, offset, rotationIdx, mirrored, level.registryAccess()
            );
            Vec3 exactPos = transformed.exactPos();
            BlockPos targetBlockPos = transformed.targetBlockPos();
            Direction facing = transformed.facing();

            if (!level.isInValidBounds(targetBlockPos) || isDuplicateEntityPresent(level, id, entityType, exactPos, targetBlockPos, facing)) {
                continue;
            }

            CompoundTag sanitizedNbt = BlueprintEntityHelper.buildSanitizedEntityNbt(
                    transformed.transformedNbt(), id, Map.of(), true, level.registryAccess()
            );
            if (LaunchedItem.ForEntity.spawnEntity(level, targetBlockPos, exactPos, id, sanitizedNbt)) {
                spawnedCount++;
            }
        }
        return spawnedCount;
    }

    private static boolean isDuplicateEntityPresent(
            ServerLevel level,
            String id,
            EntityType<?> entityType,
            Vec3 exactPos,
            BlockPos targetBlockPos,
            Direction facing
    ) {
        if (id.contains("item_frame") || id.equals("minecraft:painting")) {
            List<HangingEntity> hangings = level.getEntitiesOfClass(
                    HangingEntity.class, new AABB(targetBlockPos).inflate(0.5)
            );
            for (HangingEntity hanging : hangings) {
                if (hanging.getDirection() == facing && hanging.getType() == entityType) {
                    return true;
                }
            }
            return false;
        }

        AABB checkBox = new AABB(
                exactPos.x - 0.5, exactPos.y - 0.5, exactPos.z - 0.5,
                exactPos.x + 0.5, exactPos.y + 0.5, exactPos.z + 0.5
        );
        List<? extends Entity> existing = level.getEntities(entityType, checkBox, e -> true);
        return !existing.isEmpty();
    }
}
