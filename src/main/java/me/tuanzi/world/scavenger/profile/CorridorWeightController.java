package me.tuanzi.world.scavenger.profile;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridInventory;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 拾荒回廊 8 级动态全身负重评估与移速/奔跑惩罚控制器。
 * <p>
 * 规则：
 * <ul>
 *   <li>仅当玩家处于 {@link ModDimensions#SCAVENGER_CORRIDOR_LEVEL} 且非创造/旁观模式时激活；</li>
 *   <li>全身负重汇总：快捷栏(0..8) + 盔甲四件套 + 副手 + 战术背包本体及内部网格 + 安全箱本体及内部网格（3枚归途信标恒为 0kg）；</li>
 *   <li>Lv.1 默认 30kg 超重、40kg 超负荷，每升 1 级各 +5kg，最高 Lv.8（65kg/75kg）；</li>
 *   <li>{@code (W1, W2]} 线性减速 {@code 0% -> -20%}，允许奔跑；</li>
 *   <li>{@code > W2} 禁止奔跑，移速从 {@code -25%} 起每超 1kg 额外 {@code -2%}，封顶 {@code -65%}。</li>
 * </ul>
 */
public final class CorridorWeightController {

    public static final Identifier SPEED_PENALTY_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "corridor_weight_penalty");

    /** 零数值标记修饰器：通过原版属性包自动向客户端同步“超负荷禁止疾跑”状态，实现双端零抖动禁跑 */
    public static final Identifier OVERLOAD_NO_SPRINT_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "corridor_overload_no_sprint");

    private static final AttributeModifier OVERLOAD_MARKER_MODIFIER = new AttributeModifier(
            OVERLOAD_NO_SPRINT_MODIFIER_ID,
            0.0,
            AttributeModifier.Operation.ADD_VALUE
    );

    private static final EquipmentSlot[] WEIGHTED_EQUIPMENT_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND
    };

    public enum WeightState {
        NORMAL,
        OVERWEIGHT,
        OVERLOADED
    }

    public record WeightEvaluation(
            float totalWeightKg,
            float overweightThresholdKg,
            float overloadedThresholdKg,
            float speedMultiplier,
            boolean sprintAllowed,
            WeightState state
    ) {
    }

    private CorridorWeightController() {
    }

    /**
     * 纯函数负重区间与移速乘数评估（供服务端 Tick、客户端 GUI 负重条及单元测试共用）。
     */
    public static WeightEvaluation evaluate(
            float totalWeightKg,
            int weightLevel,
            ScavengerProgressionConfig config
    ) {
        ScavengerProgressionConfig cfg = config != null ? config : ScavengerDataRegistry.getProgression();
        int lv = cfg.clampWeightLevel(weightLevel);
        float safeTotal = Math.max(0.0f, totalWeightKg);
        float w1 = cfg.getOverweightThresholdKg(lv);
        float w2 = cfg.getOverloadedThresholdKg(lv);
        float speedMul = cfg.computeMovementSpeedMultiplier(safeTotal, lv);
        boolean canSprint = cfg.isSprintAllowed(safeTotal, lv);

        WeightState state;
        if (safeTotal <= w1) {
            state = WeightState.NORMAL;
        } else if (safeTotal <= w2) {
            state = WeightState.OVERWEIGHT;
        } else {
            state = WeightState.OVERLOADED;
        }
        return new WeightEvaluation(safeTotal, w1, w2, speedMul, canSprint, state);
    }

    /**
     * 计算指定物品栈列表与回廊档案组合的全身总重量（kg）。
     */
    public static float computeTotalBodyWeightKg(
            Iterable<ItemStack> hotbarAndEquipmentStacks,
            PlayerCorridorProfile profile
    ) {
        double sum = 0.0;
        if (hotbarAndEquipmentStacks != null) {
            for (ItemStack stack : hotbarAndEquipmentStacks) {
                if (stack != null && !stack.isEmpty()) {
                    sum += (double) stack.getCount() * ScavengerDataRegistry.resolveItemMeta(stack).weightKg();
                }
            }
        }
        if (profile != null) {
            if (!profile.equippedBackpack().isEmpty()) {
                sum += ScavengerDataRegistry.resolveItemMeta(profile.equippedBackpack()).weightKg();
                GridInventory bpGrid = profile.backpackGrid();
                if (bpGrid != null) {
                    sum += bpGrid.calculateTotalWeightKg();
                }
            }
            if (!profile.equippedSafebox().isEmpty()) {
                sum += ScavengerDataRegistry.resolveItemMeta(profile.equippedSafebox()).weightKg();
                GridInventory sbGrid = profile.safeboxGrid();
                if (sbGrid != null) {
                    sum += sbGrid.calculateTotalWeightKg();
                }
            }
        }
        return (float) sum;
    }

    /**
     * 汇总真实玩家实体当前的回廊全身负重（快捷栏 0..8 + 护甲四件 + 副手 + 战术背包及内容物 + 安全箱及内容物；3枚归途信标为 0kg）。
     */
    public static float computePlayerTotalWeightKg(Player player, PlayerCorridorProfile profile) {
        double sum = 0.0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
            ItemStack hotbarStack = inv.getItem(i);
            if (!hotbarStack.isEmpty()) {
                sum += (double) hotbarStack.getCount() * ScavengerDataRegistry.resolveItemMeta(hotbarStack).weightKg();
            }
        }
        for (EquipmentSlot slot : WEIGHTED_EQUIPMENT_SLOTS) {
            ItemStack equipStack = player.getItemBySlot(slot);
            if (!equipStack.isEmpty()) {
                sum += (double) equipStack.getCount() * ScavengerDataRegistry.resolveItemMeta(equipStack).weightKg();
            }
        }
        sum += computeTotalBodyWeightKg(null, profile);
        return (float) sum;
    }

    /**
     * 服务端周期性刷新玩家负重惩罚与禁疾跑状态。
     */
    public static void tickServerPlayer(ServerPlayer player) {
        if (!(player instanceof ScavengerPlayerTracker tracker)) {
            return;
        }
        if (player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL
                || player.isCreative()
                || player.isSpectator()) {
            if (tracker.tuanzis_mod$isCorridorOverloaded()) {
                tracker.tuanzis_mod$setCorridorOverloaded(false);
            }
            clearWeightModifiers(player);
            return;
        }

        PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
        boolean forceRecalc = profile.consumeDirty();
        if (!forceRecalc && (player.tickCount % 5 != 0)) {
            if (tracker.tuanzis_mod$isCorridorOverloaded() && player.isSprinting()) {
                player.setSprinting(false);
            }
            return;
        }

        float totalKg = computePlayerTotalWeightKg(player, profile);
        WeightEvaluation eval = evaluate(totalKg, profile.weightLevel(), ScavengerDataRegistry.getProgression());

        boolean overloaded = !eval.sprintAllowed();
        boolean prevOverloaded = tracker.tuanzis_mod$isCorridorOverloaded();
        tracker.tuanzis_mod$setCorridorOverloaded(overloaded);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        // 1. 同步超负荷禁疾跑标记修饰器并打断已有疾跑
        if (overloaded) {
            if (!speedAttr.hasModifier(OVERLOAD_NO_SPRINT_MODIFIER_ID)) {
                speedAttr.addOrUpdateTransientModifier(OVERLOAD_MARKER_MODIFIER);
            }
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
        } else if (speedAttr.hasModifier(OVERLOAD_NO_SPRINT_MODIFIER_ID)) {
            speedAttr.removeModifier(OVERLOAD_NO_SPRINT_MODIFIER_ID);
        }

        // 2. 更新移速乘法惩罚修饰器（仅当数值变化超过阈值时更新，避免无意义发包与刷屏）
        AttributeModifier existingPenalty = speedAttr.getModifier(SPEED_PENALTY_MODIFIER_ID);
        if (Math.abs(eval.speedMultiplier()) < 1e-4f) {
            if (existingPenalty != null) {
                speedAttr.removeModifier(SPEED_PENALTY_MODIFIER_ID);
                ModLog.debug(player, null, String.format(
                        "【拾荒负重】全身负重=%.2fkg <= %.1fkg(Lv.%d)，已恢复 100%% 正常移速。",
                        eval.totalWeightKg(), eval.overweightThresholdKg(), profile.weightLevel()
                ));
            }
        } else {
            double prevAmount = existingPenalty != null ? existingPenalty.amount() : 0.0;
            if (existingPenalty == null || Math.abs(prevAmount - eval.speedMultiplier()) > 0.001) {
                speedAttr.addOrUpdateTransientModifier(new AttributeModifier(
                        SPEED_PENALTY_MODIFIER_ID,
                        eval.speedMultiplier(),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
                ModLog.debug(player, null, String.format(
                        "【拾荒负重】状态=%s | 负重=%.2fkg (Lv.%d: 超重%.1fkg / 超负荷%.1fkg) -> 移速修正=%.1f%%, 允许疾跑=%b",
                        eval.state().name(),
                        eval.totalWeightKg(),
                        profile.weightLevel(),
                        eval.overweightThresholdKg(),
                        eval.overloadedThresholdKg(),
                        eval.speedMultiplier() * 100.0f,
                        eval.sprintAllowed()
                ));
            } else if (overloaded != prevOverloaded) {
                ModLog.debug(player, null, "【拾荒负重】超负荷疾跑限制切换 -> 禁止疾跑=" + overloaded);
            }
        }
    }

    /**
     * 清理玩家身上的回廊负重移速惩罚与超负荷标记（用于离开回廊或切创造模式时）。
     */
    public static void clearWeightModifiers(LivingEntity entity) {
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            if (speedAttr.hasModifier(SPEED_PENALTY_MODIFIER_ID)) {
                speedAttr.removeModifier(SPEED_PENALTY_MODIFIER_ID);
            }
            if (speedAttr.hasModifier(OVERLOAD_NO_SPRINT_MODIFIER_ID)) {
                speedAttr.removeModifier(OVERLOAD_NO_SPRINT_MODIFIER_ID);
            }
        }
    }

    /**
     * 判断指定实体当前是否因拾荒回廊超负荷而禁止开启疾跑（服务端与客户端通用）。
     */
    public static boolean shouldBlockSprinting(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.level() == null || player.level().dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            return false;
        }
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (player instanceof ScavengerPlayerTracker tracker && tracker.tuanzis_mod$isCorridorOverloaded()) {
            return true;
        }
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return speedAttr != null && speedAttr.hasModifier(OVERLOAD_NO_SPRINT_MODIFIER_ID);
    }
}
