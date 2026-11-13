package me.tuanzi.block.cannon;

import me.tuanzi.block.entity.BlueprintCannonBlockEntity.ContainerSource;
import me.tuanzi.init.ModItems;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 蓝图大炮实体构建辅助类：
 * 处理实体的分类过滤、精确坐标/朝向/锚点变换、所需材料提取与原子化箱内物品扣除。
 */
public class BlueprintEntityHelper {

    /**
     * 判断实体是否为临时/环境/无需打印的实体（录制与大炮打印时直接过滤跳过）
     */
    public static boolean isIgnoredEntity(@Nullable String entityId) {
        if (entityId == null || entityId.isEmpty()) return true;
        if (entityId.startsWith("minecraft:player") || entityId.equals("minecraft:item") || entityId.equals("minecraft:experience_orb") || entityId.equals("minecraft:lightning_bolt")) {
            return true;
        }
        // 投射物与环境实体
        if (entityId.contains("arrow") || entityId.contains("potion") || entityId.contains("fireball")
                || entityId.contains("projectile") || entityId.contains("wind_charge") || entityId.contains("trident")
                || entityId.equals("minecraft:egg") || entityId.equals("minecraft:snowball") || entityId.equals("minecraft:ender_pearl")
                || entityId.equals("minecraft:eye_of_ender") || entityId.equals("minecraft:firework_rocket")
                || entityId.equals("minecraft:falling_block") || entityId.equals("minecraft:tnt")
                || entityId.equals("minecraft:leash_knot") || entityId.equals("minecraft:area_effect_cloud")
                || entityId.equals("minecraft:spawner_minecart") || entityId.equals("minecraft:command_block_minecart")) {
            return true;
        }
        // 本模组中的临时战斗/交互实体
        if (entityId.equals("tuanzis_mod:seat") || entityId.equals("tuanzis_mod:chair_seat") || entityId.equals("tuanzis_mod:decoy")
                || entityId.equals("tuanzis_mod:shuriken") || entityId.equals("tuanzis_mod:crescent_aura")
                || entityId.startsWith("tuanzis_mod:cinder_")) {
            return true;
        }
        return false;
    }

    public static boolean isIgnoredEntity(Entity entity) {
        if (entity == null) return true;
        if (entity instanceof net.minecraft.world.entity.player.Player) return true;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return isIgnoredEntity(id.toString());
    }

    /**
     * 是否为非活体建筑实体（生存模式下大炮允许打印并消耗对应物品的实体）
     */
    public static boolean isBuildableNonLivingEntity(String entityId) {
        if (entityId == null || entityId.isEmpty()) return false;
        String cleanId = entityId.contains(":") ? entityId : "minecraft:" + entityId;

        // 装饰与假人
        if (cleanId.equals("minecraft:item_frame") || cleanId.equals("minecraft:glow_item_frame")
                || cleanId.equals("tuanzis_mod:glass_item_frame") || cleanId.equals("minecraft:painting")
                || cleanId.equals("minecraft:armor_stand") || cleanId.equals("tuanzis_mod:scarecrow")
                || cleanId.equals("tuanzis_mod:trial_dummy") || cleanId.equals("minecraft:end_crystal")
                || cleanId.equals("minecraft:cushion")) {
            return true;
        }

        // 矿车
        if (cleanId.equals("minecraft:minecart") || cleanId.equals("minecraft:chest_minecart")
                || cleanId.equals("minecraft:furnace_minecart") || cleanId.equals("minecraft:tnt_minecart")
                || cleanId.equals("minecraft:hopper_minecart")) {
            return true;
        }

        // 船与竹筏（覆盖全部 22 种船、运输船及竹筏）
        if (cleanId.endsWith("_boat") || cleanId.endsWith("_chest_boat")
                || cleanId.equals("minecraft:bamboo_raft") || cleanId.equals("minecraft:bamboo_chest_raft")
                || cleanId.equals("minecraft:boat") || cleanId.equals("minecraft:chest_boat")) {
            return true;
        }

        return false;
    }

    /**
     * 获取非活体建筑实体本体对应的基准物品 ItemStack（包含特定数据如坐垫颜色）
     */
    public static ItemStack getBaseItemStack(String entityId, CompoundTag nbt, HolderLookup.Provider registries) {
        if (entityId == null || entityId.isEmpty()) return ItemStack.EMPTY;
        String cleanId = entityId.contains(":") ? entityId : "minecraft:" + entityId;

        switch (cleanId) {
            case "minecraft:item_frame":
                return new ItemStack(Items.ITEM_FRAME);
            case "minecraft:glow_item_frame":
                return new ItemStack(Items.GLOW_ITEM_FRAME);
            case "tuanzis_mod:glass_item_frame":
                return new ItemStack(ModItems.GLASS_ITEM_FRAME);
            case "minecraft:painting":
                return new ItemStack(Items.PAINTING);
            case "minecraft:armor_stand":
                return new ItemStack(Items.ARMOR_STAND);
            case "tuanzis_mod:scarecrow":
                return new ItemStack(ModItems.SCARECROW);
            case "tuanzis_mod:trial_dummy": {
                ItemStack stack = new ItemStack(ModItems.TRIAL_DUMMY);
                int dur = nbt.getIntOr("TrialDummyDurability", 32);
                int damage = Math.clamp(32 - dur, 0, 31);
                stack.setDamageValue(damage);
                return stack;
            }
            case "minecraft:end_crystal":
                return new ItemStack(Items.END_CRYSTAL);
            case "minecraft:cushion": {
                DyeColor color = DyeColor.WHITE;
                if (nbt.contains("color")) {
                    String colorStr = nbt.getStringOr("color", "white");
                    color = DyeColor.byName(colorStr, DyeColor.WHITE);
                }
                return new ItemStack(Items.CUSHION.pick(color));
            }
            case "minecraft:minecart":
                return new ItemStack(Items.MINECART);
            case "minecraft:chest_minecart":
                return new ItemStack(Items.CHEST_MINECART);
            case "minecraft:furnace_minecart":
                return new ItemStack(Items.FURNACE_MINECART);
            case "minecraft:tnt_minecart":
                return new ItemStack(Items.TNT_MINECART);
            case "minecraft:hopper_minecart":
                return new ItemStack(Items.HOPPER_MINECART);

            // 船系列
            case "minecraft:oak_boat":
                return new ItemStack(Items.OAK_BOAT);
            case "minecraft:spruce_boat":
                return new ItemStack(Items.SPRUCE_BOAT);
            case "minecraft:birch_boat":
                return new ItemStack(Items.BIRCH_BOAT);
            case "minecraft:jungle_boat":
                return new ItemStack(Items.JUNGLE_BOAT);
            case "minecraft:acacia_boat":
                return new ItemStack(Items.ACACIA_BOAT);
            case "minecraft:cherry_boat":
                return new ItemStack(Items.CHERRY_BOAT);
            case "minecraft:dark_oak_boat":
                return new ItemStack(Items.DARK_OAK_BOAT);
            case "minecraft:pale_oak_boat":
                return new ItemStack(Items.PALE_OAK_BOAT);
            case "minecraft:mangrove_boat":
                return new ItemStack(Items.MANGROVE_BOAT);
            case "minecraft:poplar_boat":
                return new ItemStack(Items.POPLAR_BOAT);
            case "minecraft:bamboo_raft":
                return new ItemStack(Items.BAMBOO_RAFT);

            case "minecraft:oak_chest_boat":
                return new ItemStack(Items.OAK_CHEST_BOAT);
            case "minecraft:spruce_chest_boat":
                return new ItemStack(Items.SPRUCE_CHEST_BOAT);
            case "minecraft:birch_chest_boat":
                return new ItemStack(Items.BIRCH_CHEST_BOAT);
            case "minecraft:jungle_chest_boat":
                return new ItemStack(Items.JUNGLE_CHEST_BOAT);
            case "minecraft:acacia_chest_boat":
                return new ItemStack(Items.ACACIA_CHEST_BOAT);
            case "minecraft:cherry_chest_boat":
                return new ItemStack(Items.CHERRY_CHEST_BOAT);
            case "minecraft:dark_oak_chest_boat":
                return new ItemStack(Items.DARK_OAK_CHEST_BOAT);
            case "minecraft:pale_oak_chest_boat":
                return new ItemStack(Items.PALE_OAK_CHEST_BOAT);
            case "minecraft:mangrove_chest_boat":
                return new ItemStack(Items.MANGROVE_CHEST_BOAT);
            case "minecraft:poplar_chest_boat":
                return new ItemStack(Items.POPLAR_CHEST_BOAT);
            case "minecraft:bamboo_chest_raft":
                return new ItemStack(Items.BAMBOO_CHEST_RAFT);

            // 兼容旧格式 Type 字段
            case "minecraft:boat": {
                String type = nbt.getStringOr("Type", "oak");
                return switch (type) {
                    case "spruce" -> new ItemStack(Items.SPRUCE_BOAT);
                    case "birch" -> new ItemStack(Items.BIRCH_BOAT);
                    case "jungle" -> new ItemStack(Items.JUNGLE_BOAT);
                    case "acacia" -> new ItemStack(Items.ACACIA_BOAT);
                    case "cherry" -> new ItemStack(Items.CHERRY_BOAT);
                    case "dark_oak" -> new ItemStack(Items.DARK_OAK_BOAT);
                    case "pale_oak" -> new ItemStack(Items.PALE_OAK_BOAT);
                    case "mangrove" -> new ItemStack(Items.MANGROVE_BOAT);
                    case "poplar" -> new ItemStack(Items.POPLAR_BOAT);
                    case "bamboo" -> new ItemStack(Items.BAMBOO_RAFT);
                    default -> new ItemStack(Items.OAK_BOAT);
                };
            }
            case "minecraft:chest_boat": {
                String type = nbt.getStringOr("Type", "oak");
                return switch (type) {
                    case "spruce" -> new ItemStack(Items.SPRUCE_CHEST_BOAT);
                    case "birch" -> new ItemStack(Items.BIRCH_CHEST_BOAT);
                    case "jungle" -> new ItemStack(Items.JUNGLE_CHEST_BOAT);
                    case "acacia" -> new ItemStack(Items.ACACIA_CHEST_BOAT);
                    case "cherry" -> new ItemStack(Items.CHERRY_CHEST_BOAT);
                    case "dark_oak" -> new ItemStack(Items.DARK_OAK_CHEST_BOAT);
                    case "pale_oak" -> new ItemStack(Items.PALE_OAK_CHEST_BOAT);
                    case "mangrove" -> new ItemStack(Items.MANGROVE_CHEST_BOAT);
                    case "poplar" -> new ItemStack(Items.POPLAR_CHEST_BOAT);
                    case "bamboo" -> new ItemStack(Items.BAMBOO_CHEST_RAFT);
                    default -> new ItemStack(Items.OAK_CHEST_BOAT);
                };
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 从实体 NBT 中提取所有所需材料（包括本体物品、展示框内物品、盔甲架装备等）
     */
    public static List<EntityRequirement> extractRequirements(String entityId, CompoundTag nbt, HolderLookup.Provider registries) {
        List<EntityRequirement> list = new ArrayList<>();
        ItemStack baseStack = getBaseItemStack(entityId, nbt, registries);
        if (!baseStack.isEmpty()) {
            list.add(new EntityRequirement("BASE", baseStack));
        }

        // 1. 物品展示框内镶嵌物品
        if (entityId.contains("item_frame")) {
            if (nbt.contains("Item")) {
                CompoundTag itemTag = nbt.getCompoundOrEmpty("Item");
                ItemStack framedStack = ItemStackNbtHelper.read(registries, itemTag);
                if (!framedStack.isEmpty()) {
                    list.add(new EntityRequirement("FRAME_ITEM", framedStack));
                }
            }
        }

        // 2. 盔甲架 / 稻草人装备
        boolean isArmorStand = entityId.equals("minecraft:armor_stand");
        boolean isScarecrow = entityId.equals("tuanzis_mod:scarecrow");
        if (isArmorStand || isScarecrow) {
            // MC 26.3 equipment 组件格式
            if (nbt.contains("equipment")) {
                CompoundTag eqTag = nbt.getCompoundOrEmpty("equipment");
                for (String slotName : eqTag.keySet()) {
                    // 稻草人头部天生自带南瓜/南瓜灯，免扣除材料
                    if (isScarecrow && "head".equalsIgnoreCase(slotName)) continue;
                    CompoundTag itemTag = eqTag.getCompoundOrEmpty(slotName);
                    ItemStack stack = ItemStackNbtHelper.read(registries, itemTag);
                    if (!stack.isEmpty()) {
                        list.add(new EntityRequirement("EQ_" + slotName.toLowerCase(Locale.ROOT), stack));
                    }
                }
            } else {
                // 兼容旧版 ArmorItems 与 HandItems
                if (nbt.contains("ArmorItems")) {
                    ListTag armorList = nbt.getListOrEmpty("ArmorItems");
                    String[] slots = {"feet", "legs", "chest", "head"};
                    for (int i = 0; i < Math.min(armorList.size(), slots.length); i++) {
                        if (isScarecrow && i == 3) continue; // 稻草人头部南瓜免扣
                        CompoundTag itemTag = armorList.getCompoundOrEmpty(i);
                        ItemStack stack = ItemStackNbtHelper.read(registries, itemTag);
                        if (!stack.isEmpty()) {
                            list.add(new EntityRequirement("EQ_" + slots[i], stack));
                        }
                    }
                }
                if (nbt.contains("HandItems")) {
                    ListTag handList = nbt.getListOrEmpty("HandItems");
                    String[] slots = {"mainhand", "offhand"};
                    for (int i = 0; i < Math.min(handList.size(), slots.length); i++) {
                        CompoundTag itemTag = handList.getCompoundOrEmpty(i);
                        ItemStack stack = ItemStackNbtHelper.read(registries, itemTag);
                        if (!stack.isEmpty()) {
                            list.add(new EntityRequirement("EQ_" + slots[i], stack));
                        }
                    }
                }
            }
        }

        return list;
    }

    /**
     * 原子化检查并扣除所需材料：
     * 只有当所有材料在 cachedSources 中全部满足且不互相冲突时，才一次性全部扣除并持久化写回容器。
     *
     * @param cachedSources       容器物品源
     * @param requirements        需求清单
     * @param exactMatch          是否开启精准组件匹配
     * @param consumedItemsOutput 输出实际扣除的真实物品栈（供实体生成装填）
     * @return true 若全部满足且扣除成功；false 若任一材料不足（不发生任何扣除）
     */
    public static boolean tryReserveAndConsume(
            List<ContainerSource> cachedSources,
            List<EntityRequirement> requirements,
            boolean exactMatch,
            Map<String, ItemStack> consumedItemsOutput
    ) {
        if (cachedSources == null || requirements.isEmpty()) return false;

        // 跟踪每个槽位预占数量，防止同槽位被多个需求重复扣超
        Map<ContainerSource, Integer> reservedCount = new HashMap<>();
        Map<String, ContainerSource> matchResult = new LinkedHashMap<>();
        Map<String, ItemStack> matchedStacks = new LinkedHashMap<>();

        for (EntityRequirement req : requirements) {
            ItemStack template = req.template();
            ContainerSource matchedSource = null;
            ItemStack matchedStack = null;

            for (ContainerSource src : cachedSources) {
                ItemStack slotStack = src.container.getItem(src.slot);
                if (slotStack.isEmpty()) continue;

                int alreadyReserved = reservedCount.getOrDefault(src, 0);
                if (slotStack.getCount() <= alreadyReserved) continue;

                boolean match;
                if (exactMatch) {
                    match = ItemStack.isSameItemSameComponents(slotStack, template);
                } else {
                    match = slotStack.is(template.getItem());
                }

                if (match) {
                    matchedSource = src;
                    matchedStack = slotStack.copyWithCount(1);
                    break;
                }
            }

            if (matchedSource == null) {
                // 任一材料缺货，原子性中止
                ModLog.debug("BlueprintEntityHelper: 材料缺失中止扣料, 槽位=" + req.slotKey() + ", 需求=" + template);
                return false;
            }

            reservedCount.merge(matchedSource, 1, Integer::sum);
            matchResult.put(req.slotKey(), matchedSource);
            matchedStacks.put(req.slotKey(), matchedStack);
        }

        // 全部齐套，执行真实扣减
        for (Map.Entry<ContainerSource, Integer> entry : reservedCount.entrySet()) {
            ContainerSource src = entry.getKey();
            int countToShrink = entry.getValue();
            ItemStack slotStack = src.container.getItem(src.slot);
            slotStack.shrink(countToShrink);
            src.markChanged();
        }

        consumedItemsOutput.putAll(matchedStacks);
        ModLog.debug("BlueprintEntityHelper: 原子化扣料成功, 共扣除 " + requirements.size() + " 件物品");
        return true;
    }

    /**
     * 对实体进行亚像素空间几何变换（旋转、镜像、朝向、挂饰附着锚点计算）
     */
    public static TransformedEntity transformEntity(
            CompoundTag rawNbt,
            int width,
            int height,
            int length,
            BlockPos basePos,
            BlockPos offset,
            int rotationIdx,
            boolean mirrored,
            @Nullable HolderLookup.Provider registries
    ) {
        CompoundTag nbt = rawNbt.copy();
        String entityId = nbt.getStringOr("id", "");

        // 1. 读取相对坐标
        ListTag posList = nbt.getListOrEmpty("Pos");
        double ex = 0.0, ey = 0.0, ez = 0.0;
        if (posList.size() >= 3) {
            ex = posList.getDoubleOr(0, 0.0);
            ey = posList.getDoubleOr(1, 0.0);
            ez = posList.getDoubleOr(2, 0.0);
        }

        Rotation rot = Rotation.values()[(rotationIdx % 4 + 4) % 4];

        // 2. 亚像素连续旋转
        double rx = ex;
        double rz = ez;
        double wRot = width;
        if (rot == Rotation.CLOCKWISE_90) {
            rx = length - ez;
            rz = ex;
            wRot = length;
        } else if (rot == Rotation.CLOCKWISE_180) {
            rx = width - ex;
            rz = length - ez;
            wRot = width;
        } else if (rot == Rotation.COUNTERCLOCKWISE_90) {
            rx = ez;
            rz = width - ex;
            wRot = length;
        }

        // 3. 镜像翻转（X 轴对称翻转）
        if (mirrored) {
            rx = wRot - rx;
        }

        double finalX = basePos.getX() + offset.getX() + rx;
        double finalY = basePos.getY() + offset.getY() + ey;
        double finalZ = basePos.getZ() + offset.getZ() + rz;
        Vec3 exactPos = new Vec3(finalX, finalY, finalZ);

        // 4. 旋转与镜像实体的偏航角 yRot
        float yRot = 0.0f;
        float xRot = 0.0f;
        if (nbt.contains("Rotation")) {
            ListTag rotList = nbt.getListOrEmpty("Rotation");
            if (!rotList.isEmpty()) {
                yRot = rotList.getFloatOr(0, 0.0f);
                if (rotList.size() > 1) {
                    xRot = rotList.getFloatOr(1, 0.0f);
                }
            }
        }

        // 偏航角旋转
        float rotDegrees = switch (rot) {
            case CLOCKWISE_90 -> 90.0f;
            case CLOCKWISE_180 -> 180.0f;
            case COUNTERCLOCKWISE_90 -> 270.0f;
            default -> 0.0f;
        };
        yRot = (yRot + rotDegrees) % 360.0f;
        if (mirrored) {
            yRot = (-yRot + 360.0f) % 360.0f;
        }

        ListTag newRotList = new ListTag();
        newRotList.add(net.minecraft.nbt.FloatTag.valueOf(yRot));
        newRotList.add(net.minecraft.nbt.FloatTag.valueOf(xRot));
        nbt.put("Rotation", newRotList);

        // 5. 挂饰朝向与附着锚点计算
        Direction facing = Direction.SOUTH;
        if (nbt.contains("Facing")) {
            byte facingByte = nbt.getByteOr("Facing", (byte) 3);
            facing = Direction.from3DDataValue(facingByte);
        } else if (nbt.contains("facing")) {
            byte facingByte = nbt.getByteOr("facing", (byte) 3);
            facing = Direction.from2DDataValue(facingByte);
        }

        Direction transformedFacing = rot.rotate(facing);
        if (mirrored) {
            transformedFacing = Mirror.FRONT_BACK.mirror(transformedFacing);
        }

        nbt.putByte("Facing", (byte) transformedFacing.get3DDataValue());
        if (transformedFacing.getAxis().isHorizontal()) {
            nbt.putByte("facing", (byte) transformedFacing.get2DDataValue());
        }

        // 处理天花板与地面展示框的 ItemRotation
        if (entityId.contains("item_frame") && transformedFacing.getAxis().isVertical() && nbt.contains("ItemRotation")) {
            int itemRot = nbt.getByteOr("ItemRotation", (byte) 0);
            int rotStep = switch (rot) {
                case CLOCKWISE_90 -> 2;
                case CLOCKWISE_180 -> 4;
                case COUNTERCLOCKWISE_90 -> 6;
                default -> 0;
            };
            itemRot = (itemRot + rotStep) % 8;
            if (mirrored) {
                itemRot = (8 - itemRot) % 8;
            }
            nbt.putByte("ItemRotation", (byte) itemRot);
        }

        // 6. 附着锚点 block_pos 精确反推
        BlockPos targetBlockPos;
        if (entityId.equals("minecraft:painting")) {
            // 反推画锚点格
            int pWidth = 1, pHeight = 1;
            if (registries != null && nbt.contains("variant")) {
                String variantId = nbt.getStringOr("variant", "");
                Identifier id = Identifier.tryParse(variantId);
                if (id != null) {
                    var lookup = registries.lookup(Registries.PAINTING_VARIANT);
                    if (lookup.isPresent()) {
                        var opt = lookup.get().get(ResourceKey.create(Registries.PAINTING_VARIANT, id));
                        if (opt.isPresent()) {
                            PaintingVariant pv = opt.get().value();
                            pWidth = pv.width();
                            pHeight = pv.height();
                        }
                    }
                }
            }
            double horizontalOffset = (pWidth % 2 == 0) ? 0.5 : 0.0;
            double verticalOffset = (pHeight % 2 == 0) ? 0.5 : 0.0;
            Direction left = transformedFacing.getCounterClockWise();

            Vec3 attachedToWall = exactPos.relative(Direction.UP, -verticalOffset).relative(left, -horizontalOffset);
            Vec3 centerBlockVec = attachedToWall.relative(transformedFacing, 0.46875);
            targetBlockPos = BlockPos.containing(centerBlockVec);
        } else if (entityId.contains("item_frame")) {
            Vec3 centerBlockVec = exactPos.relative(transformedFacing, 0.46875);
            targetBlockPos = BlockPos.containing(centerBlockVec);
        } else {
            targetBlockPos = BlockPos.containing(exactPos);
        }

        // 写回 block_pos 与 Pos
        nbt.putIntArray("block_pos", new int[]{targetBlockPos.getX(), targetBlockPos.getY(), targetBlockPos.getZ()});
        ListTag finalPosList = new ListTag();
        finalPosList.add(DoubleTag.valueOf(exactPos.x));
        finalPosList.add(DoubleTag.valueOf(exactPos.y));
        finalPosList.add(DoubleTag.valueOf(exactPos.z));
        nbt.put("Pos", finalPosList);

        // 移除唯一 UUID
        nbt.remove("UUID");

        return new TransformedEntity(exactPos, targetBlockPos, transformedFacing, nbt);
    }

    /**
     * 生存模式安全 NBT 构建：
     * 将实际扣除的物品栈注入展示框和盔甲架，清空车船内部物资，设置试炼假人耐久，剥离敏感标签。
     */
    public static CompoundTag buildSanitizedEntityNbt(
            CompoundTag originalNbt,
            String entityId,
            Map<String, ItemStack> consumedItems,
            boolean isCreativeCore,
            HolderLookup.Provider registries
    ) {
        CompoundTag sanitized = originalNbt.copy();
        // 任何模式下都必须剥离原有 UUID，防止实体 UUID 冲突
        sanitized.remove("UUID");

        if (isCreativeCore) {
            // 创造核心模式下完全信任蓝图原版数据，保留容器内物资
            return sanitized;
        }

        // 1. 注入展示框内真实物品
        if (entityId.contains("item_frame")) {
            ItemStack frameItem = consumedItems.getOrDefault("FRAME_ITEM", ItemStack.EMPTY);
            if (!frameItem.isEmpty()) {
                sanitized.put("Item", (CompoundTag) ItemStack.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), frameItem).getOrThrow());
            } else {
                sanitized.remove("Item");
            }
        }

        // 2. 注入盔甲架/稻草人装备
        if (entityId.equals("minecraft:armor_stand") || entityId.equals("tuanzis_mod:scarecrow")) {
            CompoundTag eqTag = new CompoundTag();
            String[] slots = {"feet", "legs", "chest", "head", "mainhand", "offhand"};
            for (String slot : slots) {
                ItemStack stack = consumedItems.getOrDefault("EQ_" + slot, ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    eqTag.put(slot, (CompoundTag) ItemStack.CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack).getOrThrow());
                }
            }
            if (!eqTag.isEmpty()) {
                sanitized.put("equipment", eqTag);
            } else {
                sanitized.remove("equipment");
            }
            // 移除旧格式
            sanitized.remove("ArmorItems");
            sanitized.remove("HandItems");
        }

        // 3. 试炼假人耐久同步
        if (entityId.equals("tuanzis_mod:trial_dummy")) {
            ItemStack baseStack = consumedItems.getOrDefault("BASE", ItemStack.EMPTY);
            int dur = 32;
            if (!baseStack.isEmpty()) {
                int damage = baseStack.getOrDefault(net.minecraft.core.component.DataComponents.DAMAGE, baseStack.getDamageValue());
                dur = Math.clamp(32 - damage, 1, 32);
            }
            sanitized.putInt("TrialDummyDurability", dur);
        }

        // 4. 清空运输车与运输船容器内部物资
        if (entityId.contains("chest_boat") || entityId.equals("minecraft:bamboo_chest_raft")
                || entityId.equals("minecraft:chest_minecart") || entityId.equals("minecraft:hopper_minecart")) {
            sanitized.remove("Items");
            sanitized.remove("LootTable");
            sanitized.remove("LootTableSeed");
        }

        // 5. 剥离无敌与乘客作弊属性
        sanitized.remove("Invulnerable");
        sanitized.remove("Passengers");
        sanitized.remove("Fixed");
        sanitized.remove("Marker");

        return sanitized;
    }

    /**
     * 实体所需材料项记录（包含目标槽位/类型与模板 ItemStack）
     */
    public record EntityRequirement(String slotKey, ItemStack template) {
    }

    /**
     * 实体空间变换结果对象
     */
    public record TransformedEntity(
            Vec3 exactPos,
            BlockPos targetBlockPos,
            Direction facing,
            CompoundTag transformedNbt
    ) {
    }
}
