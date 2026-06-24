package me.tuanzi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BlueprintCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tuanzis_mod_confirm_blueprint")
            .requires(source -> true)
            .executes(BlueprintCommand::confirmBlueprint)
        );
    }

    private static int confirmBlueprint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModItems.BLANK_BLUEPRINT)) {
            stack = player.getOffhandItem();
        }

        if (!stack.is(ModItems.BLANK_BLUEPRINT)) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.not_holding").withStyle(ChatFormatting.RED));
            return 0;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.no_points").withStyle(ChatFormatting.RED));
            return 0;
        }

        CompoundTag pointsTag = customData.copyTag();
        if (!pointsTag.contains("pos1_x") || !pointsTag.contains("pos2_x")) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.incomplete_points").withStyle(ChatFormatting.RED));
            return 0;
        }

        Level level = player.level();
        String dim1 = pointsTag.getStringOr("pos1_dim", "");
        String currentDim = level.dimension().identifier().toString();
        if (!dim1.equals(currentDim)) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.dim_mismatch").withStyle(ChatFormatting.RED));
            return 0;
        }

        int x1 = pointsTag.getIntOr("pos1_x", 0);
        int y1 = pointsTag.getIntOr("pos1_y", 0);
        int z1 = pointsTag.getIntOr("pos1_z", 0);

        int x2 = pointsTag.getIntOr("pos2_x", 0);
        int y2 = pointsTag.getIntOr("pos2_y", 0);
        int z2 = pointsTag.getIntOr("pos2_z", 0);

        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);

        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;

        if (width * height * length > 262144) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.too_large").withStyle(ChatFormatting.RED));
            return 0;
        }

        List<BlockState> paletteList = new ArrayList<>();
        int[] blocks = new int[width * height * length];
        ListTag blockEntitiesTag = new ListTag();

        ModLog.debug(player, null, "开始构建蓝图数据。体积: " + width + "x" + height + "x" + length + "=" + (width * height * length));

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos p = new BlockPos(minX + x, minY + y, minZ + z);
                    BlockState state = level.getBlockState(p);
                    
                    int paletteIdx = -1;
                    for (int i = 0; i < paletteList.size(); i++) {
                        if (paletteList.get(i).equals(state)) {
                            paletteIdx = i;
                            break;
                        }
                    }
                    if (paletteIdx == -1) {
                        paletteIdx = paletteList.size();
                        paletteList.add(state);
                    }

                    int idx = y * width * length + z * width + x;
                    blocks[idx] = paletteIdx;

                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null) {
                        CompoundTag teNbt = be.saveWithFullMetadata(level.registryAccess());
                        CompoundTag record = new CompoundTag();
                        record.putIntArray("pos", new int[]{x, y, z});
                        record.put("nbt", teNbt);
                        blockEntitiesTag.add(record);
                    }
                }
            }
        }

        // 捕获实体 (排除玩家)
        net.minecraft.world.phys.AABB boundingBox = new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        List<net.minecraft.world.entity.Entity> entities = level.getEntities((net.minecraft.world.entity.Entity)null, boundingBox, entity -> !(entity instanceof net.minecraft.world.entity.player.Player));
        ListTag entitiesTag = new ListTag();
        for (net.minecraft.world.entity.Entity entity : entities) {
            net.minecraft.world.level.storage.TagValueOutput valueOutput = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
            entity.saveWithoutId(valueOutput);
            CompoundTag entityNbt = valueOutput.buildResult();
            String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            entityNbt.putString("id", typeId);
            entityNbt.remove("UUID");

            double relX = entity.getX() - minX;
            double relY = entity.getY() - minY;
            double relZ = entity.getZ() - minZ;

            ListTag relativePos = new ListTag();
            relativePos.add(net.minecraft.nbt.DoubleTag.valueOf(relX));
            relativePos.add(net.minecraft.nbt.DoubleTag.valueOf(relY));
            relativePos.add(net.minecraft.nbt.DoubleTag.valueOf(relZ));
            entityNbt.put("Pos", relativePos);

            entitiesTag.add(entityNbt);
        }

        ListTag paletteNbt = new ListTag();
        for (BlockState state : paletteList) {
            Tag tag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state)
                .result()
                .orElse(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, Blocks.AIR.defaultBlockState()).result().orElse(new CompoundTag()));
            paletteNbt.add(tag);
        }

        CompoundTag blueprintData = new CompoundTag();
        blueprintData.putInt("Width", width);
        blueprintData.putInt("Height", height);
        blueprintData.putInt("Length", length);
        blueprintData.put("Palette", paletteNbt);
        blueprintData.putIntArray("Blocks", blocks);
        blueprintData.put("BlockEntities", blockEntitiesTag);
        blueprintData.put("Entities", entitiesTag);

        blueprintData.putBoolean("GhostPlaced", false);
        blueprintData.putInt("GhostX", 0);
        blueprintData.putInt("GhostY", 0);
        blueprintData.putInt("GhostZ", 0);
        blueprintData.putInt("OffsetX", 0);
        blueprintData.putInt("OffsetY", 0);
        blueprintData.putInt("OffsetZ", 0);
        blueprintData.putInt("Rotation", 0);
        blueprintData.putBoolean("Mirrored", false);

        stack.shrink(1);

        ItemStack structureBlueprint = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
        structureBlueprint.set(DataComponents.CUSTOM_DATA, CustomData.of(blueprintData));

        if (!player.getInventory().add(structureBlueprint)) {
            player.drop(structureBlueprint, false);
        }

        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.record_success", width, height, length).withStyle(ChatFormatting.GREEN));
        ModLog.debug(player, null, "蓝图构筑完成并生成结构蓝图！");

        return 1;
    }
}
