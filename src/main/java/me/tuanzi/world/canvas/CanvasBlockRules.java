package me.tuanzi.world.canvas;

import me.tuanzi.block.BlueprintCannonBlock;
import me.tuanzi.block.BlueprintTableBlock;
import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 画布空间可放置方块校验规则：
 * 1. 建筑体素允许静态建筑方块、流体与本模组 ColorBlock 系列；禁用常规容器、红石机械、传送门、基岩及画布/其余蓝图机器方块。
 * 2. 临时施工工具允许潜影盒（含 16 色）、蓝图大炮与蓝图桌在画布空间及外围缓冲内临时放置与使用，但绝不写入画布体素数据。
 */
public final class CanvasBlockRules {
    private static final Set<String> BLOCKED_IDS = Set.of(
            "minecraft:bedrock",
            "minecraft:barrier",
            "minecraft:structure_void",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:spawner",
            "minecraft:trial_spawner",
            "minecraft:vault",
            "minecraft:respawn_anchor",
            "tuanzis_mod:easel",
            "tuanzis_mod:printer_3d",
            "tuanzis_mod:copier",
            "tuanzis_mod:paper_shredder",
            "tuanzis_mod:printed_model",
            "tuanzis_mod:printed_model_part",
            "tuanzis_mod:canvas_base_block",
            "tuanzis_mod:player_simulator",
            "tuanzis_mod:block_breaker",
            "tuanzis_mod:block_placer",
            "tuanzis_mod:soul_merchant_station",
            "tuanzis_mod:blackbox_core_case",
            "tuanzis_mod:base_stash_cabinet",
            "tuanzis_mod:base_upgrade_station"
    );

    private CanvasBlockRules() {
    }

    /**
     * 判断方块状态是否属于画布内允许使用的临时施工辅助工具（潜影盒、蓝图大炮、蓝图桌）。
     */
    public static boolean isCanvasUtilityBlock(@Nullable BlockState state) {
        return state != null && !state.isAir() && isCanvasUtilityBlock(state.getBlock());
    }

    /**
     * 判断方块是否属于画布内允许使用的临时施工辅助工具（潜影盒、蓝图大炮、蓝图桌）。
     */
    public static boolean isCanvasUtilityBlock(@Nullable Block block) {
        return block instanceof ShulkerBoxBlock
                || block instanceof BlueprintCannonBlock
                || block instanceof BlueprintTableBlock;
    }

    /**
     * 判断方块是否允许作为永久画布建筑体素保存与 3D 打印（不包含临时施工工具）。
     */
    public static boolean isAllowedAsCanvasVoxel(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return true;
        }
        Block block = state.getBlock();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null && BLOCKED_IDS.contains(id.toString())) {
            return false;
        }
        if (block instanceof ColorBlock || block instanceof ColorSlab || block instanceof ColorStairs) {
            return true;
        }
        if (block instanceof LiquidBlock) {
            return true;
        }
        // 禁止所有其余方块实体（含潜影盒、蓝图大炮、蓝图桌及其他容器与机器）进入画布体素作品
        if (block instanceof EntityBlock) {
            return false;
        }
        if (block instanceof PistonBaseBlock
                || block instanceof PistonHeadBlock
                || block instanceof MovingPistonBlock
                || block instanceof DiodeBlock
                || block instanceof ObserverBlock
                || block instanceof TntBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof PressurePlateBlock
                || block instanceof BedBlock
                || block instanceof RespawnAnchorBlock
                || block instanceof NetherPortalBlock
                || block instanceof EndPortalBlock
                || block instanceof EndPortalFrameBlock) {
            return false;
        }
        return !state.is(Blocks.SPAWNER) && !state.is(Blocks.BEDROCK);
    }

    /**
     * 判断方块是否允许玩家在画布空间维度内放置（建筑体素或临时施工工具）。
     */
    public static boolean isAllowedInCanvasSpace(@Nullable BlockState state) {
        return isCanvasUtilityBlock(state) || isAllowedAsCanvasVoxel(state);
    }
}
