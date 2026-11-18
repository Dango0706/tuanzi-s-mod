package fr.minevegas.casino;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CasinoMachine {
   public static final List<String> TYPES = List.of("roulette", "blackjack", "poker", "derby", "slots", "wheel", "plinko");

   private CasinoMachine() {
   }

   public static boolean usesBias(String type) {
      return type.equals("blackjack") || type.equals("poker");
   }

   public static String type(BlockEntity machine) {
      if (machine instanceof RouletteBlockEntity) {
         return "roulette";
      } else if (machine instanceof BlackjackTableBlockEntity) {
         return "blackjack";
      } else if (machine instanceof PokerTableBlockEntity) {
         return "poker";
      } else if (machine instanceof DuckDerbyBlockEntity) {
         return "derby";
      } else if (machine instanceof SlotMachineBlockEntity) {
         return "slots";
      } else if (machine instanceof FortuneWheelBlockEntity) {
         return "wheel";
      } else {
         return machine instanceof PlinkoBlockEntity ? "plinko" : "";
      }
   }

   public static String blockId(String type) {
      String var10000;
      switch (type) {
         case "roulette" -> var10000 = "roulette_table";
         case "blackjack" -> var10000 = "blackjack_table";
         case "poker" -> var10000 = "poker_table";
         case "derby" -> var10000 = "duck_derby";
         case "slots" -> var10000 = "slot_machine";
         case "wheel" -> var10000 = "fortune_wheel";
         default -> var10000 = type;
      }

      return var10000;
   }

   public static Component typeName(String type) {
      return Component.translatable("block.roulettecasino." + blockId(type));
   }

   public static Component displayName(String type, String name, int number) {
      return !name.isBlank() ? Component.literal(name) : typeName(type).copy().append(" #" + number);
   }

   public static BlockEntity at(Level level, BlockPos part) {
      if (!level.hasChunkAt(part)) {
         return null;
      } else {
         BlockState state = level.getBlockState(part);
         BlockPos anchor;
         if (state.getBlock() instanceof RouletteBlock) {
            anchor = RouletteBlock.origin(part, state);
         } else if (state.getBlock() instanceof BlackjackTableBlock) {
            anchor = BlackjackTableBlock.anchorOf(part, state);
         } else if (state.getBlock() instanceof PokerTableBlock) {
            anchor = PokerTableBlock.anchorOf(part, state);
         } else if (state.getBlock() instanceof DuckDerbyBlock) {
            anchor = DuckDerbyBlock.anchorOf(part, state);
         } else if (state.getBlock() instanceof SlotMachineBlock) {
            anchor = part.below(((SlotMachinePart)state.getValue(SlotMachineBlock.HALF)).ordinal());
         } else if (state.getBlock() instanceof FortuneWheelBlock) {
            anchor = FortuneWheelBlock.partPos(FortuneWheelBlock.originOf(part, state), (Direction)state.getValue(FortuneWheelBlock.FACING), 4);
         } else {
            if (!(state.getBlock() instanceof PlinkoBlock)) {
               return null;
            }

            anchor = PlinkoBlock.partPos(PlinkoBlock.originOf(part, state), (Direction)state.getValue(PlinkoBlock.FACING), 4);
         }

         return level.hasChunkAt(anchor) ? level.getBlockEntity(anchor) : null;
      }
   }
}
