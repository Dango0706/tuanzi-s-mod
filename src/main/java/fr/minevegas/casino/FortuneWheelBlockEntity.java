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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FortuneWheelBlockEntity extends BlockEntity implements MenuProvider, CasinoConfiguredMachine, CasinoPayoutMachine {
   public static final int SLOTS = 3;
   private static final int PANEL_AFTER_RESULT = 70;
   private static final double AXLE_X;
   private static final double AXLE_Y;
   private static final double FRONT_Z = (double)14.0F;
   private static final double POINTER_Y = 26.6;
   private static final double CAP_Y = (double)31.5F;
   private static final double MOUTH_X = (double)8.0F;
   private static final double MOUTH_Y = (double)-12.0F;
   private static final double MOUTH_Z = (double)14.5F;
   private static final double RIM_RADIUS = (double)21.0F;
   private static final DustParticleOptions GOLD_CONFETTI;
   private static final DustParticleOptions RED_CONFETTI;
   private static final DustParticleOptions GREEN_CONFETTI;
   private static final ItemParticleOption COIN;
   private final SimpleContainer bets = new SimpleContainer(3) {
      {
         Objects.requireNonNull(FortuneWheelBlockEntity.this);
      }

      public void setChanged() {
         super.setChanged();
         FortuneWheelBlockEntity.this.setChanged();
      }
   };
   private final ItemStack[] displayedBets = new ItemStack[3];
   private final List<ItemStack> payouts = new ArrayList();
   private boolean locked;
   private boolean broken;
   private long spinStart = -1L;
   private int from;
   private int stop;
   private long seed;
   private int stage;
   private int displayTicks;
   private int cueIndex = -1;
   private FortuneWheelSpin.Plan plan;
   private final ContainerData restData = new ContainerData() {
      {
         Objects.requireNonNull(FortuneWheelBlockEntity.this);
      }

      public int get(int index) {
         return FortuneWheelBlockEntity.this.stop;
      }

      public void set(int index, int value) {
      }

      public int getCount() {
         return 1;
      }
   };
   private CasinoMachineIdentity machineIdentity;
   private CasinoPayoutTarget payoutTarget;
   private CasinoPlayerDisplay panelPlayer;
   private CroupierSettings croupierSettings;

   public FortuneWheelBlockEntity(BlockPos pos, BlockState state) {
      super(RouletteMod.WHEEL_ENTITY, pos, state);
      this.machineIdentity = CasinoMachineIdentity.EMPTY;
      this.payoutTarget = CasinoPayoutTarget.EMPTY;
      this.panelPlayer = CasinoPlayerDisplay.EMPTY;
      this.croupierSettings = CroupierSettings.EMPTY;
      Arrays.fill(this.displayedBets, ItemStack.EMPTY);
   }

   public FortuneWheelGame.Kind resultKind() {
      int quarters = FortuneWheelGame.quarters(this.stop);
      FortuneWheelGame.Kind kind = FortuneWheelGame.kindOf(quarters);
      if (kind != FortuneWheelGame.Kind.PARTIAL) {
         return kind;
      } else {
         for(int slot = 0; slot < 3; ++slot) {
            if (this.paid(slot, quarters) > 0) {
               return kind;
            }
         }

         return FortuneWheelGame.Kind.NONE;
      }
   }

   private int paid(int slot, int quarters) {
      return FortuneWheelGame.payout(this.displayedBets[slot].getCount(), quarters, FairPayout.roll(this.seed, slot, 4));
   }

   public boolean isSpinning() {
      return this.locked;
   }

   public SimpleContainer bets() {
      return this.bets;
   }

   public ItemStack displayedBet(int slot) {
      return this.displayedBets[slot];
   }

   public long spinStart() {
      return this.spinStart;
   }

   public int stop() {
      return this.stop;
   }

   public int from() {
      return this.from;
   }

   public FortuneWheelSpin.Plan plan() {
      if (this.spinStart < 0L) {
         return null;
      } else {
         if (this.plan == null || this.plan.seed() != this.seed || this.plan.stop() != this.stop || this.plan.from() != this.from) {
            this.plan = FortuneWheelSpin.plan(this.from, this.stop, this.seed);
         }

         return this.plan;
      }
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
      return Component.translatable("block.roulettecasino.fortune_wheel");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      if (player instanceof ServerPlayer server) {
         CasinoSettings.menuOpened(server, id, this);
      }

      return this.locked ? null : new FortuneWheelMenu(id, inventory, this, this.bets, this.restData);
   }

   public void start(ServerPlayer initiator) {
      this.start(initiator, -1);
   }

   public void start(ServerPlayer initiator, int forced) {
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
                  initiator.sendOverlayMessage(Component.translatable("message.roulettecasino.fortune_wheel.no_bet"));
               }

               return;
            }

            int chance = CasinoSettings.chance(this);
            int var10000;
            if (forced >= 0) {
               var10000 = Math.floorMod(forced, 16);
            } else if (chance >= 0) {
               RandomSource var10001 = server.getRandom();
               Objects.requireNonNull(var10001);
               var10000 = CasinoOdds.wheel(chance, var10001::nextInt);
            } else {
               var10000 = FortuneWheelGame.segmentForRoll(server.getRandom().nextInt(64));
            }

            int next = var10000;
            this.from = this.stop;
            this.stop = next;
            this.seed = server.getRandom().nextLong();
            this.plan = null;
            this.spinStart = server.getGameTime();
            this.stage = 0;
            this.cueIndex = 0;
            this.displayTicks = 0;
            this.panelPlayer = CasinoPlayerDisplay.of(initiator);
            if (initiator != null) {
               this.payoutTarget = CasinoPayoutTarget.of(initiator);
            }

            this.locked = true;
            this.payouts.clear();
            int quarters = FortuneWheelGame.quarters(this.stop);

            for(int i = 0; i < 3; ++i) {
               ItemStack stake = this.bets.removeItemNoUpdate(i);
               this.displayedBets[i] = stake.copy();
               if (!stake.isEmpty()) {
                  this.payouts.addAll(CasinoExchange.payout(stake, quarters, 4, FairPayout.roll(this.seed, i, 4), CasinoSettings.exchange(this, stake), false));
               }
            }

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
         if (var5 instanceof FortuneWheelMenu menu) {
            if (menu.wheel() == this) {
               player.closeContainer();
            }
         }
      }

   }

   public static void tick(Level world, BlockPos pos, FortuneWheelBlockEntity wheel) {
      if (world instanceof ServerLevel server) {
         CasinoSettings.identify(wheel);
         if (wheel.displayTicks > 0 && --wheel.displayTicks == 0) {
            Arrays.fill(wheel.displayedBets, ItemStack.EMPTY);
            wheel.setChanged();
            wheel.sync(server);
         }

         if (wheel.locked) {
            FortuneWheelSpin.Plan plan = wheel.plan();
            if (plan == null) {
               wheel.locked = false;
               wheel.setChanged();
            } else {
               long elapsed = server.getGameTime() - wheel.spinStart;
               if (elapsed < 0L) {
                  wheel.spinStart = server.getGameTime();
                  elapsed = 0L;
                  wheel.setChanged();
                  wheel.sync(server);
               }

               wheel.cues(server, plan, elapsed);
               wheel.sparks(server, plan, elapsed);
               if (wheel.stage < 1 && elapsed >= (long)plan.resultTick()) {
                  wheel.stage = 1;
                  wheel.displayTicks = 70;
                  if (elapsed - (long)plan.resultTick() <= 10L) {
                     wheel.announce(server, plan);
                  }

                  wheel.setChanged();
               }

               if (wheel.stage >= 1) {
                  wheel.afterburst(server, plan, elapsed - (long)plan.resultTick());
               }

               if (wheel.stage >= 1 && elapsed >= (long)(plan.resultTick() + 12) && !wheel.payouts.isEmpty()) {
                  int interval = wheel.payouts.size() > 48 ? 1 : (wheel.payouts.size() > 16 ? 2 : 3);
                  if (server.getGameTime() % (long)interval == 0L) {
                     wheel.eject(server, (ItemStack)wheel.payouts.removeFirst());
                     double[] mouth = wheel.world((double)8.0F, (double)-12.0F, (double)14.5F);
                     if (interval > 1 || server.getGameTime() % 2L == 0L) {
                        server.playSound((Entity)null, mouth[0], mouth[1], mouth[2], RouletteMod.WHEEL_PAYOUT, SoundSource.BLOCKS, 0.5F, 0.9F + server.getRandom().nextFloat() * 0.25F);
                     }

                     wheel.setChanged();
                  }
               } else if (wheel.stage >= 1 && wheel.payouts.isEmpty() && elapsed >= (long)plan.readyTick()) {
                  wheel.locked = false;
                  wheel.payoutTarget = CasinoPayoutTarget.EMPTY;
                  wheel.setChanged();
               }

            }
         }
      }
   }

   private double[] world(double x, double y, double z) {
      int turns = ((Direction)this.getBlockState().getValue(FortuneWheelBlock.FACING)).get2DDataValue();
      double[] at = SlotMachineLayout.toBlock(x, y, z, turns);
      return new double[]{(double)this.worldPosition.getX() + at[0], (double)this.worldPosition.getY() + at[1], (double)this.worldPosition.getZ() + at[2]};
   }

   private void sound(ServerLevel server, SoundEvent event, double x, double y, double z, float volume, float pitch) {
      double[] at = this.world(x, y, z);
      server.playSound((Entity)null, at[0], at[1], at[2], event, SoundSource.BLOCKS, volume, pitch);
   }

   private void cues(ServerLevel server, FortuneWheelSpin.Plan plan, long elapsed) {
      List<FortuneWheelSpin.Cue> cues = plan.cues();
      if (this.cueIndex < 0) {
         for(this.cueIndex = 0; this.cueIndex < cues.size() && (long)((FortuneWheelSpin.Cue)cues.get(this.cueIndex)).tick() < elapsed - 1L; ++this.cueIndex) {
         }
      }

      while(this.cueIndex < cues.size() && (long)((FortuneWheelSpin.Cue)cues.get(this.cueIndex)).tick() <= elapsed) {
         FortuneWheelSpin.Cue cue = (FortuneWheelSpin.Cue)cues.get(this.cueIndex++);
         switch (cue.sound()) {
            case SPIN:
               this.sound(server, RouletteMod.WHEEL_SPIN, AXLE_X, AXLE_Y, (double)14.0F, cue.volume(), cue.pitch());
               break;
            case WHIRR:
               this.sound(server, RouletteMod.WHEEL_WHIRR, AXLE_X, AXLE_Y, (double)14.0F, cue.volume(), cue.pitch());
               break;
            case CLICK:
               this.sound(server, RouletteMod.WHEEL_CLICK, AXLE_X, 26.6, (double)14.0F, cue.volume(), cue.pitch());
               break;
            case STOP:
               this.sound(server, RouletteMod.WHEEL_STOP, AXLE_X, 26.6, (double)14.0F, cue.volume(), cue.pitch());
               double[] at = this.world(AXLE_X, 25.6, (double)15.0F);
               server.sendParticles(ParticleTypes.CRIT, at[0], at[1], at[2], 6, 0.12, 0.1, 0.04, 0.12);
         }
      }

   }

   private void sparks(ServerLevel server, FortuneWheelSpin.Plan plan, long elapsed) {
      double speed = plan.speed((double)elapsed);
      if (speed >= 0.7 && elapsed % 4L == 0L) {
         double a = Math.toRadians((double)(elapsed * 53L % 360L));
         double[] at = this.world(AXLE_X + (double)21.0F * Math.sin(a), AXLE_Y + (double)21.0F * Math.cos(a), (double)13.0F);
         server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at[0], at[1], at[2], 2, 0.05, 0.05, 0.05, 0.12);
      } else if (speed > 0.02 && speed < 0.16 && elapsed % 4L == 0L) {
         double[] at = this.world(AXLE_X, 25.6, (double)15.0F);
         server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 2, 0.3, 0.14, 0.04, 0.02);
      }

   }

   private void announce(ServerLevel server, FortuneWheelSpin.Plan plan) { double[] at;
      int quarters = plan.result().quarters();
      switch (this.resultKind()) {
         case JACKPOT:
            this.sound(server, RouletteMod.WHEEL_JACKPOT, AXLE_X, AXLE_Y, (double)14.0F, 1.5F, 1.0F);
            this.burst(server, (double)1.0F, true);
            break;
         case WIN:
            boolean big = quarters >= 8;
            this.sound(server, RouletteMod.WHEEL_WIN, AXLE_X, AXLE_Y, (double)14.0F, big ? 1.25F : 1.05F, 1.0F);
            this.burst(server, big ? (double)0.75F : (double)0.5F, false);
            break;
         case PUSH:
            this.sound(server, RouletteMod.WHEEL_PUSH, AXLE_X, AXLE_Y, (double)14.0F, 0.9F, 1.0F);
            at = this.world(AXLE_X, AXLE_Y, (double)15.0F);
            server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 10, 0.8, 0.8, 0.1, 0.03);
            break;
         case PARTIAL:
            this.sound(server, RouletteMod.WHEEL_PARTIAL, AXLE_X, AXLE_Y, (double)14.0F, 0.9F, 1.0F);
            double[] mouth = this.world((double)8.0F, (double)-10.5F, (double)15.0F);
            server.sendParticles(COIN, mouth[0], mouth[1], mouth[2], 6, 0.2, 0.08, 0.1, 0.12);
            break;
         default:
            this.sound(server, RouletteMod.WHEEL_LOSE, AXLE_X, AXLE_Y, (double)14.0F, 0.95F, 1.0F);
            double[] cap = this.world(AXLE_X, (double)31.5F, (double)14.0F);
            server.sendParticles(ParticleTypes.SMOKE, cap[0], cap[1], cap[2], 8, 0.15, 0.05, 0.12, 0.01);
            at = this.world(AXLE_X, 25.6, (double)15.0F);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at[0], at[1], at[2], 6, 0.35, 0.2, 0.05, 0.1);
      }

   }

   private void afterburst(ServerLevel server, FortuneWheelSpin.Plan plan, long sinceResult) {
      int quarters = plan.result().quarters();
      FortuneWheelGame.Kind kind = this.resultKind();
      if (kind == FortuneWheelGame.Kind.JACKPOT) {
         if (sinceResult >= 5L && sinceResult <= 35L && sinceResult % 5L == 0L) {
            this.burst(server, 0.8 - 0.1 * (double)(sinceResult / 5L - 1L), false);
         }
      } else if (kind == FortuneWheelGame.Kind.WIN && sinceResult == 8L) {
         this.burst(server, 0.35, false);
      }

   }

   private void burst(ServerLevel server, double strength, boolean jackpot) {
      double[] mouth = this.world((double)8.0F, (double)-10.5F, (double)15.5F);
      server.sendParticles(COIN, mouth[0], mouth[1], mouth[2], (int)Math.round((double)26.0F * strength), 0.3, 0.1, 0.12, 0.2);
      double[] face = this.world(AXLE_X, AXLE_Y, (double)15.5F);
      int n = (int)Math.round((double)24.0F * strength);
      server.sendParticles(GOLD_CONFETTI, face[0], face[1], face[2], n, 0.9, 0.9, (double)0.25F, 0.16);
      server.sendParticles(RED_CONFETTI, face[0], face[1], face[2], n, 0.9, 0.9, (double)0.25F, 0.16);
      server.sendParticles(GREEN_CONFETTI, face[0], face[1], face[2], n, 0.9, 0.9, (double)0.25F, 0.16);
      double[] cap = this.world(AXLE_X, (double)32.0F, (double)14.0F);
      server.sendParticles(ParticleTypes.FIREWORK, cap[0], cap[1], cap[2], (int)Math.round((double)22.0F * strength), (double)0.25F, 0.2, (double)0.25F, 0.22);
      server.sendParticles(ParticleTypes.END_ROD, cap[0], cap[1], cap[2], (int)Math.round((double)14.0F * strength), (double)0.25F, 0.2, (double)0.25F, 0.16);
      if (strength >= 0.7) {
         for(int i = 0; i < 16; ++i) {
            double[] bulb = this.world(FortuneWheelRenderGeometry.BULBS[3 * i], FortuneWheelRenderGeometry.BULBS[3 * i + 1], (double)14.5F);
            server.sendParticles(ParticleTypes.FIREWORK, bulb[0], bulb[1], bulb[2], 1, 0.04, 0.04, 0.04, 0.1);
         }
      }

      if (jackpot) {
         server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, face[0], face[1], face[2], 40, 0.8, 0.8, 0.3, 0.35);
         server.sendParticles(COIN, cap[0], cap[1] - 0.3, cap[2], 30, 0.3, 0.1, 0.3, 0.28);
      }

   }

   private void eject(ServerLevel server, ItemStack stack) {
      int turns = ((Direction)this.getBlockState().getValue(FortuneWheelBlock.FACING)).get2DDataValue();
      double[] at = this.world((double)8.0F, (double)-11.5F, (double)15.0F);
      int[] step = SlotMachineLayout.facingStep(turns);
      RandomSource random = server.getRandom();
      double side = (random.nextDouble() - (double)0.5F) * 0.16;
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

            for(ItemStack stack : this.payouts) {
               this.eject(server, stack);
            }

            this.payouts.clear();
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

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.machineIdentity = (CasinoMachineIdentity)input.read("MachineIdentity", CasinoMachineIdentity.CODEC).orElse(CasinoMachineIdentity.EMPTY);
      this.panelPlayer = (CasinoPlayerDisplay)input.read("PanelPlayer", CasinoPlayerDisplay.CODEC).orElse(CasinoPlayerDisplay.EMPTY);
      this.payoutTarget = (CasinoPayoutTarget)input.read("PayoutTarget", CasinoPayoutTarget.CODEC).orElse(CasinoPayoutTarget.EMPTY);
      this.croupierSettings = (CroupierSettings)input.read("Croupier", CroupierSettings.CODEC).orElse(CroupierSettings.EMPTY);
      this.locked = input.getBooleanOr("Locked", false);
      this.spinStart = input.getLongOr("SpinStart", -1L);
      this.seed = input.getLongOr("Seed", 0L);
      this.stage = input.getIntOr("Stage", 0);
      this.displayTicks = input.getIntOr("DisplayTicks", 0);
      this.cueIndex = -1;
      this.from = Math.floorMod(input.getIntOr("From", 0), 16);
      this.stop = Math.floorMod(input.getIntOr("Stop", 0), 16);
      this.plan = null;
      this.payouts.clear();

      for(ItemStack stack : input.listOrEmpty("Payouts", ItemStack.CODEC)) {
         this.payouts.add(stack);
      }

      for(int i = 0; i < 3; ++i) {
         this.bets.setItem(i, (ItemStack)input.read("Bet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
         this.displayedBets[i] = (ItemStack)input.read("DisplayBet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
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

      output.putBoolean("Locked", this.locked);
      output.putLong("SpinStart", this.spinStart);
      output.putLong("Seed", this.seed);
      output.putInt("Stage", this.stage);
      output.putInt("DisplayTicks", this.displayTicks);
      output.putInt("From", this.from);
      output.putInt("Stop", this.stop);
      ValueOutput.TypedOutputList<ItemStack> list = output.<ItemStack>list("Payouts", ItemStack.CODEC);

      for(ItemStack stack : this.payouts) {
         list.add(stack);
      }

      for(int i = 0; i < 3; ++i) {
         ItemStack bet = this.bets.getItem(i);
         if (!bet.isEmpty()) {
            output.store("Bet" + i, ItemStack.CODEC, bet);
         }

         if (!this.displayedBets[i].isEmpty()) {
            output.store("DisplayBet" + i, ItemStack.CODEC, this.displayedBets[i]);
         }
      }

   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
      output.putLong("SpinStart", this.spinStart);
      output.putLong("Seed", this.seed);
      output.putInt("From", this.from);
      output.putInt("Stop", this.stop);

      for(int i = 0; i < 3; ++i) {
         if (!this.displayedBets[i].isEmpty()) {
            output.store("DisplayBet" + i, ItemStack.CODEC, this.displayedBets[i]);
         }
      }

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
      AXLE_X = FortuneWheelRenderGeometry.AXLE[0];
      AXLE_Y = FortuneWheelRenderGeometry.AXLE[1];
      GOLD_CONFETTI = new DustParticleOptions(16765288, 1.25F);
      RED_CONFETTI = new DustParticleOptions(14629707, 1.15F);
      GREEN_CONFETTI = new DustParticleOptions(3594374, 1.15F);
      COIN = new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET);
   }
}

