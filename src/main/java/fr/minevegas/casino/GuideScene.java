package fr.minevegas.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.minevegas.casinodeco.CasinoChandelierBlock;
import fr.minevegas.casinodeco.CasinoSofaBlock;
import fr.minevegas.casinodeco.LargeCasinoSignBlock;
import fr.minevegas.casinodeco.SofaBack;
import fr.minevegas.casinodeco.SofaLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class GuideScene {
   private static final int AWAY = 1000000;
   private final List<Placed> blocks;
   private List<Machine> machines;
   private List<GuideFlames.Flame> flames;
   private String compatibility = "";
   private int seed;
   private final boolean hiring;
   private GuideHiringPreview hiringPreview;
   private final String animated;
   private final int frames;
   private final int ticksPerFrame;
   public final float centerX;
   public final float centerY;
   public final float centerZ;
   public final float radius;
   public final float height;
   public final float yaw;
   public final float pitch;
   public final float zoom;
   public final boolean spin;

   private GuideScene(List<Placed> blocks, String animated, int frames, int ticksPerFrame, float yaw, float pitch, float zoom, boolean spin, float[] focus, boolean hiring) {
      this.blocks = List.copyOf(blocks);
      this.hiring = hiring;
      this.animated = animated;
      this.frames = Math.max(1, frames);
      this.ticksPerFrame = Math.max(1, ticksPerFrame);
      this.yaw = yaw;
      this.pitch = pitch;
      this.zoom = zoom;
      this.spin = spin;
      int x0 = Integer.MAX_VALUE;
      int y0 = Integer.MAX_VALUE;
      int z0 = Integer.MAX_VALUE;
      int x1 = Integer.MIN_VALUE;
      int y1 = Integer.MIN_VALUE;
      int z1 = Integer.MIN_VALUE;

      for(Placed placed : this.blocks) {
         x0 = Math.min(x0, placed.x);
         y0 = Math.min(y0, placed.y);
         z0 = Math.min(z0, placed.z);
         x1 = Math.max(x1, placed.x + 1);
         y1 = Math.max(y1, placed.y + 1);
         z1 = Math.max(z1, placed.z + 1);
      }

      if (this.blocks.isEmpty()) {
         z0 = 0;
         y0 = 0;
         x0 = 0;
         z1 = 1;
         y1 = 1;
         x1 = 1;
      }

      this.centerX = focus != null ? focus[0] : (float)(x0 + x1) / 2.0F;
      this.centerY = focus != null ? focus[1] : (float)(y0 + y1) / 2.0F;
      this.centerZ = focus != null ? focus[2] : (float)(z0 + z1) / 2.0F;
      this.radius = focus != null ? focus[3] : (float)Math.hypot((double)(x1 - x0), (double)(z1 - z0)) / 2.0F;
      this.height = focus != null ? focus[4] : (float)(y1 - y0);
   }

   public List<Placed> blocks(int age) {
      if (this.animated == null) {
         return this.blocks;
      } else {
         int frame = age / this.ticksPerFrame % this.frames;
         if (this.animated.equals("screen")) {
            int channel = 0;

            for(int i = 0; i < frame; ++i) {
               channel = CasinoTelevisionChannels.next(channel);
            }

            frame = channel;
         }

         String value = Integer.toString(frame);
         List<Placed> moved = new ArrayList(this.blocks.size());

         for(Placed placed : this.blocks) {
            BlockState state = with(placed.state, this.animated, value);
            if (this.animated.equals("screen") && state.getBlock() instanceof CasinoTelevisionBlock) {
               state = (BlockState)state.setValue(CasinoTelevisionBlock.ON, frame != 0);
            }

            moved.add(new Placed(state, placed.x, placed.y, placed.z));
         }

         return moved;
      }
   }

   public List<Machine> machines() {
      if (this.machines == null) {
         this.machines = new ArrayList();
         ClientLevel level = Minecraft.getInstance().level;

         for(Placed placed : this.blocks) {
            Block block = placed.state.getBlock();
            boolean game = block instanceof RouletteBlock || block instanceof BlackjackTableBlock || block instanceof PokerTableBlock || block instanceof DuckDerbyBlock || block instanceof SlotMachineBlock || block instanceof FortuneWheelBlock || block instanceof PlinkoBlock;
            if (game && level != null) {
               BlockEntity entity = ((EntityBlock)block).newBlockEntity(new BlockPos(placed.x + 1000000 + this.seed * 137, placed.y, placed.z + 1000000 + this.seed * 137), placed.state);
               if (entity != null) {
                  entity.setLevel(level);
                  if (this.compatibility.equals("easy_npc") && entity instanceof CasinoDealerTable) {
                     CasinoDealerTable table = (CasinoDealerTable)entity;
                     table.dealerAppearance(EasyNpcDealerRenderer.guideAppearance());
                  }

                  if (this.compatibility.equals("mca") && entity instanceof CasinoDealerTable) {
                     GuideDealerPreview.mark(entity, block instanceof PokerTableBlock ? GuideDealerPreview.Preset.POKER : GuideDealerPreview.Preset.BLACKJACK);
                  }

                  this.machines.add(new Machine(placed, entity));
               }
            }
         }
      }

      return this.machines;
   }

   public List<GuideFlames.Flame> flames() {
      if (this.flames == null) {
         this.flames = GuideFlames.positions(this.blocks);
      }

      return this.flames;
   }

   public GuideHiringPreview.Frame hiringFrame(float age) {
      if (this.hiring && Minecraft.getInstance().player != null) {
         if (this.hiringPreview == null) {
            this.hiringPreview = new GuideHiringPreview(this.compatibility.equals("easy_npc"));
         }

         return this.hiringPreview.frame(age);
      } else {
         return GuideHiringPreview.Frame.EMPTY;
      }
   }

   public float fit(int w, int h, float viewPitch) {
      double tilt = Math.toRadians((double)viewPitch);
      double tall = (double)this.height * Math.cos(tilt) + (double)(2.0F * this.radius) * Math.sin(tilt);
      return this.zoom * (float)Math.min((double)w / ((double)2.0F * (double)this.radius), (double)h / tall);
   }

   public static GuideScene parse(JsonObject json) {
      List<Placed> blocks = new ArrayList();

      for(JsonElement element : json.getAsJsonArray("blocks")) {
         JsonObject entry = element.getAsJsonObject();
         if (entry.has("sofa")) {
            sofa(blocks, entry.getAsJsonArray("sofa"), entry.get("y").getAsInt());
         } else {
            int[] at = ints(entry.getAsJsonArray("at"));
            if (entry.has("machine")) {
               machine(blocks, block(entry.get("machine").getAsString()), Direction.byName(entry.has("facing") ? entry.get("facing").getAsString() : "south"), at[0], at[1], at[2]);
            } else {
               BlockState state = state(entry.get("state").getAsString());
               int[] to = entry.has("to") ? ints(entry.getAsJsonArray("to")) : at;

               for(int x = Math.min(at[0], to[0]); x <= Math.max(at[0], to[0]); ++x) {
                  for(int y = Math.min(at[1], to[1]); y <= Math.max(at[1], to[1]); ++y) {
                     for(int z = Math.min(at[2], to[2]); z <= Math.max(at[2], to[2]); ++z) {
                        blocks.add(new Placed(state, x, y, z));
                     }
                  }
               }
            }
         }
      }

      JsonObject animate = json.has("animate") ? json.getAsJsonObject("animate") : null;
      float[] focus = null;
      if (json.has("focus")) {
         JsonArray values = json.getAsJsonArray("focus");
         focus = new float[5];

         for(int i = 0; i < 5; ++i) {
            focus[i] = values.get(i).getAsFloat();
         }
      }

      GuideScene result = new GuideScene(blocks, animate == null ? null : animate.get("property").getAsString(), animate == null ? 1 : animate.get("count").getAsInt(), animate == null ? 1 : animate.get("ticks").getAsInt(), json.has("yaw") ? json.get("yaw").getAsFloat() : -35.0F, json.has("pitch") ? json.get("pitch").getAsFloat() : 30.0F, json.has("zoom") ? json.get("zoom").getAsFloat() : 1.0F, !json.has("spin") || json.get("spin").getAsBoolean(), focus, json.has("hiring") && json.get("hiring").getAsBoolean());
      result.compatibility = json.has("compat") ? json.get("compat").getAsString() : "";
      if (json.has("seed")) {
         result.seed = json.get("seed").getAsInt();
      }

      return result;
   }

   private static int[] ints(JsonArray array) {
      return new int[]{array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()};
   }

   static void machine(List<Placed> out, Block block, Direction facing, int x, int y, int z) {
      if (facing == null || facing.getAxis() == Axis.Y) {
         facing = Direction.SOUTH;
      }

      if (block instanceof RouletteBlock) {
         for(int part = 0; part < 4; ++part) {
            out.add(new Placed((BlockState)block.defaultBlockState().setValue(RouletteBlock.PART, part), x + (part & 1), y, z + (part >> 1)));
         }
      } else if (block instanceof BlackjackTableBlock) {
         for(int part = 0; part < 8; ++part) {
            add(out, (BlockState)((BlockState)block.defaultBlockState().setValue(BlackjackTableBlock.FACING, facing)).setValue(BlackjackTableBlock.PART, part), BlackjackTableBlock.partPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof PokerTableBlock) {
         for(int part = 0; part < 12; ++part) {
            add(out, (BlockState)((BlockState)block.defaultBlockState().setValue(PokerTableBlock.FACING, facing)).setValue(PokerTableBlock.PART, part), PokerTableBlock.partPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof DuckDerbyBlock) {
         for(int part = 0; part < 12; ++part) {
            add(out, (BlockState)((BlockState)block.defaultBlockState().setValue(DuckDerbyBlock.FACING, facing)).setValue(DuckDerbyBlock.PART, part), DuckDerbyBlock.partPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof FortuneWheelBlock) {
         for(int part = 0; part < 9; ++part) {
            add(out, (BlockState)((BlockState)block.defaultBlockState().setValue(FortuneWheelBlock.FACING, facing)).setValue(FortuneWheelBlock.PART, part), FortuneWheelBlock.partPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof PlinkoBlock) {
         for(int part = 0; part < 9; ++part) {
            add(out, (BlockState)((BlockState)block.defaultBlockState().setValue(PlinkoBlock.FACING, facing)).setValue(PlinkoBlock.PART, part), PlinkoBlock.partPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof SlotMachineBlock) {
         int level = 0;

         for(SlotMachinePart part = SlotMachinePart.LOWER; part != null; part = part.above()) {
            out.add(new Placed((BlockState)((BlockState)block.defaultBlockState().setValue(SlotMachineBlock.FACING, facing)).setValue(SlotMachineBlock.HALF, part), x, y + level++, z));
         }
      } else if (block instanceof CasinoTelevisionBlock) {
         CasinoTelevisionBlock television = (CasinoTelevisionBlock)block;

         for(int part = 0; part < television.parts(); ++part) {
            add(out, (BlockState)((BlockState)((BlockState)block.defaultBlockState().setValue(CasinoTelevisionBlock.FACING, facing)).setValue(CasinoTelevisionBlock.ON, true)).setValue(television.partProperty(), part), television.cellPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof LargeCasinoSignBlock) {
         for(int part = 0; part < 6; ++part) {
            add(out, (BlockState)((BlockState)block.defaultBlockState().setValue(LargeCasinoSignBlock.FACING, facing)).setValue(LargeCasinoSignBlock.PART, part), LargeCasinoSignBlock.partPos(BlockPos.ZERO, facing, part), x, y, z);
         }
      } else if (block instanceof CasinoChandelierBlock) {
         for(int part = 0; part < 9; ++part) {
            add(out, (BlockState)block.defaultBlockState().setValue(CasinoChandelierBlock.PART, part), CasinoChandelierBlock.bodyPos(BlockPos.ZERO, part), x, y, z);
         }

         add(out, (BlockState)block.defaultBlockState().setValue(CasinoChandelierBlock.PART, 9), CasinoChandelierBlock.chainPos(BlockPos.ZERO, 1), x, y, z);
         add(out, (BlockState)block.defaultBlockState().setValue(CasinoChandelierBlock.PART, 10), CasinoChandelierBlock.chainPos(BlockPos.ZERO, 2), x, y, z);
      } else {
         out.add(new Placed(with(block.defaultBlockState(), "facing", facing.getSerializedName()), x, y, z));
      }

   }

   private static void sofa(List<Placed> out, JsonArray cells, int y) {
      Map<SofaLayout.Cell, Integer> facing = new HashMap();
      Map<SofaLayout.Cell, Direction> faces = new HashMap();

      for(JsonElement element : cells) {
         JsonArray cell = element.getAsJsonArray();
         SofaLayout.Cell at = new SofaLayout.Cell(cell.get(0).getAsInt(), cell.get(1).getAsInt());
         Direction face = Direction.byName(cell.get(2).getAsString());
         faces.put(at, face);
         facing.put(at, CasinoSofaBlock.cardinal(face));
      }

      Block block = block("casinodeco:casino_sofa");

      for(Map.Entry<SofaLayout.Cell, SofaLayout.Layout> entry : SofaLayout.calculate(facing).entrySet()) {
         out.add(new Placed((BlockState)((BlockState)((BlockState)block.defaultBlockState().setValue(CasinoSofaBlock.FACING, (Direction)faces.get(entry.getKey()))).setValue(CasinoSofaBlock.JOINS, ((SofaLayout.Layout)entry.getValue()).joins())).setValue(CasinoSofaBlock.BACK, SofaBack.fromMask(((SofaLayout.Layout)entry.getValue()).backs())), ((SofaLayout.Cell)entry.getKey()).x(), y, ((SofaLayout.Cell)entry.getKey()).z()));
      }

   }

   private static void add(List<Placed> out, BlockState state, BlockPos offset, int x, int y, int z) {
      out.add(new Placed(state, x + offset.getX(), y + offset.getY(), z + offset.getZ()));
   }

   static BlockState state(String text) {
      int bracket = text.indexOf(91);
      BlockState state = block(bracket < 0 ? text : text.substring(0, bracket)).defaultBlockState();
      if (bracket < 0) {
         return state;
      } else {
         for(String pair : text.substring(bracket + 1, text.length() - 1).split(",")) {
            int equals = pair.indexOf(61);
            if (equals > 0) {
               state = with(state, pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
            }
         }

         return state;
      }
   }

   static Block block(String id) {
      return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
   }

   static BlockState with(BlockState state, String name, String value) {
      Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
      return property == null ? state : with(state, property, value);
   }

   private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
      return (BlockState)property.getValue(value).map((v) -> (BlockState)state.setValue(property, v)).orElse(state);
   }

   public static record Placed(BlockState state, int x, int y, int z) {
   }

   public static record Machine(Placed placed, BlockEntity entity) {
   }
}
