package me.tuanzi.mixin.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import me.tuanzi.util.BlueprintGhostHolder;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements BlueprintGhostHolder {
    private boolean tuanzis_mod$isBlueprintGhost = false;

    @Override
    public boolean tuanzis_mod$isBlueprintGhost() {
        return this.tuanzis_mod$isBlueprintGhost;
    }

    @Override
    public void tuanzis_mod$setBlueprintGhost(boolean val) {
        this.tuanzis_mod$isBlueprintGhost = val;
    }
}
