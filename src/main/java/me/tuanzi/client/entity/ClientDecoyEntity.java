package me.tuanzi.client.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import me.tuanzi.entity.DecoyEntity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class ClientDecoyEntity extends DecoyEntity implements ClientAvatarEntity {
    private final ClientAvatarState avatarState = new ClientAvatarState();
    private final PlayerSkinRenderCache skinRenderCache;
    private PlayerSkin skin;
    private CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> skinLookup;

    public ClientDecoyEntity(Level level, PlayerSkinRenderCache skinRenderCache) {
        super(me.tuanzi.init.ModEntities.DECOY, level);
        this.skinRenderCache = skinRenderCache;
        this.skin = DefaultPlayerSkin.get(this.getProfile().partialProfile());
    }

    @Override
    public void tick() {
        super.tick();
        this.avatarState.tick(this.position(), this.getDeltaMovement());
        
        if (this.skinLookup != null && this.skinLookup.isDone()) {
            try {
                this.skinLookup.get().ifPresent(info -> this.skin = info.playerSkin());
                this.skinLookup = null;
            } catch (Exception e) {
                // 忽略异常
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor.equals(DATA_PROFILE)) {
            this.updateSkin();
        }
    }

    private void updateSkin() {
        if (this.skinLookup != null) {
            CompletableFuture<Optional<PlayerSkinRenderCache.RenderInfo>> future = this.skinLookup;
            this.skinLookup = null;
            future.cancel(false);
        }
        this.skinLookup = this.skinRenderCache.lookup(this.getProfile());
    }

    @Override
    public ClientAvatarState avatarState() {
        return this.avatarState;
    }

    @Override
    public PlayerSkin getSkin() {
        return this.skin;
    }

    @Override
    public Parrot.Variant getParrotVariantOnShoulder(boolean left) {
        return null;
    }

    @Override
    public boolean showExtraEars() {
        return false;
    }
}
