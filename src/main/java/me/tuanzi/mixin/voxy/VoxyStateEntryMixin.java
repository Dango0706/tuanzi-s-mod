package me.tuanzi.mixin.voxy;

import me.cortex.voxy.common.world.other.Mapper;
import me.tuanzi.integration.voxy.VoxyStateEntryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Pseudo
@Mixin(value = Mapper.StateEntry.class, remap = false)
public class VoxyStateEntryMixin implements VoxyStateEntryAccess {
    @Unique
    private static final String KEY_COLOR = "tuanzis_color";
    @Unique
    private static final String KEY_TOP_COLOR = "tuanzis_top_color";
    @Unique
    private static final ThreadLocal<int[]> TUANZIS_MOD$DESERIALIZED_COLORS = ThreadLocal.withInitial(() -> new int[]{-1, -1});

    @Shadow
    @Final
    public BlockState state;

    /**
     * 以 +1 偏移存储颜色（0 表示未设置自定义颜色），确保即便 JVM 仅做零初始化也不会误判为 #000000。
     */
    @Unique
    private int tuanzis_mod$colorPlusOne;

    @Unique
    private int tuanzis_mod$topColorPlusOne;

    @Override
    public int tuanzis_mod$getCustomColor() {
        return this.tuanzis_mod$colorPlusOne - 1;
    }

    @Override
    public int tuanzis_mod$getCustomTopColor() {
        return this.tuanzis_mod$topColorPlusOne - 1;
    }

    @Override
    public void tuanzis_mod$setCustomColors(int color, int topColor) {
        this.tuanzis_mod$colorPlusOne = color >= 0 ? ((color & 0xFFFFFF) + 1) : 0;
        this.tuanzis_mod$topColorPlusOne = topColor >= 0 ? ((topColor & 0xFFFFFF) + 1) : 0;
    }

    @Redirect(
            method = "serialize",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V"
            ),
            require = 0
    )
    private void tuanzis_mod$writeColorFields(CompoundTag tag, OutputStream out) throws IOException {
        int c = this.tuanzis_mod$getCustomColor();
        int tc = this.tuanzis_mod$getCustomTopColor();
        if (c >= 0) {
            tag.putInt(KEY_COLOR, c);
        }
        if (tc >= 0) {
            tag.putInt(KEY_TOP_COLOR, tc);
        }
        NbtIo.writeCompressed(tag, out);
    }

    @Redirect(
            method = "deserialize",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;readCompressed(Ljava/io/InputStream;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;"
            ),
            require = 0
    )
    private static CompoundTag tuanzis_mod$readColorFields(InputStream input, NbtAccounter accounter) throws IOException {
        CompoundTag compound = NbtIo.readCompressed(input, accounter);
        int[] holder = TUANZIS_MOD$DESERIALIZED_COLORS.get();
        holder[0] = compound.getIntOr(KEY_COLOR, -1);
        holder[1] = compound.getIntOr(KEY_TOP_COLOR, -1);
        return compound;
    }

    @Inject(method = "deserialize", at = @At("RETURN"), require = 0)
    private static void tuanzis_mod$applyDeserializedColors(int id, byte[] data, boolean[] forceResave, CallbackInfoReturnable<Mapper.StateEntry> cir) {
        int[] holder = TUANZIS_MOD$DESERIALIZED_COLORS.get();
        int c = holder[0];
        int tc = holder[1];
        holder[0] = -1;
        holder[1] = -1;

        Mapper.StateEntry entry = cir.getReturnValue();
        if (entry != null && !entry.state.isAir() && c >= 0) {
            ((VoxyStateEntryAccess) (Object) entry).tuanzis_mod$setCustomColors(c, tc);
        }
    }
}
