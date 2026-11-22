package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PlinkoBlockEntity extends BlockEntity implements MenuProvider, CasinoConfiguredMachine, CasinoPayoutMachine {
   public static final int SLOTS = 3;
   private static final int PANEL_AFTER_RESULT = 70;
   private static final double FRONT_Z = (double)14.5F;
   private static final double MOUTH_X = (double)24.0F;
   private static final double MOUTH_Y = (double)6.5F;
   private static final double MOUTH_Z = (double)14.5F;
   private static final double BOARD_X = (double)24.0F;
   private static final double BOARD_Y = (double)26.0F;
   private static final double CREST_Y = (double)47.0F;
   private static final DustParticleOptions GOLD_CONFETTI = new DustParticleOptions(16765288, 1.2F);
   private static final DustParticleOptions RED_CONFETTI = new DustParticleOptions(14629707, 1.1F);
   private static final DustParticleOptions GREEN_CONFETTI = new DustParticleOptions(3594374, 1.1F);
   private static final DustParticleOptions LAMP_SPARK = new DustParticleOptions(16773296, 0.55F);
   private static final ItemParticleOption COIN;
   private final SimpleContainer bets = new SimpleContainer(3) {
      {
         Objects.requireNonNull(PlinkoBlockEntity.this);
      }

      public void setChanged() {
         super.setChanged();
         PlinkoBlockEntity.this.setChanged();
      }
   };
   private final ItemStack[] displayedBets = new ItemStack[3];
   private final List<ItemStack>[] payouts = new List[3];
   private boolean locked;
   private boolean broken;
   private long start = -1L;
   private long seed;
   private int balls;
   private final int[] paths = new int[3];
   private final int[] slotOf = new int[3];
   private int stage;
   private int displayTicks;
   private int cueIndex = -1;
   private PlinkoDrop.Plan plan;
   private CasinoMachineIdentity machineIdentity;
   private CasinoPayoutTarget payoutTarget;
   private CasinoPlayerDisplay panelPlayer;
   private CroupierSettings croupierSettings;

   public PlinkoBlockEntity(BlockPos pos, BlockState state) {
      super(RouletteMod.PLINKO_ENTITY, pos, state);
      this.machineIdentity = CasinoMachineIdentity.EMPTY;
      this.payoutTarget = CasinoPayoutTarget.EMPTY;
      this.panelPlayer = CasinoPlayerDisplay.EMPTY;
      this.croupierSettings = CroupierSettings.EMPTY;
      Arrays.fill(this.displayedBets, ItemStack.EMPTY);

      for(int i = 0; i < 3; ++i) {
         this.payouts[i] = new ArrayList();
      }

   }

   public boolean isPlaying() {
      return this.locked;
   }

   public SimpleContainer bets() {
      return this.bets;
   }

   public ItemStack displayedBet(int slot) {
      return this.displayedBets[slot];
   }

   public long start() {
      return this.start;
   }

   public int ballCount() {
      return this.balls;
   }

   public int slotOf(int ball) {
      return this.slotOf[ball];
   }

   public PlinkoDrop.Plan plan() {
      if (this.start >= 0L && this.balls >= 1) {
         if (this.plan == null || this.plan.seed() != this.seed || !Arrays.equals(this.plan.paths(), Arrays.copyOf(this.paths, this.balls))) {
            this.plan = PlinkoDrop.plan(Arrays.copyOf(this.paths, this.balls), this.seed);
         }

         return this.plan;
      } else {
         return null;
      }
   }

   public PlinkoGame.Kind ballKind(int ball) {
      PlinkoDrop.Plan p = this.plan();
      if (p != null && ball < this.balls) {
         int quarters = PlinkoGame.quarters(p.ball(ball).pocket());
         return PlinkoGame.kindFor(quarters, this.paid(ball, quarters));
      } else {
         return PlinkoGame.Kind.NONE;
      }
   }

   private int paid(int ball, int quarters) {
      return PlinkoGame.payout(this.displayedBets[this.slotOf[ball]].getCount(), quarters, FairPayout.roll(this.seed, this.slotOf[ball], 4));
   }

   public PlinkoGame.Kind resultKind() {
      PlinkoGame.Kind best = PlinkoGame.Kind.NONE;

      for(int b = 0; b < this.balls; ++b) {
         if (this.ballKind(b).ordinal() > best.ordinal()) {
            best = this.ballKind(b);
         }
      }

      return best;
   }

   public CasinoPayoutTarget payoutTarget() {
      return this.payoutTarget;
   }

   public void payoutTarget(CasinoPayoutTarget target) {
      this.payoutTarget = target;
      this.setChanged();
   }

   public CasinoPlayerDisplay panelPlayer() {
      for(ItemStack stake : this.displayedBets) {
         if (!stake.isEmpty()) {
            return this.panelPlayer;
         }
      }

      return CasinoPlayerDisplay.EMPTY;
   }

   public CasinoMachineIdentity machineIdentity() {
      return this.machineIdentity;
   }

   public void machineIdentity(CasinoMachineIdentity value) {
      this.machineIdentity = value;
   }

   public CroupierSettings croupierSettings() {
      return this.croupierSettings;
   }

   public void croupierSettings(CroupierSettings value) {
      this.croupierSettings = value;
   }

   public Component getDisplayName() {
      return Component.translatable("block.roulettecasino.plinko");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      if (player instanceof ServerPlayer server) {
         CasinoSettings.menuOpened(server, id, this);
      }

      return this.locked ? null : new PlinkoMenu(id, inventory, this, this.bets);
   }

   public void start(ServerPlayer initiator) {
      this.start(initiator, (int[])null);
   }

   public void start(ServerPlayer initiator, int[] forced) {
      if (!this.locked) {
         Level var4 = this.level;
         if (var4 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var4;
            if (!CasinoWagerRouting.mayUse(this, this.bets, initiator)) {
               return;
            }

            CasinoSettings.returnRejected(this, this.bets, initiator);
            if (this.bets.isEmpty()) {
               if (initiator != null) {
                  initiator.sendOverlayMessage(Component.translatable("message.roulettecasino.plinko.no_bet"));
               }

               return;
            }

            int chance = CasinoSettings.chance(this);
            double luck = CasinoSettings.playerLuck(initiator);
            this.seed = server.getRandom().nextLong();
            boolean loggedLuck = false;

            for(List<ItemStack> list : this.payouts) {
               list.clear();
            }

            this.balls = 0;

            for(int slot = 0; slot < 3; ++slot) {
               ItemStack stake = this.bets.removeItemNoUpdate(slot);
               this.displayedBets[slot] = stake.copy();
               if (!stake.isEmpty()) {
                  int b = this.balls++;
                  this.slotOf[b] = slot;
                  int[] var10000 = this.paths;
                  int var10002;
                  if (forced != null && b < forced.length) {
                     var10002 = Math.floorMod(forced[b], 256);
                  } else if (chance >= 0 || luck != 0.0D) {
                     RandomSource random = server.getRandom();
                     var10002 = CasinoOdds.plinko(chance, luck, random::nextDouble, random::nextInt);
                     if (!loggedLuck) {
                        loggedLuck = true;
                        me.tuanzi.util.ModLog.debug(initiator, "【赌场-弹珠台】配置胜率=%d, 玩家幸运=%.2f, 幸运胜率修正=%.2f%%",
                                chance, luck, luck * 0.5D);
                     }
                  } else {
                     var10002 = server.getRandom().nextInt(256);
                  }

                  var10000[b] = var10002;
                  int quarters = PlinkoGame.quarters(PlinkoGame.pocketOf(this.paths[b]));
                  this.payouts[b].addAll(CasinoExchange.payout(stake, quarters, 4, FairPayout.roll(this.seed, this.slotOf[b], 4), CasinoSettings.exchange(this, stake), false));
               }
            }

            this.plan = null;
            this.start = server.getGameTime();
            this.stage = 0;
            this.cueIndex = 0;
            this.displayTicks = 0;
            this.panelPlayer = CasinoPlayerDisplay.of(initiator);
            if (initiator != null) {
               this.payoutTarget = CasinoPayoutTarget.of(initiator);
            }

            this.locked = true;
            this.closeMenus(server);
            this.setChanged();
            this.sync(server);
            return;
         }
      }

   }

   private void closeMenus(ServerLevel server) {
      for(ServerPlayer player : server.players()) {
         AbstractContainerMenu var5 = player.containerMenu;
         if (var5 instanceof PlinkoMenu menu) {
            if (menu.machine() == this) {
               player.closeContainer();
            }
         }
      }

   }

   public static void tick(Level world, BlockPos pos, PlinkoBlockEntity machine) {
      if (world instanceof ServerLevel server) {
         CasinoSettings.identify(machine);
         if (machine.displayTicks > 0 && --machine.displayTicks == 0) {
            Arrays.fill(machine.displayedBets, ItemStack.EMPTY);
            machine.setChanged();
            machine.sync(server);
         }

         if (machine.locked) {
            PlinkoDrop.Plan plan = machine.plan();
            if (plan == null) {
               machine.locked = false;
               machine.setChanged();
            } else {
               long elapsed = server.getGameTime() - machine.start;
               if (elapsed < 0L) {
                  machine.start = server.getGameTime();
                  elapsed = 0L;
                  machine.setChanged();
                  machine.sync(server);
               }

               machine.cues(server, plan, elapsed);

               for(int b = 0; b < machine.balls; ++b) {
                  if ((machine.stage & 1 << b) == 0 && (double)elapsed >= plan.ball(b).landTick()) {
                     machine.stage |= 1 << b;
                     if ((double)elapsed - plan.ball(b).landTick() <= (double)10.0F) {
                        machine.landed(server, plan, b);
                     }

                     machine.setChanged();
                  }
               }

               if ((machine.stage & 256) == 0 && elapsed >= (long)plan.resultTick()) {
                  machine.stage |= 256;
                  machine.displayTicks = 70;
                  if (elapsed - (long)plan.resultTick() <= 10L) {
                     machine.announce(server);
                  }

                  machine.setChanged();
               }

               if ((machine.stage & 256) != 0) {
                  machine.afterburst(server, elapsed - (long)plan.resultTick());
               }

               boolean owed = false;

               for(int b = 0; b < machine.balls; ++b) {
                  if (!machine.payouts[b].isEmpty()) {
                     owed = true;
                     if (!((double)elapsed < plan.ball(b).landTick() + (double)10.0F)) {
                        int interval = machine.payouts[b].size() > 48 ? 1 : (machine.payouts[b].size() > 16 ? 2 : 3);
                        if ((server.getGameTime() + (long)b) % (long)interval == 0L) {
                           machine.eject(server, (ItemStack)machine.payouts[b].removeFirst());
                           double[] mouth = machine.world((double)24.0F, (double)6.5F, (double)14.5F);
                           if (interval > 1 || server.getGameTime() % 2L == 0L) {
                              server.playSound((Entity)null, mouth[0], mouth[1], mouth[2], RouletteMod.PLINKO_PAYOUT, SoundSource.BLOCKS, 0.5F, 0.9F + server.getRandom().nextFloat() * 0.25F);
                           }

                           machine.setChanged();
                        }
                     }
                  }
               }

               if (!owed && (machine.stage & 256) != 0 && elapsed >= (long)plan.readyTick()) {
                  machine.locked = false;
                  machine.payoutTarget = CasinoPayoutTarget.EMPTY;
                  machine.setChanged();
               }

            }
         }
      }
   }

   private double[] world(double x, double y, double z) {
      int turns = ((Direction)this.getBlockState().getValue(PlinkoBlock.FACING)).get2DDataValue();
      double[] at = SlotMachineLayout.toBlock(x - (double)16.0F, y - (double)16.0F, z, turns);
      return new double[]{(double)this.worldPosition.getX() + at[0], (double)this.worldPosition.getY() + at[1], (double)this.worldPosition.getZ() + at[2]};
   }

   private void sound(ServerLevel server, SoundEvent event, double x, double y, double z, float volume, float pitch) {
      double[] at = this.world(x, y, z);
      server.playSound((Entity)null, at[0], at[1], at[2], event, SoundSource.BLOCKS, volume, pitch);
   }

   private void cues(ServerLevel server, PlinkoDrop.Plan plan, long elapsed) {
      List<PlinkoDrop.Cue> cues = plan.cues();
      if (this.cueIndex < 0) {
         for(this.cueIndex = 0; this.cueIndex < cues.size() && (long)((PlinkoDrop.Cue)cues.get(this.cueIndex)).tick() < elapsed - 1L; ++this.cueIndex) {
         }
      }

      while(this.cueIndex < cues.size() && (long)((PlinkoDrop.Cue)cues.get(this.cueIndex)).tick() <= elapsed) {
         PlinkoDrop.Cue cue = (PlinkoDrop.Cue)cues.get(this.cueIndex++);
         switch (cue.sound()) {
            case DROP:
               this.sound(server, RouletteMod.PLINKO_DROP, (double)24.0F, (double)38.5F, (double)14.5F, cue.volume(), cue.pitch());
               break;
            case PEG:
               int pin = plan.ball(cue.ball()).hitPin(cue.index());
               double x = PlinkoLights.pinX(pin);
               double y = PlinkoLights.pinY(pin);
               this.sound(server, RouletteMod.PLINKO_PEG, x, y, (double)14.5F, cue.volume(), cue.pitch());
               double[] at = this.world(x, y, 14.3);
               server.sendParticles(LAMP_SPARK, at[0], at[1], at[2], 2, 0.02, 0.02, 0.01, (double)0.0F);
               break;
            case POCKET:
               this.sound(server, RouletteMod.PLINKO_POCKET, PlinkoGame.pocketX(cue.index()), (double)14.5F, (double)14.5F, cue.volume(), cue.pitch());
         }
      }

   }

   private void landed(ServerLevel server, PlinkoDrop.Plan plan, int ball) {
      double x = PlinkoGame.pocketX(plan.ball(ball).pocket());
      double[] at = this.world(x, (double)15.0F, (double)15.0F);
      switch (this.ballKind(ball)) {
         case JACKPOT:
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at[0], at[1], at[2], 30, 0.12, (double)0.25F, 0.08, 0.35);
            server.sendParticles(GOLD_CONFETTI, at[0], at[1], at[2], 30, 0.2, 0.3, 0.1, 0.12);
            server.sendParticles(ParticleTypes.FIREWORK, at[0], at[1], at[2], 18, 0.1, 0.2, 0.1, 0.18);
            break;
         case BIG:
            server.sendParticles(GOLD_CONFETTI, at[0], at[1], at[2], 16, 0.15, (double)0.25F, 0.08, 0.1);
            server.sendParticles(GREEN_CONFETTI, at[0], at[1], at[2], 12, 0.15, (double)0.25F, 0.08, 0.1);
            server.sendParticles(ParticleTypes.FIREWORK, at[0], at[1], at[2], 10, 0.1, 0.15, 0.08, 0.14);
            break;
         case WIN:
            server.sendParticles(GOLD_CONFETTI, at[0], at[1], at[2], 10, 0.12, 0.2, 0.08, 0.08);
            server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 6, 0.1, 0.15, 0.05, 0.04);
            break;
         case PARTIAL:
            server.sendParticles(COIN, at[0], at[1], at[2], 5, 0.08, 0.05, 0.05, 0.08);
            break;
         default:
            server.sendParticles(ParticleTypes.SMOKE, at[0], at[1], at[2], 5, 0.08, 0.05, 0.05, 0.01);
      }

   }

   private void announce(ServerLevel server) {
      switch (this.resultKind()) {
         case JACKPOT:
            this.sound(server, RouletteMod.PLINKO_JACKPOT, (double)24.0F, (double)26.0F, (double)14.5F, 1.5F, 1.0F);
            this.burst(server, (double)1.0F, true);
            break;
         case BIG:
            this.sound(server, RouletteMod.PLINKO_WIN, (double)24.0F, (double)26.0F, (double)14.5F, 1.25F, 1.0F);
            this.burst(server, (double)0.75F, false);
            break;
         case WIN:
            this.sound(server, RouletteMod.PLINKO_WIN, (double)24.0F, (double)26.0F, (double)14.5F, 1.05F, 1.0F);
            this.burst(server, (double)0.5F, false);
            break;
         case PARTIAL:
            this.sound(server, RouletteMod.PLINKO_PARTIAL, (double)24.0F, (double)26.0F, (double)14.5F, 0.9F, 1.0F);
            double[] mouth = this.world((double)24.0F, (double)8.0F, (double)15.0F);
            server.sendParticles(COIN, mouth[0], mouth[1], mouth[2], 6, 0.2, 0.08, 0.1, 0.12);
            break;
         default:
            this.sound(server, RouletteMod.PLINKO_LOSE, (double)24.0F, (double)26.0F, (double)14.5F, 0.95F, 1.0F);
            double[] crest = this.world((double)24.0F, (double)47.0F, (double)12.0F);
            server.sendParticles(ParticleTypes.SMOKE, crest[0], crest[1], crest[2], 8, 0.4, 0.05, 0.1, 0.01);
      }

   }

   private void afterburst(ServerLevel server, long since) {
      switch (this.resultKind()) {
         case JACKPOT:
            if (since >= 5L && since <= (long)PlinkoDrop.LAST_BURST[PlinkoGame.Kind.JACKPOT.ordinal()] && since % 5L == 0L) {
               this.burst(server, 0.8 - 0.1 * (double)(since / 5L - 1L), false);
            }
            break;
         case BIG:
            if (since == 7L || since == (long)PlinkoDrop.LAST_BURST[PlinkoGame.Kind.BIG.ordinal()]) {
               this.burst(server, 0.4, false);
            }
            break;
         case WIN:
            if (since == (long)PlinkoDrop.LAST_BURST[PlinkoGame.Kind.WIN.ordinal()]) {
               this.burst(server, 0.3, false);
            }
      }

   }

   private void burst(ServerLevel server, double strength, boolean jackpot) {
      double[] mouth = this.world((double)24.0F, (double)8.0F, (double)15.5F);
      server.sendParticles(COIN, mouth[0], mouth[1], mouth[2], (int)Math.round((double)24.0F * strength), 0.4, 0.1, 0.12, 0.2);
      double[] board = this.world((double)24.0F, (double)26.0F, (double)15.5F);
      int n = (int)Math.round((double)24.0F * strength);
      server.sendParticles(GOLD_CONFETTI, board[0], board[1], board[2], n, 0.9, 0.7, (double)0.25F, 0.16);
      server.sendParticles(RED_CONFETTI, board[0], board[1], board[2], n, 0.9, 0.7, (double)0.25F, 0.16);
      server.sendParticles(GREEN_CONFETTI, board[0], board[1], board[2], n, 0.9, 0.7, (double)0.25F, 0.16);
      double[] crest = this.world((double)24.0F, (double)47.5F, (double)12.0F);
      server.sendParticles(ParticleTypes.FIREWORK, crest[0], crest[1], crest[2], (int)Math.round((double)20.0F * strength), 0.6, 0.2, (double)0.25F, 0.22);
      server.sendParticles(ParticleTypes.END_ROD, crest[0], crest[1], crest[2], (int)Math.round((double)12.0F * strength), 0.6, 0.2, (double)0.25F, 0.16);
      if (strength >= 0.7) {
         for(int i = 0; i < 11; ++i) {
            double[] bulb = this.world(PlinkoRenderGeometry.BULBS[2 * i], PlinkoRenderGeometry.BULBS[2 * i + 1], (double)12.5F);
            server.sendParticles(ParticleTypes.FIREWORK, bulb[0], bulb[1], bulb[2], 1, 0.04, 0.04, 0.04, 0.1);
         }
      }

      if (jackpot) {
         server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, board[0], board[1], board[2], 40, 0.8, 0.7, 0.3, 0.35);
         server.sendParticles(COIN, crest[0], crest[1] - 0.3, crest[2], 30, (double)0.5F, 0.1, 0.3, 0.28);
      }

   }

   private void eject(ServerLevel server, ItemStack stack) {
      int turns = ((Direction)this.getBlockState().getValue(PlinkoBlock.FACING)).get2DDataValue();
      double[] at = this.world((double)24.0F, (double)7.0F, (double)15.0F);
      int[] step = SlotMachineLayout.facingStep(turns);
      RandomSource random = server.getRandom();
      double side = (random.nextDouble() - (double)0.5F) * 0.2;
      ItemEntity item = new ItemEntity(server, at[0], at[1], at[2], stack);
      item.setDeltaMovement((double)step[0] * 0.18 + (double)step[1] * side, 0.22, (double)step[1] * 0.18 + (double)step[0] * side);
      this.payoutTarget.aim(server, item);
      server.addFreshEntity(item);
   }

   public void onBroken() {
      if (!this.broken) {
         this.broken = true;
         Level var2 = this.level;
         if (var2 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var2;
            this.closeMenus(server);

            for(List<ItemStack> list : this.payouts) {
               for(ItemStack stack : list) {
                  this.eject(server, stack);
               }

               list.clear();
            }

            Arrays.fill(this.displayedBets, ItemStack.EMPTY);
            this.displayTicks = 0;

            for(int i = 0; i < 3; ++i) {
               ItemStack stake = this.bets.removeItemNoUpdate(i);
               if (!stake.isEmpty()) {
                  this.eject(server, stake);
               }
            }

            this.locked = false;
            this.setChanged();
         }
      }
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      this.onBroken();
      CasinoSettings.forget(this);
      super.preRemoveSideEffects(pos, state);
   }

   public void sync(ServerLevel server) {
      server.getChunkSource().blockChanged(this.worldPosition);
   }

   private void readGame(ValueInput input) {
      this.start = input.getLongOr("Start", -1L);
      this.seed = input.getLongOr("Seed", 0L);
      this.balls = Math.max(0, Math.min(3, input.getIntOr("Balls", 0)));

      for(int b = 0; b < 3; ++b) {
         this.paths[b] = Math.floorMod(input.getIntOr("Path" + b, 0), 256);
         this.slotOf[b] = Math.floorMod(input.getIntOr("Slot" + b, b), 3);
      }

      this.plan = null;

      for(int i = 0; i < 3; ++i) {
         this.displayedBets[i] = (ItemStack)input.read("DisplayBet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
      }

   }

   private void writeGame(ValueOutput output) {
      output.putLong("Start", this.start);
      output.putLong("Seed", this.seed);
      output.putInt("Balls", this.balls);

      for(int b = 0; b < 3; ++b) {
         output.putInt("Path" + b, this.paths[b]);
         output.putInt("Slot" + b, this.slotOf[b]);
      }

      for(int i = 0; i < 3; ++i) {
         if (!this.displayedBets[i].isEmpty()) {
            output.store("DisplayBet" + i, ItemStack.CODEC, this.displayedBets[i]);
         }
      }

   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.machineIdentity = (CasinoMachineIdentity)input.read("MachineIdentity", CasinoMachineIdentity.CODEC).orElse(CasinoMachineIdentity.EMPTY);
      this.panelPlayer = (CasinoPlayerDisplay)input.read("PanelPlayer", CasinoPlayerDisplay.CODEC).orElse(CasinoPlayerDisplay.EMPTY);
      this.payoutTarget = (CasinoPayoutTarget)input.read("PayoutTarget", CasinoPayoutTarget.CODEC).orElse(CasinoPayoutTarget.EMPTY);
      this.croupierSettings = (CroupierSettings)input.read("Croupier", CroupierSettings.CODEC).orElse(CroupierSettings.EMPTY);
      this.readGame(input);
      this.locked = input.getBooleanOr("Locked", false);
      this.stage = input.getIntOr("Stage", 0);
      this.displayTicks = input.getIntOr("DisplayTicks", 0);
      this.cueIndex = -1;

      for(int b = 0; b < 3; ++b) {
         this.payouts[b].clear();

         for(ItemStack stack : input.listOrEmpty("Payouts" + b, ItemStack.CODEC)) {
            this.payouts[b].add(stack);
         }
      }

      for(int i = 0; i < 3; ++i) {
         this.bets.setItem(i, (ItemStack)input.read("Bet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
      }

   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      if (this.machineIdentity.number() > 0) {
         output.store("MachineIdentity", CasinoMachineIdentity.CODEC, this.machineIdentity);
      }

      if (!this.panelPlayer.id().isEmpty()) {
         output.store("PanelPlayer", CasinoPlayerDisplay.CODEC, this.panelPlayer);
      }

      if (!this.payoutTarget.owner().isEmpty()) {
         output.store("PayoutTarget", CasinoPayoutTarget.CODEC, this.payoutTarget);
      }

      if (!this.croupierSettings.equals(CroupierSettings.EMPTY)) {
         output.store("Croupier", CroupierSettings.CODEC, this.croupierSettings);
      }

      this.writeGame(output);
      output.putBoolean("Locked", this.locked);
      output.putInt("Stage", this.stage);
      output.putInt("DisplayTicks", this.displayTicks);

      for(int b = 0; b < 3; ++b) {
         ValueOutput.TypedOutputList<ItemStack> list = output.<ItemStack>list("Payouts" + b, ItemStack.CODEC);

         for(ItemStack stack : this.payouts[b]) {
            list.add(stack);
         }
      }

      for(int i = 0; i < 3; ++i) {
         ItemStack bet = this.bets.getItem(i);
         if (!bet.isEmpty()) {
            output.store("Bet" + i, ItemStack.CODEC, bet);
         }
      }

   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
      this.writeGame(output);
      if (!this.croupierSettings.equals(CroupierSettings.EMPTY)) {
         output.store("Croupier", CroupierSettings.CODEC, this.croupierSettings.publicView());
      }

      if (this.machineIdentity.number() > 0) {
         output.store("MachineIdentity", CasinoMachineIdentity.CODEC, this.machineIdentity);
      }

      if (!this.panelPlayer.id().isEmpty()) {
         output.store("PanelPlayer", CasinoPlayerDisplay.CODEC, this.panelPlayer);
      }

      return output.buildResult();
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   static {
      COIN = new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET);
   }
}
