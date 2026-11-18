package fr.minevegas.casinodeco;

import net.minecraft.util.StringRepresentable;

public enum SofaBack implements StringRepresentable {
   NONE("none", 0),
   NORTH("north", 1),
   EAST("east", 2),
   SOUTH("south", 4),
   WEST("west", 8),
   NORTH_EAST("north_east", 3),
   SOUTH_EAST("south_east", 6),
   SOUTH_WEST("south_west", 12),
   NORTH_WEST("north_west", 9);

   private final String name;
   private final int mask;

   private SofaBack(String name, int mask) {
      this.name = name;
      this.mask = mask;
   }

   public String getSerializedName() {
      return this.name;
   }

   public int mask() {
      return this.mask;
   }

   public static SofaBack fromMask(int mask) {
      for(SofaBack b : values()) {
         if (b.mask == mask) {
            return b;
         }
      }

      throw new IllegalArgumentException("Invalid sofa back: " + mask);
   }

   // $FF: synthetic method
   private static SofaBack[] $values() {
      return new SofaBack[]{NONE, NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};
   }
}
