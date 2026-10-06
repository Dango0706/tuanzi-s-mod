package me.tuanzi.entity;

import me.tuanzi.network.TrialDummyDamagePacket;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrialDummyEntity extends LivingEntity {
    private int durability = 32;
    @Nullable
    private UUID ownerUuid = null;

    private final Map<UUID, TrialDummyTestSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, TrialDummyTestSession> lastSessions = new ConcurrentHashMap<>();

    // 客户端专属的伤害文字列表
    public final java.util.List<me.tuanzi.entity.DamageText> clientDamageTexts = new java.util.concurrent.CopyOnWriteArrayList<>();

    // 存储当前被攻击的玩家 UUID 的全局 Set，供 Mixin 在相同 Tick 阻断耐久损耗
    public static final java.util.Set<UUID> ATTACKING_PLAYERS = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public TrialDummyEntity(EntityType<? extends LivingEntity> type, net.minecraft.world.level.Level level) {
        super(type, level);
        this.setPermanentlyInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public net.minecraft.world.entity.HumanoidArm getMainArm() {
        return net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public int getDurability() {
        return this.durability;
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public void tick() {
        super.tick();

        // 强行锁死位移
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        if (this.level().isClientSide()) {
            for (me.tuanzi.entity.DamageText text : this.clientDamageTexts) {
                if (text.tick()) {
                    this.clientDamageTexts.remove(text);
                }
            }
        } else {
            long currentTime = System.currentTimeMillis();

            // 每秒（20 ticks）进行超时清理与 Action Bar DPS 更新
            if (this.tickCount % 20 == 0) {
                for (Map.Entry<UUID, TrialDummyTestSession> entry : this.activeSessions.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    TrialDummyTestSession session = entry.getValue();

                    // 超时 4 秒未受击，则重置会话
                    if (currentTime - session.getLastHitTime() > 4000) {
                        session.setFinished(true);
                        this.lastSessions.put(playerUuid, session);
                        this.activeSessions.remove(playerUuid);

                        ServerPlayer player = (ServerPlayer) ((ServerLevel) this.level()).getPlayerByUUID(playerUuid);
                        if (player != null) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.reset"), true);
                        }
                    } else {
                        // 活跃 DPS 统计，并通过翻译键完美实现国际化与动态参数展示
                        ServerPlayer player = (ServerPlayer) ((ServerLevel) this.level()).getPlayerByUUID(playerUuid);
                        if (player != null) {
                            String totalStr = String.format("%.1f", session.getTotalDamage());
                            String dpsStr = String.format("%.1f", session.getDPS(currentTime));
                            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("hud.tuanzis_mod.trial_dummy.actionbar", totalStr, dpsStr), true);
                        }
                    }
                }
                
                // 清空本秒的攻击玩家 Set，防止溢出或失效
                ATTACKING_PLAYERS.clear();
            }
        }
    }

    /**
     * 解析伤害的真实来源实体（优先解析投射物发射者/拥有者）。
     */
    @Nullable
    public static Entity resolveSourceEntity(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity != null && !(entity instanceof Projectile)) {
            return entity;
        }
        if (source.getDirectEntity() instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null) {
                return owner;
            }
        }
        return entity != null ? entity : source.getDirectEntity();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(level, source, amount);
        }

        boolean isProjectile = source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                || source.getDirectEntity() instanceof Projectile;

        // 创造模式玩家直接近战破坏一击即碎（投射物攻击不属于直接近战破坏）
        if (source.isCreativePlayer() && !isProjectile) {
            this.trialDummyDestroyed(level, source, false, false);
            return true;
        }

        // 解析真实伤害来源实体（支持投射物拥有者/发射者）
        Entity trueSource = resolveSourceEntity(source);

        // 若来源是玩家，将其 UUID 放入防武器耐久磨损的临时集合
        if (trueSource instanceof Player player) {
            ATTACKING_PLAYERS.add(player.getUUID());
        }

        // 允许所有投射物攻击以及玩家造成的攻击（近战、远程、魔法等）参与实体化防御与伤害计算，免受所有火烧、掉落等环境伤害
        if (isProjectile || trueSource instanceof Player) {
            return super.hurtServer(level, source, amount);
        }

        return false;
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        // 双保险设计：手动遍历每个防具槽收集护甲值与韧性，确保因原版 AI tick 缺失导致属性未即时刷新时依然 100% 准确生效！
        final float[] armorBox = {0.0F};
        final float[] toughnessBox = {0.0F};

        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                net.minecraft.world.item.ItemStack stack = this.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    stack.forEachModifier(slot, (attribute, modifier) -> {
                        if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) {
                            armorBox[0] += (float) modifier.amount();
                        } else if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)) {
                            toughnessBox[0] += (float) modifier.amount();
                        }
                    });
                }
            }
        }

        float rawArmor = (float) this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        float rawToughness = (float) this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);

        float finalArmor = Math.max(rawArmor, armorBox[0]);
        float finalToughness = Math.max(rawToughness, toughnessBox[0]);

        float finalDamage = amount;
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            finalDamage = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                this, 
                finalDamage, 
                source, 
                finalArmor, 
                finalToughness
            );
        }
        finalDamage = this.getDamageAfterMagicAbsorb(source, finalDamage);

        // 解析真实伤害来源实体
        Entity trueSource = resolveSourceEntity(source);
        long currentTime = System.currentTimeMillis();

        // 统一使用 ModLog.debug 输出受击伤害诊断日志（严格遵守 GEMINI.md 规范）
        me.tuanzi.util.ModLog.debug(trueSource, this, String.format(
            "【试炼假人受击诊断】来源实体: %s, 投射物直接实体: %s, 护甲: %.1f (属性: %.1f, 装备: %.1f), 韧性: %.1f (属性: %.1f, 装备: %.1f), 原始伤害: %.1f, 减免后最终伤害: %.1f",
            trueSource != null ? trueSource.getName().getString() : "未知来源",
            source.getDirectEntity() != null ? source.getDirectEntity().getName().getString() : "无",
            finalArmor, rawArmor, armorBox[0], finalToughness, rawToughness, toughnessBox[0], amount, finalDamage
        ));

        // 投射物箭矢免消耗处理（排除三叉戟等非消耗性投射物）
        if (source.getDirectEntity() instanceof AbstractArrow arrow && !(arrow instanceof ThrownTrident)) {
            boolean isCreativeShooter = (trueSource instanceof Player player && player.hasInfiniteMaterials())
                    || arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
            if (!isCreativeShooter) {
                // 生存模式：原位无损补偿射出的真实箭矢（支持光灵箭、药水箭等特殊箭矢）
                ItemStack pickupItem = arrow.getPickupItemStackOrigin();
                if (pickupItem == null || pickupItem.isEmpty()) {
                    pickupItem = new ItemStack(Items.ARROW);
                } else {
                    pickupItem = pickupItem.copy();
                }
                this.spawnAtLocation(level, pickupItem);
            }
            arrow.discard();
        }

        // 伤害与 DPS 统计
        if (trueSource instanceof ServerPlayer player) {
            UUID playerUuid = player.getUUID();

            // 获取或创建玩家测试会话
            TrialDummyTestSession session = this.activeSessions.computeIfAbsent(playerUuid, 
                    uuid -> new TrialDummyTestSession(uuid, this.getUUID(), currentTime));

            session.recordHit(finalDamage, currentTime);

            // 立刻实时刷新攻击玩家的 Action Bar HUD，提供零延迟打击反馈
            String totalStr = String.format("%.1f", session.getTotalDamage());
            String dpsStr = String.format("%.1f", session.getDPS(currentTime));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("hud.tuanzis_mod.trial_dummy.actionbar", totalStr, dpsStr), true);
        } else if (trueSource != null) {
            // 来源为非玩家实体（如怪物等）：记录测试会话便于调试与扩展
            UUID sourceUuid = trueSource.getUUID();
            TrialDummyTestSession session = this.activeSessions.computeIfAbsent(sourceUuid,
                    uuid -> new TrialDummyTestSession(uuid, this.getUUID(), currentTime));
            session.recordHit(finalDamage, currentTime);
        }

        // 无论来源为何，只要造成有效伤害，均向附近 64 格内的所有玩家广播浮空跳字
        if (finalDamage > 0.0f) {
            TrialDummyDamagePacket packet = new TrialDummyDamagePacket(this.getId(), finalDamage);
            for (ServerPlayer nearby : level.players()) {
                if (nearby.distanceToSqr(this) < 64 * 64) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(nearby, packet);
                }
            }
        }

        // 执行原版受伤（展现红光受击硬直与声音）
        super.actuallyHurt(level, source, amount);

        // 立刻将血量回满，实现完美无敌！
        this.setHealth(this.getMaxHealth());
        this.setInvulnerableTime(0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
        ItemStack handStack = player.getItemInHand(hand);

        // 1. 蹲下空手：安全回收
        if (player.isSecondaryUseActive() && handStack.isEmpty()) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                this.trialDummyDestroyed(serverLevel, player.damageSources().playerAttack(player), true, true);
            }
            return InteractionResult.SUCCESS;
        }

        // 2. 正常空手：查询统计数据
        if (handStack.isEmpty()) {
            if (!this.level().isClientSide()) {
                UUID playerUuid = player.getUUID();
                TrialDummyTestSession session = this.activeSessions.get(playerUuid);
                if (session == null) {
                    session = this.lastSessions.get(playerUuid);
                }

                if (session == null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.no_tests"));
                } else {
                    long currentTime = System.currentTimeMillis();
                    double duration = session.getDurationInSeconds(currentTime);
                    String durationStr = String.format("%.1f", duration);
                    String titleKey = session.isFinished() 
                        ? "message.tuanzis_mod.trial_dummy.stats.title_archive"
                        : "message.tuanzis_mod.trial_dummy.stats.title_active";
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(titleKey));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.stats.total_damage", String.format("%.1f", session.getTotalDamage())));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.stats.max_damage", String.format("%.1f", session.getMaxSingleDamage())));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.stats.hits", session.getAttackCount()));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.stats.dps", String.format("%.1f", session.getDPS(currentTime))));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.stats.duration", durationStr));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.tuanzis_mod.trial_dummy.stats.footer"));
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 3. 手持防具或物品：对调装备栏（头盔、胸甲、护腿、靴子、主手、副手）
        net.minecraft.world.item.equipment.Equippable equippable = handStack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        EquipmentSlot slot = null;
        if (equippable != null) {
            slot = equippable.slot();
        } else {
            // 如若不是标准防具，则进行主副手防具检测（如盾牌），默认主手
            if (handStack.is(Items.SHIELD)) {
                slot = EquipmentSlot.OFFHAND;
            } else {
                slot = EquipmentSlot.MAINHAND;
            }
        }
        
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            if (!this.level().isClientSide()) {
                ItemStack currentEquip = this.getItemBySlot(slot);
                
                this.setItemSlot(slot, handStack.copy());
                player.setItemInHand(hand, currentEquip);
                
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public void trialDummyDestroyed(ServerLevel level, DamageSource source, boolean safelyRecycled, boolean deductDurability) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARMOR_STAND_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);

        if (safelyRecycled) {
            int nextDurability = deductDurability ? this.durability - 1 : this.durability;
            if (nextDurability > 0) {
                ItemStack dropStack = new ItemStack(me.tuanzi.init.ModItems.TRIAL_DUMMY);
                dropStack.setDamageValue(32 - nextDurability);
                this.spawnAtLocation(level, dropStack);
            } else {
                int sticks = this.getRandom().nextInt(3);
                if (sticks > 0) {
                    this.spawnAtLocation(level, new ItemStack(Items.STICK, sticks));
                }
                this.spawnAtLocation(level, new ItemStack(Items.HAY_BLOCK, 1));
            }
        } else {
            this.spawnAtLocation(level, new ItemStack(me.tuanzi.init.ModItems.TRIAL_DUMMY));
        }

        // 无损掉落所有穿戴装备
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(level, stack);
                this.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        level.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState()),
                this.getX(),
                this.getY() + 1.0,
                this.getZ(),
                15,
                0.25,
                0.5,
                0.25,
                0.05
        );

        this.discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void knockback(double power, double xd, double zd, net.minecraft.world.damagesource.DamageSource source, float damage) {
        // 彻底屏蔽击退力，稳如泰山！
    }

    @Override
    public void die(DamageSource damageSource) {
        if (damageSource.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            super.die(damageSource);
            return;
        }
        // 试炼人偶免疫常规致命伤害，强制恢复满血存活
        this.setHealth(this.getMaxHealth());
    }



    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("TrialDummyDurability", this.durability);
        if (this.ownerUuid != null) {
            output.putString("TrialDummyOwner", this.ownerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.durability = input.getIntOr("TrialDummyDurability", 32);
        String ownerStr = input.getStringOr("TrialDummyOwner", "");
        if (!ownerStr.isEmpty()) {
            this.ownerUuid = UUID.fromString(ownerStr);
        }
    }
}
