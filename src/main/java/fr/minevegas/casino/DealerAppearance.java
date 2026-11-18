package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record DealerAppearance(String entityType, String model, String variant, CompoundTag skin, String name) {
   public static final String KEY = "MineVegasDealer";
   public static final DealerAppearance EMPTY = new DealerAppearance("", "", "", new CompoundTag(), "");
   public static final Codec<DealerAppearance> CODEC = RecordCodecBuilder.<DealerAppearance>create((i) -> i.group(Codec.STRING.fieldOf("entity").forGetter(DealerAppearance::entityType), Codec.STRING.fieldOf("model").forGetter(DealerAppearance::model), Codec.STRING.fieldOf("variant").forGetter(DealerAppearance::variant), CompoundTag.CODEC.fieldOf("skin").forGetter(DealerAppearance::skin), Codec.STRING.optionalFieldOf("name", "").forGetter(DealerAppearance::name)).apply(i, DealerAppearance::new));

   public DealerAppearance(String entityType, String model, String variant, CompoundTag skin, String name) {
      skin = skin.copy();
      this.entityType = entityType;
      this.model = model;
      this.variant = variant;
      this.skin = skin;
      this.name = name;
   }

   public CompoundTag skin() {
      return this.skin.copy();
   }

   public boolean isEmpty() {
      return this.entityType.isEmpty();
   }

   public static DealerAppearance from(ItemStack stack) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      return tag != null && tag.get("MineVegasDealer") != null ? (DealerAppearance)CODEC.parse(NbtOps.INSTANCE, tag.get("MineVegasDealer")).result().orElse(EMPTY) : EMPTY;
   }

   public void putOn(ItemStack stack) {
      if (!this.isEmpty()) {
         CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         CODEC.encodeStart(NbtOps.INSTANCE, this).result().ifPresent((value) -> tag.put("MineVegasDealer", value));
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }
}
