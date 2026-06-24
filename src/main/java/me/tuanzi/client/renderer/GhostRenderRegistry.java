package me.tuanzi.client.renderer;

import me.tuanzi.mixin.client.RenderTypeAccessor;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.WeakHashMap;

public class GhostRenderRegistry {
    private static final Map<RenderType, GhostRenderInfo> registry = new WeakHashMap<>();

    public static class GhostRenderInfo {
        public final RenderType original;
        public final float alpha;

        public GhostRenderInfo(RenderType original, float alpha) {
            this.original = original;
            this.alpha = alpha;
        }
    }

    public static RenderType getOrCreateGhostType(RenderType original, float alpha) {
        synchronized (registry) {
            // 用以缓存对应的 ghost 类型
            for (Map.Entry<RenderType, GhostRenderInfo> entry : registry.entrySet()) {
                if (entry.getValue().original == original && Math.abs(entry.getValue().alpha - alpha) < 0.01f) {
                    return entry.getKey();
                }
            }

            RenderType ghostType = convertToTranslucent(original);
            registry.put(ghostType, new GhostRenderInfo(original, alpha));
            return ghostType;
        }
    }

    public static void registerGhostType(RenderType ghostType, RenderType original, float alpha) {
        synchronized (registry) {
            registry.put(ghostType, new GhostRenderInfo(original, alpha));
        }
    }

    private static RenderType convertToTranslucent(RenderType original) {
        String name = original.toString();
        Identifier texture = getTextureLocation(original);

        if (texture != null) {
            String path = texture.getPath();
            // 实体、盔甲或道具
            if (name.contains("entity") || name.contains("armor") || name.contains("item") || path.contains("entity/")) {
                if (name.contains("armor")) {
                    return net.minecraft.client.renderer.rendertype.RenderTypes.armorTranslucent(texture);
                } else if (name.contains("item")) {
                    return net.minecraft.client.renderer.rendertype.RenderTypes.itemTranslucent(texture);
                } else {
                    return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
                }
            }
        }

        // 方块或者其它使用方块图册的类型
        if (name.contains("block") || name.contains("moving_block") || name.contains("solid") || name.contains("cutout")) {
            return net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock();
        }

        return original;
    }

    public static Identifier getTextureLocation(RenderType renderType) {
        try {
            RenderSetup state = ((RenderTypeAccessor) renderType).getState();
            java.lang.reflect.Field texturesField = state.getClass().getDeclaredField("textures");
            texturesField.setAccessible(true);
            java.util.Map<?, ?> texturesMap = (java.util.Map<?, ?>) texturesField.get(state);
            Object binding = texturesMap.get("Sampler0");
            if (binding != null) {
                java.lang.reflect.Method locationMethod = binding.getClass().getMethod("location");
                return (Identifier) locationMethod.invoke(binding);
            }
        } catch (Exception e) {
            me.tuanzi.util.ModLog.debug("GhostRenderRegistry: Failed to extract texture location for RenderType: " + renderType + ", error: " + e.getMessage());
        }
        return null;
    }

    public static GhostRenderInfo getInfo(RenderType renderType) {
        synchronized (registry) {
            return registry.get(renderType);
        }
    }
}
