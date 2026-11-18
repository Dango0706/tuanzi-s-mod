package fr.minevegas.casino.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   targets = {"net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding"}
)
public interface PanelTextureBindingAccess {
   @Accessor("location")
   Identifier minevegas$location();
}
