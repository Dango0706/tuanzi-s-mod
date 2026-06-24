package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.mixin.VillagerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import me.tuanzi.entity.StationVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SoulMerchantStationBlockEntity extends BlockEntity {
    private CompoundTag storedVillagerNbt = null;
    private Villager cachedVillager = null;
    private long lastRestockCheckDay = -1L;
    private int restockTimer = 0;
    private java.util.UUID villagerUuid = null;
    private float villagerYaw = 0.0f;

    public SoulMerchantStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SOUL_MERCHANT_STATION_BLOCK_ENTITY, pos, state);
    }

    public boolean hasVillager() {
        return this.storedVillagerNbt != null && !this.storedVillagerNbt.isEmpty();
    }

    public Villager getOrCreateVillager() {
        if (this.cachedVillager == null && this.storedVillagerNbt != null && !this.storedVillagerNbt.isEmpty()) {
            if (this.level instanceof ServerLevel serverLevel) {
                recreateVillager(serverLevel);
            }
        }
        return this.cachedVillager;
    }

    private void recreateVillager(ServerLevel serverLevel) {
        if (this.villagerUuid != null) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.villagerUuid);
            if (entity instanceof Villager v) {
                this.cachedVillager = v;
                return;
            }
        }

        this.cachedVillager = new StationVillager(net.minecraft.world.entity.EntityTypes.VILLAGER, serverLevel);
        if (this.cachedVillager != null) {
            net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
            net.minecraft.world.level.storage.ValueInput tagValueInput = 
                net.minecraft.world.level.storage.TagValueInput.create(reporter, serverLevel.registryAccess(), this.storedVillagerNbt);
            this.cachedVillager.load(tagValueInput);
            
            // 确保属性
            this.cachedVillager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(0.25);
            this.cachedVillager.setNoAi(true);
            this.cachedVillager.setInvulnerable(true);
            this.cachedVillager.setSilent(true);
            this.cachedVillager.snapTo(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.33, this.worldPosition.getZ() + 0.5, this.villagerYaw, 0.0f);
            this.cachedVillager.setYRot(this.villagerYaw);
            this.cachedVillager.setYHeadRot(this.villagerYaw);
            this.cachedVillager.setYBodyRot(this.villagerYaw);
        }
    }

    public void importVillager(CompoundTag villagerTag) {
        this.storedVillagerNbt = villagerTag.copy();
        this.cachedVillager = null; 
        this.villagerUuid = null; // 触发在 serverTick 中自动 spawn 村民实体
        this.villagerYaw = 0.0f;
        this.setChanged();
    }

    public CompoundTag exportVillager() {
        syncVillagerToNbt();
        CompoundTag result = this.storedVillagerNbt != null ? this.storedVillagerNbt.copy() : new CompoundTag();
        
        // 安全将缩小版世界实体移除
        if (this.villagerUuid != null && this.level instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.villagerUuid);
            if (entity != null) {
                entity.discard();
            }
        }

        this.storedVillagerNbt = null;
        this.cachedVillager = null;
        this.villagerUuid = null;
        this.setChanged();
        return result;
    }

    public void renameVillager(Component name) {
        Villager villager = this.getOrCreateVillager();
        if (villager != null) {
            villager.setCustomName(name);
            syncVillagerToNbt();
            this.setChanged();
        }
    }

    public void syncVillagerToNbt() {
        if (this.cachedVillager != null && this.level instanceof ServerLevel serverLevel) {
            net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
            net.minecraft.world.level.storage.TagValueOutput tagValueOutput = 
                net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, serverLevel.registryAccess());
            this.cachedVillager.save(tagValueOutput);
            this.storedVillagerNbt = tagValueOutput.buildResult();
        }
    }

    public void openTradingScreen(Player player) {
        Villager villager = this.getOrCreateVillager();
        if (villager != null) {
            // 折扣注入
            ((VillagerAccessor) villager).invokeUpdateSpecialPrices(player);
            // 绑定交互玩家并直接打开原生村民交易界面！
            villager.setTradingPlayer(player);
            villager.openTradingScreen(player, villager.getDisplayName(), villager.getVillagerData().level());
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SoulMerchantStationBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        if (be.hasVillager()) {
            Villager villager = null;
            if (be.villagerUuid != null) {
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(be.villagerUuid);
                if (entity instanceof Villager v) {
                    villager = v;
                }
            }

            // 1. 如果世界实体没找到（服务器重启、区块重载、意外被清理），我们重新在世界里 spawn 并绑定它！
            if (villager == null) {
                villager = new me.tuanzi.entity.StationVillager(net.minecraft.world.entity.EntityTypes.VILLAGER, serverLevel);
                if (be.storedVillagerNbt != null && !be.storedVillagerNbt.isEmpty()) {
                    net.minecraft.util.ProblemReporter reporter = net.minecraft.util.ProblemReporter.DISCARDING;
                    net.minecraft.world.level.storage.ValueInput tagValueInput = 
                        net.minecraft.world.level.storage.TagValueInput.create(reporter, serverLevel.registryAccess(), be.storedVillagerNbt);
                    villager.load(tagValueInput);
                }
                
                // 设为 0.25 缩小比例，无 AI，无敌，静音，浮空于平台中央
                villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(0.25);
                villager.setNoAi(true);
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.snapTo(pos.getX() + 0.5, pos.getY() + 0.33, pos.getZ() + 0.5, be.villagerYaw, 0.0f);
                villager.setYRot(be.villagerYaw);
                villager.setYHeadRot(be.villagerYaw);
                villager.setYBodyRot(be.villagerYaw);
                
                serverLevel.addFreshEntity(villager);
                be.villagerUuid = villager.getUUID();
                be.setChanged();
            }

            // 2. 如果找到了世界实体，我们每 tick 都强行将其状态锁定在光笼正中心，并将其朝向固定在 yaw 上！
            if (villager != null) {
                villager.snapTo(pos.getX() + 0.5, pos.getY() + 0.33, pos.getZ() + 0.5, be.villagerYaw, 0.0f);
                villager.setYRot(be.villagerYaw);
                villager.setYHeadRot(be.villagerYaw);
                villager.setYBodyRot(be.villagerYaw);
                villager.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                
                villager.setNoAi(true);
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(0.25);
                
                be.cachedVillager = villager; // 让 getOrCreateVillager 始终关联此活体实体，支持右键直接打开交易
            }

            // 3. 同步原本的交易升级和刷新逻辑
            if (villager != null) {
                // 同步玩家交互完成后的粒子效果和声望
                Player lastTraded = ((VillagerAccessor) villager).getLastTradedPlayer();
                if (lastTraded != null) {
                    serverLevel.onReputationEvent(net.minecraft.world.entity.ai.village.ReputationEventType.TRADE, lastTraded, villager);
                    serverLevel.broadcastEntityEvent(villager, (byte)14); // 绿粒子效果
                    ((VillagerAccessor) villager).setLastTradedPlayer(null);
                    be.syncVillagerToNbt();
                    be.setChanged();
                }

                // 模拟升级倒计时
                VillagerAccessor accessor = (VillagerAccessor) villager;
                if (!villager.isTrading() && accessor.getUpdateMerchantTimer() > 0) {
                    int timer = accessor.getUpdateMerchantTimer() - 1;
                    accessor.setUpdateMerchantTimer(timer);
                    if (timer <= 0) {
                        if (accessor.getIncreaseProfessionLevelOnUpdate()) {
                            accessor.invokeIncreaseMerchantCareer(serverLevel);
                            accessor.setIncreaseProfessionLevelOnUpdate(false);
                            serverLevel.broadcastEntityEvent(villager, (byte)14); // 再次播放升级成功的绿粒子
                            be.syncVillagerToNbt();
                            be.setChanged();
                        }
                    }
                }

                // 方块实体自身 Tick 计时刷新交易锁定，每 8000 ticks (8小时游戏时间) 补货一次
                be.restockTimer++;
                if (be.restockTimer >= 8000) {
                    me.tuanzi.util.ModLog.debug(null, pos.toString(), "灵笼贸易站已持续运行 8000 ticks (8小时游戏时间)，触发补货。");
                    be.restockTimer = 0;
                    be.tryRestock(serverLevel);
                    be.setChanged();
                }
            }
        } else {
            // 如果没有村民数据但是世界上还存留有实体，我们将其 discard 安全丢弃，防止遗落僵尸村民
            if (be.villagerUuid != null) {
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(be.villagerUuid);
                if (entity != null) {
                    entity.discard();
                }
                be.villagerUuid = null;
                be.setChanged();
            }
        }
    }

    public void tryRestock(ServerLevel serverLevel) {
        Villager villager = this.getOrCreateVillager();
        if (villager == null) return;

        if (villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.villager.VillagerProfession.NONE)) {
            return;
        }

        java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.entity.ai.village.poi.PoiType>> jobSitePredicate = 
            villager.getVillagerData().profession().value().heldJobSite();

        BlockPos pos = this.getBlockPos();
        boolean foundFreeJobSite = false;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (serverLevel.getPoiManager().getInRange(jobSitePredicate, neighborPos, 0, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).findAny().isPresent()) {
                foundFreeJobSite = true;
                break;
            }
        }

        if (foundFreeJobSite) {
            villager.restock();
            syncVillagerToNbt();
            serverLevel.playSound(null, pos, SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.BLOCKS, 1.0f, 1.0f);
            me.tuanzi.util.ModLog.debug(null, pos.toString(), "灵笼贸易站检测到周围工作站，村民交易已自动刷新（村民: " + villager.getDisplayName().getString() + "）。");
        }
    }

    public void rotateVillager() {
        this.villagerYaw = (this.villagerYaw + 90.0f) % 360.0f;
        if (this.villagerUuid != null && this.level instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.villagerUuid);
            if (entity instanceof Villager v) {
                v.snapTo(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.33, this.worldPosition.getZ() + 0.5, this.villagerYaw, 0.0f);
                v.setYRot(this.villagerYaw);
                v.setYHeadRot(this.villagerYaw);
                v.setYBodyRot(this.villagerYaw);
            }
        }
        this.setChanged();
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.SPAWNER_PLACE, SoundSource.BLOCKS, 0.7f, 1.2f);
        }
    }

    public void releaseVillagerOnBreak(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            Villager villager = null;
            if (this.villagerUuid != null) {
                net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.villagerUuid);
                if (entity instanceof Villager v) {
                    villager = v;
                }
            }

            if (villager == null) {
                villager = this.getOrCreateVillager();
            }

            if (villager != null) {
                // 关闭无 AI、无敌与静音
                villager.setNoAi(false);
                villager.setInvulnerable(false);
                villager.setSilent(false);
                // 还原为 1.0 尺寸的正常人！
                villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1.0);
                
                // 移动到破坏的方块中心，并在世界上保留它！
                villager.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, villager.getYRot(), villager.getXRot());
                
                // 确保它被正确加入世界里（如果先前未激活）
                if (!villager.isAlive()) {
                    level.addFreshEntity(villager);
                }
            }
        }
        
        this.cachedVillager = null;
        this.storedVillagerNbt = null;
        this.villagerUuid = null;
        this.setChanged();
    }

    public ItemStack getDropStack() {
        this.syncVillagerToNbt();
        ItemStack stack = new ItemStack(ModBlocks.SOUL_MERCHANT_STATION);
        if (this.level != null) {
            CompoundTag beTag = this.saveWithoutMetadata(this.level.registryAccess());
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.put("BlockEntityTag", beTag);
            
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                net.minecraft.world.item.component.CustomData.of(blockEntityTag));
        }
        return stack;
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.storedVillagerNbt = input.read("StoredVillager", CompoundTag.CODEC).orElse(null);
        this.lastRestockCheckDay = input.getLongOr("LastRestockCheckDay", -1L);
        this.restockTimer = input.getIntOr("RestockTimer", 0);
        this.villagerUuid = input.read("VillagerUuid", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        this.villagerYaw = input.getFloatOr("VillagerYaw", 0.0f);
        this.cachedVillager = null;
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        this.syncVillagerToNbt();
        if (this.storedVillagerNbt != null && !this.storedVillagerNbt.isEmpty()) {
            output.store("StoredVillager", CompoundTag.CODEC, this.storedVillagerNbt);
        }
        output.putLong("LastRestockCheckDay", this.lastRestockCheckDay);
        output.putInt("RestockTimer", this.restockTimer);
        if (this.villagerUuid != null) {
            output.store("VillagerUuid", net.minecraft.core.UUIDUtil.CODEC, this.villagerUuid);
        }
        output.putFloat("VillagerYaw", this.villagerYaw);
    }
}
