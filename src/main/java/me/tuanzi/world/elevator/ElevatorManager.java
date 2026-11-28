package me.tuanzi.world.elevator;

import me.tuanzi.block.ElevatorBlock;
import me.tuanzi.block.entity.ElevatorBlockEntity;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电梯系统核心控制器：处理玩家跳跃/潜行传送、1秒（20 ticks）冷却、阻挡跳过判定以及蹲下右键方块伪装。
 */
public final class ElevatorManager {
    public static final int TELEPORT_COOLDOWN_TICKS = 20;

    private static final Map<UUID, Long> LAST_TELEPORT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Input> PREV_INPUT = new ConcurrentHashMap<>();
    private static final Map<UUID, StandingState> LAST_STANDING_ELEVATOR = new ConcurrentHashMap<>();

    private record StandingState(BlockPos pos, long tick) {
    }

    private ElevatorManager() {
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register(ElevatorManager::onUseBlock);
    }

    public static void onPlayerDisconnect(UUID uuid) {
        LAST_TELEPORT_TICK.remove(uuid);
        PREV_INPUT.remove(uuid);
        LAST_STANDING_ELEVATOR.remove(uuid);
    }

    public static void clear() {
        LAST_TELEPORT_TICK.clear();
        PREV_INPUT.clear();
        LAST_STANDING_ELEVATOR.clear();
    }

    /**
     * 判断距离上次传送是否仍处于 1 秒（20 ticks）冷却时间内。
     */
    public static boolean isCooldownActive(long lastTeleportTick, long currentTick) {
        return lastTeleportTick >= 0 && (currentTick - lastTeleportTick) < TELEPORT_COOLDOWN_TICKS;
    }

    /**
     * 查找从起点起向指定垂直方向（+1 向上，-1 向下）最近的有效同色电梯。
     * 若某同色电梯上方 2 格被碰撞方块阻挡，则跳过并继续搜寻下一台同色电梯。
     */
    @Nullable
    public static BlockPos findTargetElevator(
            BlockGetter level,
            BlockPos originPos,
            DyeColor requiredColor,
            int stepY,
            int minY,
            int maxY,
            CollisionContext collisionContext
    ) {
        if (stepY != 1 && stepY != -1) {
            return null;
        }
        int y = originPos.getY() + stepY;
        while (y >= minY && y <= maxY) {
            BlockPos candidatePos = new BlockPos(originPos.getX(), y, originPos.getZ());
            BlockState candidateState = level.getBlockState(candidatePos);
            if (candidateState.getBlock() instanceof ElevatorBlock candidateElevator
                    && candidateElevator.getColor() == requiredColor) {
                if (hasTwoBlocksHeadroom(level, candidatePos, candidateState, collisionContext)) {
                    return candidatePos;
                }
                ModLog.debug("[电梯] 候选目标电梯 " + candidatePos + " 上方空间被阻挡，跳过继续搜寻");
            }
            y += stepY;
        }
        return null;
    }

    /**
     * 判断目标电梯上方是否有足够的 2 格无碰撞站立空间（适配完整方块、半砖、楼梯等动态碰撞箱）。
     */
    public static boolean hasTwoBlocksHeadroom(
            BlockGetter level,
            BlockPos elevatorPos,
            BlockState elevatorState,
            CollisionContext collisionContext
    ) {
        double topOffset = getElevatorTopOffset(level, elevatorPos, elevatorState, collisionContext);
        // 若电梯本身为半砖（topOffset <= 0.5），检查上方第 1、2 格（仅要求目标格及上方格无阻挡；若碰撞箱顶部仍在同一格内也兼容）
        BlockPos above1 = elevatorPos.above(1);
        BlockPos above2 = elevatorPos.above(2);
        boolean above1Blocked = !level.getBlockState(above1).getCollisionShape(level, above1, collisionContext).isEmpty();
        boolean above2Blocked = !level.getBlockState(above2).getCollisionShape(level, above2, collisionContext).isEmpty();
        // 当电梯高度 <= 0.15（极薄方块兜底）或常规高度时，至少需要上方两格空间通畅（若站立在半砖且第1格空、第2格顶有方块，玩家身高1.8+0.5=2.3需要第2格也空）
        if (topOffset > 0.2) {
            return !above1Blocked && !above2Blocked;
        }
        return !above1Blocked && !above2Blocked;
    }

    public static double getElevatorTopOffset(
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            CollisionContext context
    ) {
        VoxelShape shape = state.getCollisionShape(level, pos, context);
        if (shape.isEmpty()) {
            return 1.0;
        }
        return shape.max(Direction.Axis.Y);
    }

    /**
     * 服务端每 tick 监听所有在线玩家的跳跃与潜行输入上升沿以触发电梯传送。
     * 使用主世界 getGameTime() 计时，确保 /tick freeze 与步进控制下冷却严格遵循游戏刻流逝。
     */
    public static void tickServer(MinecraftServer server) {
        long currentTick = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player, currentTick);
        }
    }

    private static void tickPlayer(ServerPlayer player, long currentTick) {
        UUID uuid = player.getUUID();
        Input currentInput = player.getLastClientInput();
        Input prevInput = PREV_INPUT.put(uuid, currentInput);
        if (prevInput == null) {
            prevInput = Input.EMPTY;
        }

        if (player.isSpectator() || player.isPassenger() || player.getAbilities().flying) {
            LAST_STANDING_ELEVATOR.remove(uuid);
            return;
        }

        BlockPos standingPos = findElevatorUnderPlayer(player.level(), player, currentTick);
        if (player.onGround() && standingPos != null) {
            LAST_STANDING_ELEVATOR.put(uuid, new StandingState(standingPos, currentTick));
        }

        boolean jumpRising = currentInput.jump() && !prevInput.jump();
        boolean shiftRising = currentInput.shift() && !prevInput.shift();
        if (!jumpRising && !shiftRising) {
            return;
        }

        if (standingPos == null) {
            return;
        }

        BlockState standingState = player.level().getBlockState(standingPos);
        if (!(standingState.getBlock() instanceof ElevatorBlock originElevator)) {
            return;
        }

        long lastTick = LAST_TELEPORT_TICK.getOrDefault(uuid, -1L);
        if (isCooldownActive(lastTick, currentTick)) {
            long remaining = TELEPORT_COOLDOWN_TICKS - (currentTick - lastTick);
            ModLog.debug(player, null, "[电梯] 玩家处于传送冷却中，剩余 " + remaining + " ticks");
            return;
        }

        int direction = jumpRising ? 1 : -1;
        ServerLevel level = player.level();
        // 兼容 26.3 世界高度 API（包含主世界热配置建筑高度下界与上界）
        int minY = level.getMinY();
        int maxY = level.getMaxY();

        BlockPos targetPos = findTargetElevator(
                level,
                standingPos,
                originElevator.getColor(),
                direction,
                minY,
                maxY,
                CollisionContext.of(player)
        );
        if (targetPos == null) {
            ModLog.debug(player, null, "[电梯] 未找到可用的同色(" + originElevator.getColor().getName() + ")"
                    + (direction > 0 ? "上方" : "下方") + "目标电梯，起点=" + standingPos);
            return;
        }

        BlockState targetState = level.getBlockState(targetPos);
        double topOffset = getElevatorTopOffset(level, targetPos, targetState, CollisionContext.of(player));
        double destX = targetPos.getX() + 0.5;
        double destY = targetPos.getY() + topOffset;
        double destZ = targetPos.getZ() + 0.5;

        // 确认目标站立位置不与实体碰撞盒重叠
        AABB playerBoxAtDest = player.getDimensions(player.getPose()).makeBoundingBox(destX, destY, destZ);
        if (!level.noCollision(player, playerBoxAtDest)) {
            ModLog.debug(player, null, "[电梯] 目标位置碰撞体检查未通过，取消传送: " + targetPos);
            return;
        }

        LAST_TELEPORT_TICK.put(uuid, currentTick);
        LAST_STANDING_ELEVATOR.put(uuid, new StandingState(targetPos, currentTick));

        // 起点粒子与音效
        spawnTeleportParticles(level, player.getX(), player.getY() + 0.5, player.getZ());
        level.playSound(null, standingPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6F, direction > 0 ? 1.1F : 0.85F);

        player.teleportTo(level, destX, destY, destZ, Set.of(), player.getYRot(), player.getXRot(), false);
        player.resetFallDistance();
        Vec3 currentVel = player.getDeltaMovement();
        player.setDeltaMovement(currentVel.x, 0.0, currentVel.z);
        player.needsSync = true;

        // 终点粒子与音效
        spawnTeleportParticles(level, destX, destY + 0.5, destZ);
        level.playSound(null, targetPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6F, direction > 0 ? 1.15F : 0.9F);

        ModLog.debug(player, null, "[电梯] 传送成功 (" + (direction > 0 ? "向上" : "向下") + "): "
                + standingPos + " -> " + targetPos + " (颜色=" + originElevator.getColor().getName() + ", tick=" + currentTick + ")");
    }

    @Nullable
    private static BlockPos findElevatorUnderPlayer(Level level, ServerPlayer player, long currentTick) {
        BlockPos onPos = player.getOnPos();
        if (player.onGround()) {
            if (level.getBlockState(onPos).getBlock() instanceof ElevatorBlock) {
                return onPos;
            }
            BlockPos directBelow = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
            if (level.getBlockState(directBelow).getBlock() instanceof ElevatorBlock) {
                return directBelow;
            }
        }

        // 客户端先于服务端执行本地跳跃起跳 (vy=0.42) 时，ServerboundMovePlayerPacket 与 PlayerInputPacket 同 tick 抵达
        StandingState lastStanding = LAST_STANDING_ELEVATOR.get(player.getUUID());
        if (lastStanding != null && (currentTick - lastStanding.tick()) <= 2) {
            BlockPos pos = lastStanding.pos();
            if (level.getBlockState(pos).getBlock() instanceof ElevatorBlock) {
                double dx = Math.abs(player.getX() - (pos.getX() + 0.5));
                double dz = Math.abs(player.getZ() - (pos.getZ() + 0.5));
                double dy = player.getY() - pos.getY();
                if (dx <= 0.85 && dz <= 0.85 && dy >= 0.0 && dy <= 1.75) {
                    return pos;
                }
            }
        }
        return null;
    }

    private static void spawnTeleportParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 12, 0.25, 0.35, 0.25, 0.05);
    }

    /**
     * 拦截蹲下 + 右键电梯的交互，用于为电梯设置/清除伪装。
     * 注意：原版在玩家潜行且手持物品时会跳过 Block#useItemOn，因此必须通过 UseBlockCallback 拦截。
     */
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSpectator() || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ElevatorBlock)) {
            return InteractionResult.PASS;
        }

        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.isEmpty()) {
            if (hand != InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!(level.getBlockEntity(pos) instanceof ElevatorBlockEntity elevatorBe) || !elevatorBe.hasDisguise()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                elevatorBe.clearDisguise();
                level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.5F, 1.3F);
                ModLog.debug(player, null, "[电梯] 空手蹲下右键清除伪装: " + pos);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        if (!(heldStack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        if (blockItem.getBlock() instanceof ElevatorBlock) {
            return InteractionResult.PASS;
        }

        BlockState disguiseState = computeDisguiseState(player, level, pos, hand, heldStack, hitResult);
        if (disguiseState == null) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.tuanzis_mod.elevator.invalid_disguise"));
                ModLog.debug(player, null, "[电梯] 手持方块无法用于伪装: " + heldStack.getItem());
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ElevatorBlockEntity elevatorBe) {
            elevatorBe.setDisguiseState(disguiseState);
            SoundType soundType = disguiseState.getSoundType();
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            ModLog.debug(player, null, "[电梯] 蹲下右键设置伪装成功: pos=" + pos + ", disguise=" + disguiseState);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    /**
     * 按手持方块真实放置时的朝向、半砖、楼梯等状态计算伪装目标 BlockState。
     */
    @Nullable
    public static BlockState computeDisguiseState(
            Player player,
            Level level,
            BlockPos elevatorPos,
            InteractionHand hand,
            ItemStack stack,
            BlockHitResult hitResult
    ) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        if (blockItem.getBlock() instanceof ElevatorBlock) {
            return null;
        }

        BlockHitResult posHit = new BlockHitResult(hitResult.getLocation(), hitResult.getDirection(), elevatorPos, hitResult.isInside());
        BlockPlaceContext placeContext = new DisguisePlaceContext(level, player, hand, stack, posHit, elevatorPos);

        BlockState placementState = blockItem.getBlock().getStateForPlacement(placeContext);
        if (placementState == null) {
            return null;
        }

        BlockItemStateProperties stateProperties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (!stateProperties.isEmpty()) {
            placementState = stateProperties.apply(placementState);
        }

        if (placementState.isAir()
                || placementState.getRenderShape() != RenderShape.MODEL
                || placementState.getBlock() instanceof ElevatorBlock) {
            return null;
        }
        return placementState;
    }

    /**
     * 专用于计算伪装方块的放置上下文：将目标坐标强制锁定在电梯位置，使台阶、楼梯、原木、熔炉等状态与真实放下一致。
     */
    private static final class DisguisePlaceContext extends BlockPlaceContext {
        private final BlockPos targetPos;

        private DisguisePlaceContext(
                Level level,
                Player player,
                InteractionHand hand,
                ItemStack itemStackInHand,
                BlockHitResult hitResult,
                BlockPos targetPos
        ) {
            super(level, player, hand, itemStackInHand, hitResult);
            this.targetPos = targetPos;
            this.replaceClicked = true;
        }

        @Override
        public BlockPos getClickedPos() {
            return this.targetPos;
        }

        @Override
        public boolean canPlace() {
            return true;
        }
    }
}
