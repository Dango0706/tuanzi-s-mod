package fr.minevegas.casino.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderType.class})
public interface PanelRenderTypeAccess {
   @Accessor("state")
   RenderSetup minevegas$state();
}
