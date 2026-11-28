package me.tuanzi.mixin.voxy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.cortex.voxy.commonImpl.importers.WorldImporter;
import me.tuanzi.integration.voxy.VoxyColorIngestContext;
import me.tuanzi.integration.voxy.VoxyColorSectionSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = WorldImporter.class, remap = false)
public class VoxyWorldImporterMixin {
    @Unique
    private static final ThreadLocal<Int2ObjectMap<VoxyColorSectionSnapshot>> TUANZIS_MOD$IMPORT_COLORS = new ThreadLocal<>();

    @Inject(
            method = "importChunkNBT",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/CompoundTag;getList(Ljava/lang/String;)Ljava/util/Optional;"
            ),
            require = 0
    )
    private void tuanzis_mod$extractChunkNbtColors(CompoundTag chunk, int regionX, int regionZ, CallbackInfo ci) {
        Int2ObjectMap<VoxyColorSectionSnapshot> map = VoxyColorSectionSnapshot.fromChunkNbtBlockEntities(chunk);
        if (map != null && !map.isEmpty()) {
            TUANZIS_MOD$IMPORT_COLORS.set(map);
        } else {
            TUANZIS_MOD$IMPORT_COLORS.remove();
        }
    }

    @Inject(method = "importChunkNBT", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$clearChunkNbtColors(CompoundTag chunk, int regionX, int regionZ, CallbackInfo ci) {
        TUANZIS_MOD$IMPORT_COLORS.remove();
    }

    @Inject(method = "importSectionNBT", at = @At("HEAD"), require = 0)
    private void tuanzis_mod$bindImportSectionColors(int x, int y, int z, CompoundTag section, CallbackInfo ci) {
        Int2ObjectMap<VoxyColorSectionSnapshot> map = TUANZIS_MOD$IMPORT_COLORS.get();
        VoxyColorIngestContext.setCurrent(map != null ? map.get(y) : null);
    }

    @Inject(method = "importSectionNBT", at = @At("RETURN"), require = 0)
    private void tuanzis_mod$unbindImportSectionColors(int x, int y, int z, CompoundTag section, CallbackInfo ci) {
        VoxyColorIngestContext.clear();
    }
}
