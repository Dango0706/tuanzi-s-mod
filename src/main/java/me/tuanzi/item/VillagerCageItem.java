package me.tuanzi.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;

public class VillagerCageItem extends Item {

    public VillagerCageItem(Item.Properties properties) {
        super(properties);
    }

    // ──────────────────────────────────────────────
    // 右键实体：捕捉村民
    // ──────────────────────────────────────────────
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Villager villager)) {
            return InteractionResult.PASS;
        }
        if (isVillagerStored(stack)) {
            return InteractionResult.PASS; // 笼子已有村民，不重复捕捉
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) player.level();

        // 检查铁傀儡保护（方案B：检测铁傀儡当前攻击目标是否为该村民）
        List<IronGolem> nearbyGolems = serverLevel.getEntitiesOfClass(
                IronGolem.class,
                villager.getBoundingBox().inflate(16),
                (IronGolem golem) -> {
                    LivingEntity golemTarget = golem.getTarget();
                    // 铁傀儡正在攻击捕捉者，说明正处于保护状态；
                    // 同时检测该傀儡在村民附近8格内，确认其属于同一村庄
                    return golemTarget != null && golemTarget == player
                            && golem.distanceTo(villager) <= 32;
                }
        );

        // 若未有傀儡正在追捕玩家，则检查是否有傀儡把村民作为"需要守护对象"
        // （方案B精准：只要16格内有任意傀儡的目标是捕捉者，则触发仇恨）
        List<IronGolem> protectingGolems = serverLevel.getEntitiesOfClass(
                IronGolem.class,
                villager.getBoundingBox().inflate(16),
                (IronGolem golem) -> golem.distanceTo(villager) <= 16
        );

        boolean hasProtector = !protectingGolems.isEmpty() && protectingGolems.stream().anyMatch(
                (IronGolem golem) -> {
                    // 铁傀儡正在巡逻守护（无攻击目标 = 处于巡逻/守护状态）且距离村民≤16
                    // 或铁傀儡的当前目标是某个威胁（说明正在战斗保护）
                    LivingEntity t = golem.getTarget();
                    return t == null || !(t instanceof Player); // 还没有仇恨玩家，说明刚在保护状态
                }
        );

        if (hasProtector) {
            // 让16格内所有铁傀儡对玩家产生仇恨
            for (IronGolem golem : protectingGolems) {
                golem.setTarget(player);
                // 使用 Brain 的 AngryAt 记忆让傀儡持续追击
                try {
                    golem.getBrain().setMemory(MemoryModuleType.ANGRY_AT, player.getUUID());
                } catch (Exception ignored) {}
            }
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.villager_cage.golem_aggro"));
            me.tuanzi.util.ModLog.debug(player, null, "缚灵笼捕捉村民触发守护仇恨：附近 " + protectingGolems.size() + " 只守护中的铁傀儡对捕捉者产生仇恨。");
        }

        // 使用 26.1 的 TagValueOutput 来序列化村民
        net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
        net.minecraft.world.level.storage.TagValueOutput tagValueOutput = 
                net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, serverLevel.registryAccess());
        villager.save(tagValueOutput);
        CompoundTag villagerTag = tagValueOutput.buildResult();

        // 清除床和工作站绑定（让其在新位置重新寻找）
        villagerTag.remove("Brain"); // Brain 中含床/工作站位置记忆
        villagerTag.remove("LastRestock");
        villagerTag.remove("LastGossipDecay");

        // 存储到物品 CustomData
        CompoundTag cageData = new CompoundTag();
        cageData.put("StoredVillager", villagerTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cageData));

        // 播放灵魂沙音效
        serverLevel.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                SoundEvents.SOUL_SAND_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);

        // 产生末影粒子
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                villager.getX(), villager.getY() + 1.0, villager.getZ(),
                20, 0.3, 0.5, 0.3, 0.1);

        String capturedName = villager.getDisplayName().getString();
        String capturedProf = villager.getVillagerData().profession().toString();
        int capturedLevel = villager.getVillagerData().level();
        me.tuanzi.util.ModLog.debug(player, villager, "缚灵笼成功捕捉村民：" + capturedName + "，职业: " + capturedProf + "，等级: " + capturedLevel);

        // 从世界中移除村民
        villager.discard();

        // 标记 CustomModelData=1 以显示填充贴图
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(1.0f), List.of(), List.of(), List.of()));

        return InteractionResult.SUCCESS;
    }

    // ──────────────────────────────────────────────
    // 右键方块：潜行时释放村民
    // ──────────────────────────────────────────────
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!isVillagerStored(context.getItemInHand())) {
            return InteractionResult.PASS; // 空笼不触发
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        // 释放位置 = 被点击方块的上表面
        BlockPos spawnPos = clickedPos.above();
        BlockState groundState = serverLevel.getBlockState(clickedPos);

        // 检查是否有固体方块支撑
        if (!groundState.isSolid()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.villager_cage.need_solid"));
            return InteractionResult.FAIL;
        }

        // 检查生成位置是否有足够空间（村民高约1.95格）
        BlockState spawnState = serverLevel.getBlockState(spawnPos);
        BlockState spawnAbove = serverLevel.getBlockState(spawnPos.above());
        if (!spawnState.isAir() || !spawnAbove.isAir()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.villager_cage.need_solid"));
            return InteractionResult.FAIL;
        }

        // 读取 NBT 并恢复村民
        ItemStack stack = context.getItemInHand();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return InteractionResult.FAIL;

        CompoundTag cageData = customData.copyTag();
        CompoundTag villagerTag = cageData.getCompoundOrEmpty("StoredVillager");

        // 修改生成坐标为目标位置
        villagerTag.putDouble("Pos[0]", spawnPos.getX() + 0.5);
        villagerTag.putDouble("Pos[1]", spawnPos.getY());
        villagerTag.putDouble("Pos[2]", spawnPos.getZ() + 0.5);

        Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(serverLevel,
                net.minecraft.world.entity.EntitySpawnReason.LOAD);
        if (villager == null) return InteractionResult.FAIL;

        // 使用 26.1 的 TagValueInput 加载 NBT
        net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
        net.minecraft.world.level.storage.ValueInput tagValueInput = 
                net.minecraft.world.level.storage.TagValueInput.create(reporter, serverLevel.registryAccess(), villagerTag);
        villager.load(tagValueInput);

        villager.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                villager.getYRot(), villager.getXRot());

        serverLevel.addFreshEntity(villager);

        String releasedName = villager.getDisplayName().getString();
        String releasedProf = villager.getVillagerData().profession().toString();
        int releasedLevel = villager.getVillagerData().level();
        me.tuanzi.util.ModLog.debug(player, villager, "缚灵笼成功释放村民！坐标: " + spawnPos + "，职业: " + releasedProf + "，等级: " + releasedLevel);

        // 播放释放音效（玻璃破碎）
        serverLevel.playSound(null, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 1.2f);

        // 清除村民存储数据与自定义模型，使物品变回空笼（针对创造模式不消耗物品的场景）
        stack.remove(DataComponents.CUSTOM_DATA);
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);

        // 销毁笼子
        stack.shrink(1);

        return InteractionResult.SUCCESS;
    }

    // ──────────────────────────────────────────────
    // Tooltip 显示
    // ──────────────────────────────────────────────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        if (!isVillagerStored(stack)) {
            tooltipComponents.accept(Component.translatable("item.tuanzis_mod.villager_cage.tooltip.empty"));
            return;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag cageData = customData.copyTag();
        CompoundTag villagerTag = cageData.getCompoundOrEmpty("StoredVillager");

        // 读取村民名称（CustomName 或默认职业名）
        String displayName;
        if (villagerTag.contains("CustomName")) {
            try {
                String jsonString = villagerTag.getStringOr("CustomName", "");
                com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> registryOps = 
                        context.registries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
                Component nameComp = ComponentSerialization.CODEC.parse(
                        registryOps, 
                        com.google.gson.JsonParser.parseString(jsonString)
                ).result().orElse(null);
                displayName = nameComp != null ? nameComp.getString() : "村民";
            } catch (Exception e) {
                displayName = "村民";
            }
        } else {
            displayName = "村民";
        }

        // 读取职业
        String professionId = "generic";
        CompoundTag vData = villagerTag.getCompoundOrEmpty("VillagerData");
        String raw = vData.getStringOr("profession", "generic");
        professionId = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;

        // 读取等级
        int level = vData.getIntOr("level", 1);

        String profKey = "entity.minecraft.villager." + professionId;
        tooltipComponents.accept(Component.translatable("item.tuanzis_mod.villager_cage.tooltip.filled",
                displayName,
                Component.translatable(profKey),
                level));
    }

    // ──────────────────────────────────────────────
    // 工具方法
    // ──────────────────────────────────────────────
    public static boolean isVillagerStored(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        return customData.copyTag().contains("StoredVillager");
    }
}
