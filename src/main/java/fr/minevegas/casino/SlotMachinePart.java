package fr.minevegas.casino;

import net.minecraft.util.StringRepresentable;

public enum SlotMachinePart implements StringRepresentable {
   LOWER("lower"),
   UPPER("upper"),
   TOP("top");

   private final String serializedName;

   private SlotMachinePart(String serializedName) {
      this.serializedName = serializedName;
   }

   public String getSerializedName() {
      return this.serializedName;
   }

   public SlotMachinePart above() {
      return this == LOWER ? UPPER : (this == UPPER ? TOP : null);
   }

   public SlotMachinePart below() {
      return this == TOP ? UPPER : (this == UPPER ? LOWER : null);
   }

   // $FF: synthetic method
   private static SlotMachinePart[] $values() {
      return new SlotMachinePart[]{LOWER, UPPER, TOP};
   }
}
