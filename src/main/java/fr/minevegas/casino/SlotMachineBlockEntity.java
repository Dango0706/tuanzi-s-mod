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

public final class SlotMachineBlockEntity extends BlockEntity implements MenuProvider, CasinoConfiguredMachine, CasinoPayoutMachine {
   public static final int SLOTS = 1;
   private static final int LEGACY_SLOTS = 3;
   private static final int PANEL_AFTER_RESULT = 70;
   private static final double MOUTH_X = (double)8.0F;
   private static final double MOUTH_Y = (double)6.5F;
   private static final double MOUTH_Z = (double)16.5F;
   private static final double WINDOW_Y = (double)25.75F;
   private static final double WINDOW_Z = (double)14.5F;
   private static final DustParticleOptions GOLD_CONFETTI = new DustParticleOptions(16765288, 1.25F);
   private static final DustParticleOptions RED_CONFETTI = new DustParticleOptions(14629707, 1.15F);
   private static final DustParticleOptions GREEN_CONFETTI = new DustParticleOptions(3594374, 1.15F);
   private static final ItemParticleOption COIN;
   private final SimpleContainer bets = new SimpleContainer(1) {
      {
         Objects.requireNonNull(SlotMachineBlockEntity.this);
      }

      public void setChanged() {
         super.setChanged();
         SlotMachineBlockEntity.this.setChanged();
      }
   };
   private final ItemStack[] displayedBets = new ItemStack[1];
   private final List<ItemStack> payouts = new ArrayList();
   private final List<ItemStack> legacyStakes = new ArrayList();
   private boolean locked;
   private boolean broken;
   private long spinStart = -1L;
   private int[] from = new int[3];
   private int[] stops = new int[3];
   private long seed;
   private int stage;
   private int displayTicks;
   private int cueIndex = -1;
   private SlotMachineSpin.Plan plan;
   private final ContainerData faceData = new ContainerData() {
      {
         Objects.requireNonNull(SlotMachineBlockEntity.this);
      }

      public int get(int index) {
         return SlotMachineBlockEntity.this.stops[index];
      }

      public void set(int index, int value) {
      }

      public int getCount() {
         return 3;
      }
   };
   private CasinoMachineIdentity machineIdentity;
   private CasinoPayoutTarget payoutTarget;
   private CasinoPlayerDisplay panelPlayer;
   private CroupierSettings croupierSettings;

   public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
      super(RouletteMod.SLOT_ENTITY, pos, state);
      this.machineIdentity = CasinoMachineIdentity.EMPTY;
      this.payoutTarget = CasinoPayoutTarget.EMPTY;
      this.panelPlayer = CasinoPlayerDisplay.EMPTY;
      this.croupierSettings = CroupierSettings.EMPTY;
      Arrays.fill(this.displayedBets, ItemStack.EMPTY);
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

   public int[] stops() {
      return (int[])this.stops.clone();
   }

   public int[] from() {
      return (int[])this.from.clone();
   }

   public SlotMachineSpin.Plan plan() {
      if (this.spinStart < 0L) {
         return null;
      } else {
         if (this.plan == null || this.plan.seed() != this.seed || !Arrays.equals(this.plan.stops(), this.stops) || !Arrays.equals(this.plan.from(), this.from)) {
            this.plan = SlotMachineSpin.plan(this.from, this.stops, this.seed);
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
      return Component.translatable("block.roulettecasino.slot_machine");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      if (player instanceof ServerPlayer server) {
         CasinoSettings.menuOpened(server, id, this);
      }

      return this.locked ? null : new SlotMachineMenu(id, inventory, this, this.bets, this.faceData);
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
                  initiator.sendOverlayMessage(Component.translatable("message.roulettecasino.slot_machine.no_bet"));
               }

               return;
            }

            int chance = CasinoSettings.chance(this);
            double luck = CasinoSettings.playerLuck(initiator);
            int[] next = new int[3];
            if (forced == null && (chance >= 0 || luck != 0.0D)) {
               RandomSource random = server.getRandom();
               next = CasinoOdds.slots(chance, luck, random::nextDouble, random::nextInt);
               me.tuanzi.util.ModLog.debug(initiator, "【赌场-老虎机】配置胜率=%d, 玩家幸运=%.2f, 幸运胜率修正=%.2f%%",
                       chance, luck, luck * 0.5D);
            } else {
               for(int k = 0; k < 3; ++k) {
                  next[k] = forced != null ? Math.floorMod(forced[k], 8) : server.getRandom().nextInt(8);
               }
            }

            this.from = (int[])this.stops.clone();
            this.stops = next;
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
            int multiplier = SlotMachineGame.evaluate(this.stops).multiplier();

            for(int i = 0; i < 1; ++i) {
               ItemStack stake = this.bets.removeItemNoUpdate(i);
               this.displayedBets[i] = stake.copy();
               if (!stake.isEmpty() && multiplier != 0) {
                  this.payouts.addAll(CasinoExchange.payout(stake, multiplier, 1, 0, CasinoSettings.exchange(this, stake), false));
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
         if (var5 instanceof SlotMachineMenu menu) {
            if (menu.machine() == this) {
               player.closeContainer();
            }
         }
      }

   }

   public static void tick(Level world, BlockPos pos, SlotMachineBlockEntity machine) {
      if (world instanceof ServerLevel server) {
         CasinoSettings.identify(machine);
         if (!machine.legacyStakes.isEmpty()) {
            for(ItemStack stake : machine.legacyStakes) {
               machine.eject(server, stake);
            }

            machine.legacyStakes.clear();
            machine.setChanged();
         }

         if (machine.displayTicks > 0 && --machine.displayTicks == 0) {
            Arrays.fill(machine.displayedBets, ItemStack.EMPTY);
            machine.setChanged();
            machine.sync(server);
         }

         if (machine.locked) {
            SlotMachineSpin.Plan plan = machine.plan();
            if (plan == null) {
               machine.locked = false;
               machine.setChanged();
            } else {
               long elapsed = server.getGameTime() - machine.spinStart;
               if (elapsed < 0L) {
                  machine.spinStart = server.getGameTime();
                  elapsed = 0L;
                  machine.setChanged();
                  machine.sync(server);
               }

               machine.cues(server, plan, elapsed);
               machine.teaseSparks(server, plan, elapsed);
               if (machine.stage < 1 && elapsed >= (long)plan.resultTick()) {
                  machine.stage = 1;
                  machine.displayTicks = 70;
                  if (elapsed - (long)plan.resultTick() <= 10L) {
                     machine.announce(server, plan);
                  }

                  machine.setChanged();
               }

               if (machine.stage >= 1) {
                  machine.afterburst(server, plan, elapsed - (long)plan.resultTick());
               }

               if (machine.stage >= 1 && elapsed >= (long)(plan.resultTick() + 12) && !machine.payouts.isEmpty()) {
                  int interval = machine.payouts.size() > 48 ? 1 : (machine.payouts.size() > 16 ? 2 : 3);
                  if (server.getGameTime() % (long)interval == 0L) {
                     machine.eject(server, (ItemStack)machine.payouts.removeFirst());
                     double[] mouth = machine.world((double)8.0F, (double)6.5F, (double)16.5F);
                     if (interval > 1 || server.getGameTime() % 2L == 0L) {
                        server.playSound((Entity)null, mouth[0], mouth[1], mouth[2], RouletteMod.SLOT_PAYOUT, SoundSource.BLOCKS, 0.5F, 0.9F + server.getRandom().nextFloat() * 0.25F);
                     }

                     machine.setChanged();
                  }
               } else if (machine.stage >= 1 && machine.payouts.isEmpty() && elapsed >= (long)plan.readyTick()) {
                  machine.locked = false;
                  machine.payoutTarget = CasinoPayoutTarget.EMPTY;
                  machine.setChanged();
               }

            }
         }
      }
   }

   private double[] world(double x, double y, double z) {
      int turns = ((Direction)this.getBlockState().getValue(SlotMachineBlock.FACING)).get2DDataValue();
      double[] at = SlotMachineLayout.toBlock(x, y, z, turns);
      return new double[]{(double)this.worldPosition.getX() + at[0], (double)this.worldPosition.getY() + at[1], (double)this.worldPosition.getZ() + at[2]};
   }

   private void sound(ServerLevel server, SoundEvent event, double x, double y, double z, float volume, float pitch) {
      double[] at = this.world(x, y, z);
      server.playSound((Entity)null, at[0], at[1], at[2], event, SoundSource.BLOCKS, volume, pitch);
   }

   private void cues(ServerLevel server, SlotMachineSpin.Plan plan, long elapsed) {
      List<SlotMachineSpin.Cue> cues = plan.cues();
      if (this.cueIndex < 0) {
         for(this.cueIndex = 0; this.cueIndex < cues.size() && (long)((SlotMachineSpin.Cue)cues.get(this.cueIndex)).tick() < elapsed - 1L; ++this.cueIndex) {
         }
      }

      while(this.cueIndex < cues.size() && (long)((SlotMachineSpin.Cue)cues.get(this.cueIndex)).tick() <= elapsed) {
         SlotMachineSpin.Cue cue = (SlotMachineSpin.Cue)cues.get(this.cueIndex++);
         double drumX = cue.reel() >= 0 ? SlotMachineRenderGeometry.DRUM_X0[cue.reel()] + (double)2.5F : (double)8.0F;
         switch (cue.sound()) {
            case LEVER:
               this.sound(server, RouletteMod.SLOT_LEVER, 20.6, (double)10.0F, (double)12.0F, cue.volume(), cue.pitch());
               break;
            case WHIRR:
               this.sound(server, RouletteMod.SLOT_WHIRR, (double)8.0F, (double)25.75F, (double)14.5F, cue.volume(), cue.pitch());
               break;
            case CLICK:
               this.sound(server, RouletteMod.SLOT_CLICK, drumX, (double)25.75F, (double)14.5F, cue.volume(), cue.pitch());
               break;
            case STOP:
               this.sound(server, RouletteMod.SLOT_STOP, drumX, (double)25.75F, (double)14.5F, cue.volume(), cue.pitch());
               double[] at = this.world(drumX, (double)25.75F, (double)15.5F);
               server.sendParticles(ParticleTypes.CRIT, at[0], at[1], at[2], 5, 0.12, 0.1, 0.04, 0.12);
         }
      }

   }

   private void teaseSparks(ServerLevel server, SlotMachineSpin.Plan plan, long elapsed) {
      if (plan.tease() && elapsed % 3L == 0L) {
         double speed = plan.speed(2, (double)elapsed);
         if (!(speed <= (double)0.0F) && !(speed >= 0.55)) {
            double[] at = this.world((double)8.0F, (double)25.75F, (double)15.5F);
            server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 3, 0.38, 0.22, 0.04, 0.02);
         }
      }
   }

   private void announce(ServerLevel server, SlotMachineSpin.Plan plan) { double[] at;
      SlotMachineGame.Kind kind = plan.result().kind();
      switch (kind) {
         case JACKPOT:
            this.sound(server, RouletteMod.SLOT_JACKPOT, (double)8.0F, (double)25.75F, (double)14.5F, 1.5F, 1.0F);
            this.burst(server, (double)1.0F, true);
            break;
         case BIG:
            this.sound(server, RouletteMod.SLOT_WIN, (double)8.0F, (double)25.75F, (double)14.5F, 1.3F, 1.0F);
            this.burst(server, 0.8, false);
            break;
         case WIN:
            this.sound(server, RouletteMod.SLOT_WIN, (double)8.0F, (double)25.75F, (double)14.5F, 1.05F, 1.0F);
            this.burst(server, (double)0.5F, false);
            break;
         case PUSH:
            this.sound(server, RouletteMod.SLOT_PUSH, (double)8.0F, (double)25.75F, (double)14.5F, 0.9F, 1.0F);
            at = this.world((double)8.0F, (double)7.5F, (double)17.0F);
            server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 8, (double)0.25F, 0.1, 0.1, 0.03);
            break;
         default:
            this.sound(server, RouletteMod.SLOT_LOSE, (double)8.0F, (double)25.75F, (double)14.5F, 0.95F, 1.0F);
            double[] lamp = this.world((double)8.0F, (double)39.5F, (double)7.5F);
            server.sendParticles(ParticleTypes.SMOKE, lamp[0], lamp[1], lamp[2], 6, 0.12, 0.05, 0.12, 0.01);
            at = this.world((double)8.0F, (double)25.75F, (double)15.5F);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at[0], at[1], at[2], 6, 0.4, 0.2, 0.05, 0.1);
      }

   }

   private void afterburst(ServerLevel server, SlotMachineSpin.Plan plan, long sinceResult) {
      SlotMachineGame.Kind kind = plan.result().kind();
      if (kind == SlotMachineGame.Kind.JACKPOT) {
         if (sinceResult >= 5L && sinceResult <= 35L && sinceResult % 5L == 0L) {
            this.burst(server, 0.8 - 0.1 * (double)(sinceResult / 5L - 1L), false);
         }
      } else if (kind == SlotMachineGame.Kind.BIG) {
         if (sinceResult == 7L || sinceResult == 14L) {
            this.burst(server, (double)0.5F, false);
         }
      } else if (kind == SlotMachineGame.Kind.WIN && sinceResult == 8L) {
         this.burst(server, 0.35, false);
      }

   }

   private void burst(ServerLevel server, double strength, boolean jackpot) {
      double[] mouth = this.world((double)8.0F, (double)8.0F, (double)17.5F);
      server.sendParticles(COIN, mouth[0], mouth[1], mouth[2], (int)Math.round((double)26.0F * strength), 0.22, 0.1, 0.12, 0.2);
      double[] window = this.world((double)8.0F, (double)28.75F, (double)16.0F);
      int n = (int)Math.round((double)26.0F * strength);
      server.sendParticles(GOLD_CONFETTI, window[0], window[1], window[2], n, 0.55, 0.45, (double)0.25F, 0.16);
      server.sendParticles(RED_CONFETTI, window[0], window[1], window[2], n, 0.55, 0.45, (double)0.25F, 0.16);
      server.sendParticles(GREEN_CONFETTI, window[0], window[1], window[2], n, 0.55, 0.45, (double)0.25F, 0.16);
      double[] lamp = this.world((double)8.0F, (double)40.0F, (double)7.5F);
      server.sendParticles(ParticleTypes.FIREWORK, lamp[0], lamp[1], lamp[2], (int)Math.round((double)22.0F * strength), (double)0.25F, 0.2, (double)0.25F, 0.22);
      server.sendParticles(ParticleTypes.END_ROD, lamp[0], lamp[1], lamp[2], (int)Math.round((double)14.0F * strength), (double)0.25F, 0.2, (double)0.25F, 0.16);
      if (jackpot) {
         server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, window[0], window[1], window[2], 36, (double)0.5F, (double)0.5F, 0.3, 0.35);
         server.sendParticles(COIN, lamp[0], lamp[1] - 0.3, lamp[2], 30, 0.3, 0.1, 0.3, 0.28);
      }

   }

   private void eject(ServerLevel server, ItemStack stack) {
      int turns = ((Direction)this.getBlockState().getValue(SlotMachineBlock.FACING)).get2DDataValue();
      double[] at = this.world((double)8.0F, (double)7.0F, (double)17.0F);
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

            for(ItemStack stack : this.legacyStakes) {
               this.eject(server, stack);
            }

            this.legacyStakes.clear();
            Arrays.fill(this.displayedBets, ItemStack.EMPTY);
            this.displayTicks = 0;

            for(int i = 0; i < 1; ++i) {
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

      for(int k = 0; k < 3; ++k) {
         this.from[k] = Math.floorMod(input.getIntOr("From" + k, 0), 8);
         this.stops[k] = Math.floorMod(input.getIntOr("Stop" + k, 0), 8);
      }

      this.plan = null;
      this.payouts.clear();

      for(ItemStack stack : input.listOrEmpty("Payouts", ItemStack.CODEC)) {
         this.payouts.add(stack);
      }

      for(int i = 0; i < 1; ++i) {
         this.bets.setItem(i, (ItemStack)input.read("Bet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
         this.displayedBets[i] = (ItemStack)input.read("DisplayBet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
      }

      this.legacyStakes.clear();

      for(int i = 1; i < 3; ++i) {
         ItemStack stake = (ItemStack)input.read("Bet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
         if (!stake.isEmpty()) {
            this.legacyStakes.add(stake);
         }
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

      for(int k = 0; k < 3; ++k) {
         output.putInt("From" + k, this.from[k]);
         output.putInt("Stop" + k, this.stops[k]);
      }

      ValueOutput.TypedOutputList<ItemStack> list = output.<ItemStack>list("Payouts", ItemStack.CODEC);

      for(ItemStack stack : this.payouts) {
         list.add(stack);
      }

      for(int i = 0; i < 1; ++i) {
         ItemStack bet = this.bets.getItem(i);
         if (!bet.isEmpty()) {
            output.store("Bet" + i, ItemStack.CODEC, bet);
         }

         if (!this.displayedBets[i].isEmpty()) {
            output.store("DisplayBet" + i, ItemStack.CODEC, this.displayedBets[i]);
         }
      }

      for(int i = 0; i < this.legacyStakes.size() && 1 + i < 3; ++i) {
         output.store("Bet" + (1 + i), ItemStack.CODEC, (ItemStack)this.legacyStakes.get(i));
      }

   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
      output.putLong("SpinStart", this.spinStart);
      output.putLong("Seed", this.seed);

      for(int k = 0; k < 3; ++k) {
         output.putInt("From" + k, this.from[k]);
         output.putInt("Stop" + k, this.stops[k]);
      }

      for(int i = 0; i < 1; ++i) {
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
      COIN = new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_NUGGET);
   }
}

