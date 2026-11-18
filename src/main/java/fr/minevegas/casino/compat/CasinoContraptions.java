package fr.minevegas.casino.compat;

import com.mojang.logging.LogUtils;
import fr.minevegas.casino.BlackjackTableBlock;
import fr.minevegas.casino.CasinoSeatBlock;
import fr.minevegas.casino.CasinoTelevisionBlock;
import fr.minevegas.casino.DuckDerbyBlock;
import fr.minevegas.casino.FortuneWheelBlock;
import fr.minevegas.casino.PlinkoBlock;
import fr.minevegas.casino.PokerTableBlock;
import fr.minevegas.casino.RouletteBlock;
import fr.minevegas.casino.SlotMachineBlock;
import fr.minevegas.casinodeco.CasinoBarBlock;
import fr.minevegas.casinodeco.CasinoChandelierBlock;
import fr.minevegas.casinodeco.CasinoChipCaseBlock;
import fr.minevegas.casinodeco.CasinoDecorBlock;
import fr.minevegas.casinodeco.CasinoSignBlock;
import fr.minevegas.casinodeco.CasinoSofaBlock;
import fr.minevegas.casinodeco.CasinoStackedDecorBlock;
import fr.minevegas.casinodeco.CasinoTableBlock;
import fr.minevegas.casinodeco.CasinoWallSconceBlock;
import fr.minevegas.casinodeco.LargeCasinoSignBlock;
import fr.minevegas.casinodeco.VelvetRopePostBlock;
import fr.minevegas.casinosound.SpeakerBlock;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;

public final class CasinoContraptions {
   private static final String[] CREATE = new String[]{"com.simibubi.create.api.contraption.BlockMovementChecks", "com.simibubi.create.content.contraptions.BlockMovementChecks"};
   private static final ThreadLocal<BlockPos> ORPHAN = new ThreadLocal();
   private static volatile boolean registered;
   private static volatile boolean answerFailed;

   private CasinoContraptions() {
   }

   private static boolean machine(Block block) {
      return block instanceof RouletteBlock || block instanceof BlackjackTableBlock || block instanceof PokerTableBlock || block instanceof DuckDerbyBlock || block instanceof SlotMachineBlock || block instanceof FortuneWheelBlock || block instanceof PlinkoBlock || block instanceof CasinoTelevisionBlock || block instanceof CasinoChandelierBlock || block instanceof LargeCasinoSignBlock;
   }

   public static PushReaction pistonReaction() {
      return PushReaction.IMMOVEABLE;
   }

   public static boolean brittle(BlockState state) {
      Block block = state.getBlock();
      return block instanceof SlotMachineBlock || block instanceof CasinoSeatBlock || block instanceof CasinoSofaBlock || block instanceof CasinoTableBlock || block instanceof CasinoBarBlock || block instanceof CasinoChipCaseBlock || block instanceof CasinoDecorBlock || block instanceof CasinoStackedDecorBlock || block instanceof CasinoWallSconceBlock || block instanceof CasinoSignBlock || block instanceof VelvetRopePostBlock || block instanceof SpeakerBlock;
   }

   public static boolean attached(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      Block block = state.getBlock();
      if (!machine(block) && !brittle(state)) {
         return false;
      } else if (machine(block) && level.getBlockState(pos.relative(direction)).is(block)) {
         return true;
      } else if (block instanceof SpeakerBlock) {
         Direction var10001;
         switch ((AttachFace)state.getValue(BlockStateProperties.ATTACH_FACE)) {
            case FLOOR -> var10001 = Direction.DOWN;
            case CEILING -> var10001 = Direction.UP;
            case WALL -> var10001 = ((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)).getOpposite();
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return direction == var10001;
      } else {
         boolean var10000;
         label94: {
            if (!(block instanceof CasinoTelevisionBlock) && !(block instanceof CasinoWallSconceBlock) && !(block instanceof CasinoSignBlock)) {
               label86: {
                  if (block instanceof CasinoDecorBlock) {
                     CasinoDecorBlock decor = (CasinoDecorBlock)block;
                     if (decor.onWall()) {
                        break label86;
                     }
                  }

                  var10000 = false;
                  break label94;
               }
            }

            var10000 = true;
         }

         boolean wall = var10000;
         if (wall && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return direction == ((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)).getOpposite();
         } else if (block instanceof CasinoChandelierBlock) {
            return direction == Direction.UP;
         } else {
            return direction == Direction.DOWN;
         }
      }
   }

   public static boolean movable(BlockState state) {
      return machine(state.getBlock()) || brittle(state);
   }

   public static void harvesting(BlockPos pos, boolean whole) {
      ORPHAN.set(whole ? null : pos.immutable());
   }

   public static boolean orphan(BlockPos pos) {
      BlockPos alone = (BlockPos)ORPHAN.get();
      ORPHAN.remove();
      return pos.equals(alone);
   }

   public static void register() {
      if (!registered) {
         synchronized(CasinoContraptions.class) {
            if (!registered) {
               registered = true;
               registerWithCreate();
            }
         }
      }
   }

   private static void registerWithCreate() {
      String[] var0 = CREATE;
      int var1 = var0.length;
      int var2 = 0;

      while(true) {
         if (var2 < var1) {
            String name = var0[var2];

            Class<?> checks;
            try {
               checks = Class.forName(name, true, CasinoContraptions.class.getClassLoader());
            } catch (LinkageError | ClassNotFoundException var9) {
               ++var2;
               continue;
            }

            try {
               Class<?> result = Class.forName(name + "$CheckResult", true, checks.getClassLoader());
               Object success = result.getField("SUCCESS").get((Object)null);
               Object pass = result.getField("PASS").get((Object)null);
               register(checks, "AttachedCheck", result, pass, (args) -> attached((BlockState)args[0], (BlockGetter)args[1], (BlockPos)args[2], (Direction)args[3]) ? success : pass, BlockState.class, Level.class, BlockPos.class, Direction.class);
               register(checks, "MovementAllowedCheck", result, pass, (args) -> movable((BlockState)args[0]) ? success : pass, BlockState.class, Level.class, BlockPos.class);
               register(checks, "BrittleCheck", result, pass, (args) -> brittle((BlockState)args[0]) ? success : pass, BlockState.class);
               LogUtils.getLogger().info("[MineVegas Casino] Create found: its contraptions move the casino machines whole");
            } catch (RuntimeException | LinkageError | ReflectiveOperationException error) {
               LogUtils.getLogger().warn((String)"[MineVegas Casino] Create's movement checks could not be registered", (Throwable)error);
            }

            return;
         }

         return;
      }
   }

   private static void register(Class<?> checks, String type, Class<?> result, Object pass, Function<Object[], Object> answer, Class<?>... question) throws ReflectiveOperationException {
      Class<?> check = Class.forName(checks.getName() + "$" + type, true, checks.getClassLoader());
      Method asked = null;

      for(Method method : check.getMethods()) {
         if (Modifier.isAbstract(method.getModifiers())) {
            asked = method;
         }
      }

      if (asked != null && asked.getReturnType() == result && Arrays.equals(asked.getParameterTypes(), question)) {
         Object proxy = Proxy.newProxyInstance(check.getClassLoader(), new Class[]{check}, (self, methodx, args) -> {
            if (methodx.isDefault()) {
               return InvocationHandler.invokeDefault(self, methodx, args);
            } else {
               Object var10000;
               switch (methodx.getName()) {
                  case "equals":
                     Boolean var12 = self == args[0];
                     var10000 = var12;
                     break;
                  case "hashCode":
                     Integer var11 = System.identityHashCode(self);
                     var10000 = var11;
                     break;
                  case "toString":
                     String var8 = "MineVegas " + type;
                     var10000 = var8;
                     break;
                  default:
                     Object var13;
                     try {
                        var13 = answer.apply(args);
                     } catch (RuntimeException error) {
                        if (!answerFailed) {
                           answerFailed = true;
                           LogUtils.getLogger().warn((String)"[MineVegas Casino] A Create movement check failed; Create decides alone", (Throwable)error);
                        }

                        var10000 = pass;
                        return var10000;
                     }

                     var10000 = var13;
               }

               return var10000;
            }
         });
         checks.getMethod("register" + type, check).invoke((Object)null, proxy);
      } else {
         throw new NoSuchMethodException("Create's " + type + " is no longer " + Arrays.toString(question));
      }
   }
}
