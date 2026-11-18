package fr.minevegas.casino.mixin;

import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderSetup.class})
public interface PanelRenderSetupAccess {
   @Accessor("textures")
   Map<String, ?> minevegas$textures();
}
