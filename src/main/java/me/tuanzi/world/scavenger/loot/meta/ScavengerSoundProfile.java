package me.tuanzi.world.scavenger.loot.meta;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.IntFunction;

/**
 * 搜刮物品声效配置组（控制搜索翻找、高价值预警脉冲与网格拿放音效）
 */
public enum ScavengerSoundProfile implements StringRepresentable {
    DEFAULT_TACTICAL("DEFAULT_TACTICAL", SoundEvents.BUNDLE_INSERT, SoundEvents.UI_BUTTON_CLICK.value(), SoundEvents.ARMOR_EQUIP_GENERIC.value()),
    FABRIC_RUSTLE("FABRIC_RUSTLE", SoundEvents.BUNDLE_REMOVE_ONE, SoundEvents.WOOL_STEP, SoundEvents.ARMOR_EQUIP_LEATHER.value()),
    PLASTIC_SNAP("PLASTIC_SNAP", SoundEvents.ITEM_PICKUP, SoundEvents.COPPER_BULB_TURN_ON, SoundEvents.DISPENSER_DISPENSE),
    TECH_BEEP("TECH_BEEP", SoundEvents.NOTE_BLOCK_BIT.value(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.COPPER_BULB_TURN_OFF),
    HEAVY_METAL("HEAVY_METAL", SoundEvents.IRON_DOOR_CLOSE, SoundEvents.ANVIL_PLACE, SoundEvents.ARMOR_EQUIP_NETHERITE.value()),
    CARD_SWIPE("CARD_SWIPE", SoundEvents.BOOK_PAGE_TURN, SoundEvents.VAULT_INSERT_ITEM, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT),
    ABYSSAL_RESONANCE("ABYSSAL_RESONANCE", SoundEvents.SCULK_CATALYST_BLOOM, SoundEvents.BEACON_ACTIVATE, SoundEvents.AMETHYST_CLUSTER_PLACE);

    public static final Codec<ScavengerSoundProfile> CODEC = new StringRepresentable.EnumCodec<>(
            values(),
            name -> {
                if (name == null) return null;
                String upper = name.toUpperCase(Locale.ROOT);
                for (ScavengerSoundProfile v : values()) {
                    if (v.serializedName.equals(upper)) return v;
                }
                return null;
            }
    );

    private static final IntFunction<ScavengerSoundProfile> BY_ID = ByIdMap.continuous(
            Enum::ordinal,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, ScavengerSoundProfile> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String serializedName;
    private final SoundEvent searchingSound;
    private final SoundEvent revealSound;
    private final SoundEvent placeSound;

    ScavengerSoundProfile(String serializedName, SoundEvent searchingSound, SoundEvent revealSound, SoundEvent placeSound) {
        this.serializedName = serializedName;
        this.searchingSound = searchingSound;
        this.revealSound = revealSound;
        this.placeSound = placeSound;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public SoundEvent searchingSound() {
        return this.searchingSound;
    }

    public SoundEvent revealSound() {
        return this.revealSound;
    }

    public SoundEvent placeSound() {
        return this.placeSound;
    }
}
