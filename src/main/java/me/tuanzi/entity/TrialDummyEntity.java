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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
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
        this.setInvulnerable(false);
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

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurtServer(level, source, amount);
        }

        // 创造模式玩家左键一击即碎
        if (source.isCreativePlayer()) {
            this.trialDummyDestroyed(level, source, false, false);
            return true;
        }

        // 仅允许玩家造成的近战、远程、魔法攻击参与实体化防御计算，免受所有火烧、掉落等环境伤害
        if (source.getEntity() instanceof Player player) {
            // 将玩家 UUID 放入防武器耐久磨损的临时集合
            ATTACKING_PLAYERS.add(player.getUUID());
            
            // 实体化伤害计算流：调用原版受到伤害的逻辑
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

        // 打印诊断，以便在开发控制台清晰直白地查阅
        me.tuanzi.util.ModLog.info(String.format("[TrialDummy] Damage diagnostics - armor: %.1f (attribute: %.1f, box: %.1f), toughness: %.1f (attribute: %.1f, box: %.1f), rawAmount: %.1f, finalDamage: %.1f",
                finalArmor, rawArmor, armorBox[0], finalToughness, rawToughness, toughnessBox[0], amount, finalDamage));

        if (source.getEntity() instanceof ServerPlayer player) {
            long currentTime = System.currentTimeMillis();
            UUID playerUuid = player.getUUID();

            // 如果是弹射物（如箭矢），进行箭矢免消耗归还
            if (source.getDirectEntity() instanceof AbstractArrow arrow) {
                // 命中后原位无损补偿一根普通箭矢，并清除弹射物实体
                this.spawnAtLocation(level, new ItemStack(Items.ARROW));
                arrow.discard();
            }

            // 获取或创建测试会话
            TrialDummyTestSession session = this.activeSessions.computeIfAbsent(playerUuid, 
                    uuid -> new TrialDummyTestSession(uuid, this.getUUID(), currentTime));

            session.recordHit(finalDamage, currentTime);

            // 发送 S2C 跳字同步包
            TrialDummyDamagePacket packet = new TrialDummyDamagePacket(this.getId(), finalDamage);
            for (ServerPlayer nearby : level.players()) {
                if (nearby.distanceToSqr(this) < 64 * 64) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(nearby, packet);
                }
            }

            // 立刻实时刷新攻击玩家的 Action Bar HUD，提供零延迟高能打击反馈
            String totalStr = String.format("%.1f", session.getTotalDamage());
            String dpsStr = String.format("%.1f", session.getDPS(currentTime));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("hud.tuanzis_mod.trial_dummy.actionbar", totalStr, dpsStr), true);
        }

        // 执行原版受伤（展现红光受击硬直与声音）
        super.actuallyHurt(level, source, amount);

        // 立刻将血量回满，实现完美无敌！
        this.setHealth(this.getMaxHealth());
        this.invulnerableTime = 0;
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
    public void knockback(double strength, double x, double z) {
        // 彻底屏蔽击退力，稳如泰山！
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
