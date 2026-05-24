package me.tuanzi.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import me.tuanzi.init.ModItems;

public class ScarecrowEntity extends ArmorStand {
    private float hitDamage = 0.0F;
    
    public ScarecrowEntity(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        // 稻草人天生有手臂，且不显示底座
        this.setShowArms(true);
        this.setNoBasePlate(true);
        // 默认佩戴雕刻南瓜
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        
        // 设置真正的向两侧水平平行于地面展开手臂 (侧平举 T-Pose)
        this.setLeftArmPose(new net.minecraft.core.Rotations(0.0F, 0.0F, -90.0F));
        this.setRightArmPose(new net.minecraft.core.Rotations(0.0F, 0.0F, 90.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            // 受击累计伤害值随时间平滑衰减
            if (this.hitDamage > 0.0F) {
                this.hitDamage = Math.max(0.0F, this.hitDamage - 0.1F);
            }

            // 每秒（20 ticks）做一次手臂姿势锁定与时间检查，确保手臂水平且南瓜灯正常转换
            if (this.tickCount % 20 == 0) {
                this.setLeftArmPose(new net.minecraft.core.Rotations(0.0F, 0.0F, -90.0F));
                this.setRightArmPose(new net.minecraft.core.Rotations(0.0F, 0.0F, 90.0F));
                
                ItemStack headStack = this.getItemBySlot(EquipmentSlot.HEAD);
                if (this.level().isDarkOutside()) {
                    if (headStack.isEmpty() || headStack.is(Items.CARVED_PUMPKIN)) {
                        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.JACK_O_LANTERN));
                    }
                    // 夜晚南瓜灯发光：在头部位置放置亮度为 7 的隐形光源方块
                    this.placeHeadLight();
                } else {
                    if (headStack.isEmpty() || headStack.is(Items.JACK_O_LANTERN)) {
                        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
                    }
                    // 白天移除光源
                    this.removeHeadLight();
                }
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 input) {
        // 锁死在原地，不发生位移
    }

    /**
     * 获取稻草人头部对应的世界坐标（用于放置/清除光源方块）。
     * 头部大约位于实体脚底上方 2 格的位置。
     */
    private BlockPos getHeadLightPos() {
        return this.blockPosition().above(2);
    }

    /**
     * 在头部位置放置亮度为 7 的隐形光源方块。
     */
    private void placeHeadLight() {
        BlockPos headPos = this.getHeadLightPos();
        BlockState current = this.level().getBlockState(headPos);
        if (current.isAir()) {
            this.level().setBlock(headPos,
                    Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 7), 2);
        }
    }

    /**
     * 移除头部位置的隐形光源方块（仅移除我们放置的 LIGHT 方块）。
     */
    private void removeHeadLight() {
        BlockPos headPos = this.getHeadLightPos();
        BlockState current = this.level().getBlockState(headPos);
        if (current.is(Blocks.LIGHT)) {
            this.level().removeBlock(headPos, false);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // 实体被移除时（无论何种原因），确保清除我们放置的光源方块，避免残留脏数据
        if (!this.level().isClientSide()) {
            this.removeHeadLight();
        }
        super.remove(reason);
    }

    private void scarecrowDestroyed(ServerLevel level, DamageSource source) {
        // 播放碎裂音效
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.ARMOR_STAND_BREAK, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);

        // 掉落稻草人物品本身
        this.spawnAtLocation(level, ModItems.SCARECROW);

        // 掉落玩家穿戴在它身上的其他护甲，过滤掉我们默认生成的南瓜头部
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot != EquipmentSlot.HEAD) {
                ItemStack stack = this.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(level, stack);
                }
            } else {
                ItemStack stack = this.getItemBySlot(slot);
                if (!stack.isEmpty() && !stack.is(Items.CARVED_PUMPKIN) && !stack.is(Items.JACK_O_LANTERN)) {
                    this.spawnAtLocation(level, stack);
                }
            }
        }

        // 碎裂木板粒子效果，增强打击沉浸感
        level.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState()),
                this.getX(),
                this.getY(0.6666666666666666),
                this.getZ(),
                10,
                this.getBbWidth() / 4.0F,
                this.getBbHeight() / 4.0F,
                this.getBbWidth() / 4.0F,
                0.05
        );
        // 碎裂前主动清除光源方块
        this.removeHeadLight();
        this.discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 vec) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (player.isSecondaryUseActive()) {
                // 蹲下右键：安全碎裂回收，无损回收全部装备
                this.scarecrowDestroyed(serverLevel, null);
                return InteractionResult.SUCCESS;
            }
        }
        // 允许右键互动换装
        return super.interact(player, hand, vec);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(level, source, amount);
        }
        if (this.isRemoved()) {
            return false;
        }

        // 创造模式下一击即碎，方便创造玩家拆建
        if (source.isCreativePlayer()) {
            if (!this.level().isClientSide()) {
                this.scarecrowDestroyed(level, source);
            }
            return true;
        }

        // 检查防破坏保护：如果玩家不能在此地建造，则不能伤害稻草人
        if (source.getEntity() instanceof Player player && !player.getAbilities().mayBuild) {
            return false;
        }

        // 广播受击事件，使客户端显示来回晃动的物理动作
        level.broadcastEntityEvent(this, (byte)32);
        this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DAMAGE, source.getEntity());
        this.lastHit = level.getGameTime();

        // 累加受击伤害值，如果没有具体的伤害量（如手打），默认算 1.0F 伤害
        float damageApplied = amount > 0.0F ? amount : 1.0F;
        this.hitDamage += damageApplied;

        // 如果累计伤害满 10.0F，或者是秒杀伤害类型，则碎裂掉落；否则仅播放受击声音和动画
        if (this.hitDamage >= 10.0F || source.is(net.minecraft.tags.DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS)) {
            if (!this.level().isClientSide()) {
                this.scarecrowDestroyed(level, source);
            }
        } else {
            // 播放被敲击的木头声音，极大增强击打手感
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.WOOD_HIT, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        return true;
    }
}
