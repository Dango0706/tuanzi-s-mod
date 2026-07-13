package me.tuanzi.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.PacketSendListener;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.UUID;

public class SimulatorFakePlayer extends ServerPlayer {
    private static final GameProfile PROFILE = new GameProfile(
        UUID.fromString("6a461d36-8c46-4e5b-b9d9-4d575ea48d8e"), 
        "[PlayerSimulator]"
    );

    public SimulatorFakePlayer(ServerLevel level) {
        super(level.getServer(), level, PROFILE, ClientInformation.createDefault());
        
        // 显式将生存相关的玩家能力设为 false，确保耐久扣除在原版判定中畅通无阻
        this.getAbilities().instabuild = false;
        this.getAbilities().invulnerable = false;
        this.getAbilities().mayfly = false;
        
        // 创建虚拟的网络连接以防 NullPointerException
        net.minecraft.network.Connection dummyConn = new net.minecraft.network.Connection(PacketFlow.CLIENTBOUND);
        this.connection = new ServerGamePacketListenerImpl(
            level.getServer(), 
            dummyConn, 
            this, 
            CommonListenerCookie.createInitial(PROFILE, false)
        );
    }

    public void publicUpdateUsingItem() {
        this.updateUsingItem(this.getMainHandItem());
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    // 确保假玩家不被常规事件影响
    @Override
    public boolean isLocalPlayer() {
        return false;
    }

    public void setAttackStrengthTicker(int ticks) {
        this.attackStrengthTicker = ticks;
    }

    public int getAttackStrengthTicker() {
        return this.attackStrengthTicker;
    }

    public void updateEquipmentModifiers(net.minecraft.world.item.ItemStack previous, net.minecraft.world.item.ItemStack current) {
        if (!previous.isEmpty()) {
            previous.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                net.minecraft.world.entity.ai.attributes.AttributeInstance instance = this.getAttributes().getInstance(attribute);
                if (instance != null) {
                    instance.removeModifier(modifier.id());
                }
            });
        }
        if (!current.isEmpty()) {
            current.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                net.minecraft.world.entity.ai.attributes.AttributeInstance instance = this.getAttributes().getInstance(attribute);
                if (instance != null) {
                    instance.removeModifier(modifier.id());
                    instance.addTransientModifier(modifier);
                }
            });
        }
    }

    private UUID ownerUuid = null;

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public net.minecraft.world.entity.player.Player getOwnerPlayer() {
        if (this.ownerUuid == null) {
            return null;
        }
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(this.ownerUuid);
        }
        return null;
    }

    @Override
    public net.minecraft.world.damagesource.DamageSources damageSources() {
        net.minecraft.world.entity.player.Player owner = this.getOwnerPlayer();
        if (owner != null) {
            return new net.minecraft.world.damagesource.DamageSources(this.registryAccess()) {
                @Override
                public net.minecraft.world.damagesource.DamageSource playerAttack(net.minecraft.world.entity.player.Player attacker) {
                    return super.playerAttack(owner);
                }

                @Override
                public net.minecraft.world.damagesource.DamageSource arrow(net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow, net.minecraft.world.entity.Entity shooter) {
                    return super.arrow(arrow, owner);
                }

                @Override
                public net.minecraft.world.damagesource.DamageSource trident(net.minecraft.world.entity.Entity trident, net.minecraft.world.entity.Entity shooter) {
                    return super.trident(trident, owner);
                }
            };
        }
        return super.damageSources();
    }
}
