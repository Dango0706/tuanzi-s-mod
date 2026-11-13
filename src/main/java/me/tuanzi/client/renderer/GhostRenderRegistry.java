package me.tuanzi.client.renderer;

import me.tuanzi.util.ModLog;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

public class GhostRenderRegistry {
    private static final Map<RenderType, GhostRenderInfo> registry = new WeakHashMap<>();

    public static RenderType getOrCreateGhostType(RenderType original, float alpha) {
        if (original == null) {
            return Sheets.translucentBlockItemSheet();
        }
        synchronized (registry) {
            for (Map.Entry<RenderType, GhostRenderInfo> entry : registry.entrySet()) {
                if (entry.getValue().original == original && Math.abs(entry.getValue().alpha - alpha) < 0.01f) {
                    return entry.getKey();
                }
            }

            RenderType ghostType = convertToTranslucent(original);
            registry.put(ghostType, new GhostRenderInfo(original, alpha));
            ModLog.debug("[GhostRenderRegistry] 映射半透明渲染类型: " + original + " -> " + ghostType + " (alpha=" + alpha + ")");
            return ghostType;
        }
    }

    public static void registerGhostType(RenderType ghostType, RenderType original, float alpha) {
        synchronized (registry) {
            registry.put(ghostType, new GhostRenderInfo(original, alpha));
        }
    }

    private static RenderType convertToTranslucent(RenderType original) {
        if (original == Sheets.cutoutBlockItemSheet() || original == Sheets.translucentBlockItemSheet()) {
            return Sheets.translucentBlockItemSheet();
        }
        if (original == Sheets.cutoutItemSheet() || original == Sheets.translucentItemSheet()) {
            return Sheets.translucentItemSheet();
        }
        if (original.hasBlending()) {
            return original;
        }

        String name = original.toString();
        Identifier texture = getTextureLocation(original);

        if (texture != null) {
            if (texture.equals(TextureAtlas.LOCATION_BLOCKS)) {
                if (name.contains("entity") && !name.contains("item")) {
                    return RenderTypes.entityTranslucent(texture);
                }
                return Sheets.translucentBlockItemSheet();
            }
            if (name.contains("item")) {
                return RenderTypes.itemTranslucent(texture);
            }
            return RenderTypes.entityTranslucent(texture);
        }

        if (name.contains("block") || name.contains("moving_block") || name.contains("solid") || name.contains("cutout")) {
            return Sheets.translucentBlockItemSheet();
        }

        return original;
    }

    public static Identifier getTextureLocation(RenderType renderType) {
        if (renderType == null) return null;
        try {
            Field stateField = null;
            for (Field f : RenderType.class.getDeclaredFields()) {
                if (RenderSetup.class.isAssignableFrom(f.getType())) {
                    stateField = f;
                    break;
                }
            }
            if (stateField != null) {
                stateField.setAccessible(true);
                RenderSetup state = (RenderSetup) stateField.get(renderType);
                if (state != null) {
                    Field texturesField = null;
                    for (Field f : RenderSetup.class.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(f.getType())) {
                            texturesField = f;
                            break;
                        }
                    }
                    if (texturesField != null) {
                        texturesField.setAccessible(true);
                        Map<?, ?> texturesMap = (Map<?, ?>) texturesField.get(state);
                        if (texturesMap != null) {
                            Object binding = texturesMap.get("Sampler0");
                            if (binding != null) {
                                for (Field bf : binding.getClass().getDeclaredFields()) {
                                    if (Identifier.class.isAssignableFrom(bf.getType())) {
                                        bf.setAccessible(true);
                                        return (Identifier) bf.get(binding);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static GhostRenderInfo getInfo(RenderType renderType) {
        synchronized (registry) {
            return registry.get(renderType);
        }
    }

    public static class GhostRenderInfo {
        public final RenderType original;
        public final float alpha;

        public GhostRenderInfo(RenderType original, float alpha) {
            this.original = original;
            this.alpha = alpha;
        }
    }
}
