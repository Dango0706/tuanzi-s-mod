package me.tuanzi.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StructureBlueprintItem extends Item {

    public static final int MAX_DEFAULT_ENTITY_LINES = 4;
    private static final int ENTITY_CACHE_MAX_ENTRIES = 16;

    public record EntityTooltipEntry(String id, double x, double y, double z) {
    }

    private record CompressedPayloadKey(byte[] bytes, int hash) {
        static CompressedPayloadKey of(byte[] bytes) {
            return new CompressedPayloadKey(bytes, Arrays.hashCode(bytes));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CompressedPayloadKey other)) return false;
            return this.hash == other.hash && Arrays.equals(this.bytes, other.bytes);
        }

        @Override
        public int hashCode() {
            return this.hash;
        }
    }

    private static final Map<CompressedPayloadKey, List<EntityTooltipEntry>> COMPRESSED_ENTITY_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(ENTITY_CACHE_MAX_ENTRIES, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CompressedPayloadKey, List<EntityTooltipEntry>> eldest) {
                    return size() > ENTITY_CACHE_MAX_ENTRIES;
                }
            });

    public StructureBlueprintItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        me.tuanzi.util.LitematicaParser.compactItemStackIfNeeded(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos clickPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();

            if (!tag.contains("Width")) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.invalid").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }

            boolean ghostPlaced = tag.getBooleanOr("GhostPlaced", false);
            if (!ghostPlaced) {
                // 在点击方块的位置（朝上偏移 1 格）生成虚影
                BlockPos targetPos = clickPos.relative(context.getClickedFace());
                tag.putBoolean("GhostPlaced", true);
                tag.putInt("GhostX", targetPos.getX());
                tag.putInt("GhostY", targetPos.getY());
                tag.putInt("GhostZ", targetPos.getZ());
                tag.putInt("OffsetX", 0);
                tag.putInt("OffsetY", 0);
                tag.putInt("OffsetZ", 0);
                tag.putInt("Rotation", 0); // 0, 1, 2, 3
                tag.putBoolean("Mirrored", false);

                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(me.tuanzi.util.LitematicaParser.compactBlueprintTag(tag)));
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.containerMenu.broadcastChanges();
                }
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_placed", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN));
            } else {
                // 已生成虚影时，再次点击将其清除
                tag.remove("GhostPlaced");
                tag.remove("GhostX");
                tag.remove("GhostY");
                tag.remove("GhostZ");
                tag.remove("OffsetX");
                tag.remove("OffsetY");
                tag.remove("OffsetZ");
                tag.remove("Rotation");
                tag.remove("Mirrored");

                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(me.tuanzi.util.LitematicaParser.compactBlueprintTag(tag)));
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.containerMenu.broadcastChanges();
                }
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_cleared").withStyle(ChatFormatting.YELLOW));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
            if (tag.getBooleanOr("GhostPlaced", false)) {
                tag.remove("GhostPlaced");
                tag.remove("GhostX");
                tag.remove("GhostY");
                tag.remove("GhostZ");
                tag.remove("OffsetX");
                tag.remove("OffsetY");
                tag.remove("OffsetZ");
                tag.remove("Rotation");
                tag.remove("Mirrored");
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(me.tuanzi.util.LitematicaParser.compactBlueprintTag(tag)));
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.containerMenu.broadcastChanges();
                }
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_cleared").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("Width")) {
                int w = tag.getIntOr("Width", 0);
                int h = tag.getIntOr("Height", 0);
                int l = tag.getIntOr("Length", 0);
                int blockCount = tag.contains(me.tuanzi.util.LitematicaParser.KEY_BLOCK_COUNT)
                        ? tag.getIntOr(me.tuanzi.util.LitematicaParser.KEY_BLOCK_COUNT, 0)
                        : tag.getIntArray("Blocks").orElse(new int[0]).length;
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.size", w, h, l).withStyle(ChatFormatting.GRAY));
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.blocks", blockCount).withStyle(ChatFormatting.GRAY));

                // 实体列表显示：默认最多显示 4 行，超出时按住 Shift 展开全部
                appendEntityTooltip(tag, isShiftDown(), builder);
                if (tag.getBooleanOr("GhostPlaced", false)) {
                    builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.ghost_active",
                            tag.getIntOr("GhostX", 0) + tag.getIntOr("OffsetX", 0),
                            tag.getIntOr("GhostY", 0) + tag.getIntOr("OffsetY", 0),
                            tag.getIntOr("GhostZ", 0) + tag.getIntOr("OffsetZ", 0)
                    ).withStyle(ChatFormatting.AQUA));
                }
            } else {
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.empty").withStyle(ChatFormatting.RED));
            }
        } else {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.empty").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
    }

    private static boolean isShiftDown() {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            return me.tuanzi.client.ClientTooltipHelper.isShiftDown();
        }
        return false;
    }

    public static void appendEntityTooltip(CompoundTag tag, boolean shiftDown, Consumer<Component> builder) {
        List<EntityTooltipEntry> entries = resolveEntityEntries(tag);
        if (entries.isEmpty()) {
            int entityCount = tag.getIntOr("EntityCount", 0);
            if (entityCount > 0) {
                builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.entities", entityCount).withStyle(ChatFormatting.DARK_PURPLE));
            }
            return;
        }

        builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.entities", entries.size()).withStyle(ChatFormatting.DARK_PURPLE));
        boolean showAll = shiftDown || entries.size() <= MAX_DEFAULT_ENTITY_LINES;
        int displayCount = showAll ? entries.size() : MAX_DEFAULT_ENTITY_LINES;

        for (int i = 0; i < displayCount; i++) {
            EntityTooltipEntry entry = entries.get(i);
            String id = entry.id();
            String displayName = id;
            if (id.contains(":")) {
                displayName = id.substring(id.indexOf(":") + 1);
            }
            Component entityName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getOptional(net.minecraft.resources.Identifier.tryParse(id))
                    .map(net.minecraft.world.entity.EntityType::getDescription)
                    .orElse(Component.literal(displayName));
            builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.entity_entry", entityName, entry.x(), entry.y(), entry.z())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (!showAll) {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.blueprint.entities_more", entries.size() - MAX_DEFAULT_ENTITY_LINES)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static List<EntityTooltipEntry> resolveEntityEntries(CompoundTag tag) {
        if (tag == null) {
            return List.of();
        }
        if (tag.contains("Entities")) {
            return extractEntityEntries(tag.getListOrEmpty("Entities"));
        }
        if (tag.getIntOr("EntityCount", 0) > 0 && tag.contains(me.tuanzi.util.LitematicaParser.KEY_COMPRESSED_PAYLOAD)) {
            byte[] compressed = tag.getByteArray(me.tuanzi.util.LitematicaParser.KEY_COMPRESSED_PAYLOAD).orElse(new byte[0]);
            if (compressed.length == 0) {
                return List.of();
            }
            CompressedPayloadKey key = CompressedPayloadKey.of(compressed);
            List<EntityTooltipEntry> cached = COMPRESSED_ENTITY_CACHE.get(key);
            if (cached != null) {
                return cached;
            }
            CompoundTag expanded = me.tuanzi.util.LitematicaParser.expandBlueprintTag(tag);
            List<EntityTooltipEntry> extracted = expanded != null && expanded.contains("Entities")
                    ? List.copyOf(extractEntityEntries(expanded.getListOrEmpty("Entities")))
                    : List.of();
            COMPRESSED_ENTITY_CACHE.put(key, extracted);
            me.tuanzi.util.ModLog.debug("[StructureBlueprintItem] 解压并缓存压缩蓝图实体列表: count=" + extracted.size());
            return extracted;
        }
        return List.of();
    }

    private static List<EntityTooltipEntry> extractEntityEntries(net.minecraft.nbt.ListTag entitiesTag) {
        if (entitiesTag == null || entitiesTag.isEmpty()) {
            return List.of();
        }
        List<EntityTooltipEntry> entries = new ArrayList<>(entitiesTag.size());
        for (int i = 0; i < entitiesTag.size(); i++) {
            CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(i);
            net.minecraft.nbt.ListTag posList = entityNbt.getListOrEmpty("Pos");
            if (posList.size() >= 3) {
                String id = entityNbt.getStringOr("id", "");
                double rx = posList.getDoubleOr(0, 0.0);
                double ry = posList.getDoubleOr(1, 0.0);
                double rz = posList.getDoubleOr(2, 0.0);
                entries.add(new EntityTooltipEntry(id, rx, ry, rz));
            }
        }
        return entries;
    }
}
