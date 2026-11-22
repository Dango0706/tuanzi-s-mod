package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RouletteBlockEntity extends BlockEntity implements MenuProvider, CasinoConfiguredMachine, CasinoPayoutMachine {
   private static final DustParticleOptions RED_CONFETTI = new DustParticleOptions(14629707, 1.15F);
   private static final DustParticleOptions GOLD_CONFETTI = new DustParticleOptions(16765288, 1.25F);
   private static final DustParticleOptions GREEN_CONFETTI = new DustParticleOptions(3594374, 1.15F);
   private final SimpleContainer bets = new SimpleContainer(9) {
      {
         Objects.requireNonNull(RouletteBlockEntity.this);
      }

      public void setChanged() {
         super.setChanged();
         RouletteBlockEntity.this.setChanged();
      }
   };
   private final List<ItemStack> payouts = new ArrayList();
   private final ItemStack[] displayedBets = new ItemStack[9];
   private int displayTicks;
   private int stepsRemaining;
   private int stepCooldown;
   private int frame;
   private int result = -1;
   private boolean locked;
   private boolean broken;
   private RouletteSpin.Plan spinPlan;
   private long spinStart;
   private CasinoMachineIdentity machineIdentity;
   private CasinoPayoutTarget payoutTarget;
   private CasinoPlayerDisplay panelPlayer;
   private CroupierSettings croupierSettings;

   public RouletteBlockEntity(BlockPos pos, BlockState state) {
      super(RouletteMod.TABLE_ENTITY, pos, state);
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

   public RouletteSpin.Plan plan() {
      return this.spinPlan;
   }

   public long spinStart() {
      return this.spinStart;
   }

   public int restingFrame() {
      return (Integer)this.getBlockState().getValue(RouletteBlock.FRAME);
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
      return Component.translatable("block.roulettecasino.roulette_table");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      if (player instanceof ServerPlayer server) {
         CasinoSettings.menuOpened(server, id, this);
      }

      return this.locked ? null : new RouletteMenu(id, inventory, this, this.bets);
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
                  initiator.sendOverlayMessage(Component.translatable("message.roulettecasino.no_bet"));
               }

               return;
            }

            if (RouletteMenu.stakedColor(this.bets) == -2) {
               if (initiator != null) {
                  initiator.sendOverlayMessage(Component.translatable("gui.roulettecasino.roulette.several"));
               }

               return;
            }

            int chance = CasinoSettings.chance(this);
            double luck = CasinoSettings.playerLuck(initiator);
            int var10000;
            if (forced >= 0) {
               var10000 = Math.floorMod(forced, 37);
            } else if (chance >= 0 || luck != 0.0D) {
               int stakedColor = RouletteMenu.stakedColor(this.bets);
               RandomSource random = server.getRandom();
               var10000 = CasinoOdds.roulette(stakedColor, chance, luck, random::nextDouble, random::nextInt);
               me.tuanzi.util.ModLog.debug(initiator, "【赌场-轮盘】配置胜率=%d, 玩家幸运=%.2f, 幸运胜率修正=%.2f%%",
                       chance, luck, luck * 0.5D);
            } else {
               var10000 = server.getRandom().nextInt(37);
            }

            int roll = var10000;
            this.result = roll == 0 ? 2 : ((roll & 1) == 0 ? 1 : 0);
            this.panelPlayer = CasinoPlayerDisplay.of(initiator);
            if (initiator != null) {
               this.payoutTarget = CasinoPayoutTarget.of(initiator);
            }

            this.locked = true;
            this.frame = (Integer)server.getBlockState(this.worldPosition).getValue(RouletteBlock.FRAME);
            this.spinPlan = RouletteSpin.plan(this.frame, roll);
            this.spinStart = server.getGameTime();
            this.stepsRemaining = this.spinPlan.steps();
            this.stepCooldown = 0;
            this.displayTicks = 0;
            this.payouts.clear();

            for(int i = 0; i < 9; ++i) {
               ItemStack stake = this.bets.removeItemNoUpdate(i);
               this.displayedBets[i] = stake.copy();
               if (!stake.isEmpty() && i % 3 == this.result) {
                  this.payouts.addAll(CasinoExchange.payout(stake, this.result == 2 ? 36 : 2, 1, 0, CasinoSettings.exchange(this, stake), false));
               }
            }

            for(ServerPlayer player : server.players()) {
               AbstractContainerMenu var9 = player.containerMenu;
               if (var9 instanceof RouletteMenu) {
                  RouletteMenu menu = (RouletteMenu)var9;
                  if (menu.table() == this) {
                     player.closeContainer();
                  }
               }
            }

            server.playSound((Entity)null, this.worldPosition, RouletteMod.START, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.setChanged();
            this.syncDisplay(server);
            return;
         }
      }

   }

   public static void tick(Level world, BlockPos pos, RouletteBlockEntity entity) {
      if (world instanceof ServerLevel server) {
         CasinoSettings.identify(entity);
         if (entity.displayTicks > 0 && --entity.displayTicks == 0) {
            Arrays.fill(entity.displayedBets, ItemStack.EMPTY);
            entity.setChanged();
            entity.syncDisplay(server);
         }

         if (entity.locked) {
            if (entity.stepsRemaining > 0) {
               if (entity.spinPlan == null) {
                  entity.spinPlan = new RouletteSpin.Plan(Math.floorMod(entity.frame, 37), Math.clamp((long)entity.stepsRemaining, 1, 73));
                  entity.spinStart = server.getGameTime();
                  entity.syncDisplay(server);
               }

               if (server.getGameTime() < entity.spinStart) {
                  entity.spinStart = server.getGameTime();
                  entity.syncDisplay(server);
               }

               double elapsed = (double)(server.getGameTime() - entity.spinStart);
               double before = entity.spinPlan.phase(elapsed - (double)1.0F);
               double current = entity.spinPlan.phase(elapsed);
               entity.stepsRemaining = Math.max(1, (int)Math.ceil((double)(entity.spinPlan.initial() + entity.spinPlan.steps()) - current));
               if ((int)Math.floor(before * (double)3.0F) != (int)Math.floor(current * (double)3.0F)) {
                  server.playSound((Entity)null, pos, RouletteMod.TICK, SoundSource.BLOCKS, 0.44F, (float)(0.8 + Math.min(0.45, entity.spinPlan.speed(elapsed) * 0.4)));
               }

               entity.setChanged();
               if (elapsed >= (double)entity.spinPlan.resultTick()) {
                  entity.stepsRemaining = 0;
                  entity.frame = entity.spinPlan.finalFrame();
                  entity.setFrame(server, entity.frame);
                  entity.displayTicks = 20;
                  if (!entity.payouts.isEmpty()) {
                     entity.celebrate(server);
                  }

                  server.playSound((Entity)null, pos, entity.payouts.isEmpty() ? RouletteMod.LOSE : RouletteMod.WIN, SoundSource.BLOCKS, entity.payouts.isEmpty() ? 0.95F : 1.2F, 1.0F);
                  entity.setChanged();
                  entity.syncDisplay(server);
               }

            } else {
               if (!entity.payouts.isEmpty()) {
                  if (server.getGameTime() % 3L == 0L) {
                     ItemStack stack = (ItemStack)entity.payouts.removeFirst();
                     entity.eject(server, stack);
                     server.playSound((Entity)null, pos, RouletteMod.PAYOUT, SoundSource.BLOCKS, 0.43F, 0.95F + server.getRandom().nextFloat() * 0.2F);
                     entity.setChanged();
                  }
               } else {
                  entity.locked = false;
                  entity.payoutTarget = CasinoPayoutTarget.EMPTY;
                  entity.setChanged();
               }

            }
         }
      }
   }

   private void syncDisplay(ServerLevel server) {
      server.getChunkSource().blockChanged(this.worldPosition);
   }

   private void celebrate(ServerLevel server) {
      double x = (double)this.worldPosition.getX() + (double)1.0F;
      double y = (double)this.worldPosition.getY() + 1.1;
      double z = (double)this.worldPosition.getZ() + (double)1.0F;
      server.sendParticles(RED_CONFETTI, x, y, z, 18, 0.52, 0.16, 0.52, 0.18);
      server.sendParticles(GOLD_CONFETTI, x, y, z, 18, 0.52, 0.16, 0.52, 0.18);
      server.sendParticles(GREEN_CONFETTI, x, y, z, 18, 0.52, 0.16, 0.52, 0.18);
      server.sendParticles(ParticleTypes.FIREWORK, x, y, z, 20, 0.42, 0.2, 0.42, 0.2);
   }

   private void setFrame(ServerLevel server, int next) {
      for(int i = 0; i < 4; ++i) {
         BlockPos p = this.worldPosition.offset(i & 1, 0, i >> 1);
         BlockState old = server.getBlockState(p);
         if (old.is(RouletteMod.TABLE) && (Integer)old.getValue(RouletteBlock.FRAME) != next) {
            server.setBlock(p, (BlockState)old.setValue(RouletteBlock.FRAME, next), 2);
         }
      }

   }

   private void eject(ServerLevel server, ItemStack stack) {
      double x = (double)this.worldPosition.getX() + (double)1.0F;
      double y = (double)this.worldPosition.getY() + (double)1.0F;
      double z = (double)this.worldPosition.getZ() + (double)1.0F;
      ItemEntity item = new ItemEntity(server, x, y, z, stack);
      double angle = server.getRandom().nextDouble() * Math.PI * (double)2.0F;
      item.setDeltaMovement(Math.cos(angle) * 0.2, 0.3, Math.sin(angle) * 0.2);
      this.payoutTarget.aim(server, item);
      server.addFreshEntity(item);
   }

   public void onTableBroken() {
      if (!this.broken) {
         this.broken = true;
         Level var2 = this.level;
         if (var2 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var2;

            for(ServerPlayer player : server.players()) {
               AbstractContainerMenu var5 = player.containerMenu;
               if (var5 instanceof RouletteMenu) {
                  RouletteMenu menu = (RouletteMenu)var5;
                  if (menu.table() == this) {
                     player.closeContainer();
                  }
               }
            }

            for(ItemStack stack : this.payouts) {
               this.eject(server, stack);
            }

            this.payouts.clear();
            Arrays.fill(this.displayedBets, ItemStack.EMPTY);
            this.displayTicks = 0;

            for(int i = 0; i < 9; ++i) {
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

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.machineIdentity = (CasinoMachineIdentity)input.read("MachineIdentity", CasinoMachineIdentity.CODEC).orElse(CasinoMachineIdentity.EMPTY);
      this.panelPlayer = (CasinoPlayerDisplay)input.read("PanelPlayer", CasinoPlayerDisplay.CODEC).orElse(CasinoPlayerDisplay.EMPTY);
      this.payoutTarget = (CasinoPayoutTarget)input.read("PayoutTarget", CasinoPayoutTarget.CODEC).orElse(CasinoPayoutTarget.EMPTY);
      this.croupierSettings = (CroupierSettings)input.read("Croupier", CroupierSettings.CODEC).orElse(CroupierSettings.EMPTY);
      this.stepsRemaining = input.getIntOr("StepsRemaining", 0);
      this.stepCooldown = input.getIntOr("StepCooldown", 0);
      this.frame = input.getIntOr("Frame", 0);
      this.result = input.getIntOr("Result", -1);
      this.locked = input.getBooleanOr("Locked", false);
      this.displayTicks = input.getIntOr("DisplayTicks", 0);
      int initial = input.getIntOr("SpinInitial", -1);
      int spinSteps = input.getIntOr("SpinSteps", 0);
      this.spinPlan = initial >= 0 && initial < 37 && spinSteps >= 1 && spinSteps <= 73 ? new RouletteSpin.Plan(initial, spinSteps) : null;
      this.spinStart = input.getLongOr("SpinStart", 0L);
      this.payouts.clear();

      for(ItemStack stack : input.listOrEmpty("Payouts", ItemStack.CODEC)) {
         this.payouts.add(stack);
      }

      for(int i = 0; i < 9; ++i) {
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

      output.putInt("StepsRemaining", this.stepsRemaining);
      output.putInt("StepCooldown", this.stepCooldown);
      output.putInt("Frame", this.frame);
      output.putInt("Result", this.result);
      output.putBoolean("Locked", this.locked);
      output.putInt("DisplayTicks", this.displayTicks);
      if (this.spinPlan != null) {
         output.putInt("SpinInitial", this.spinPlan.initial());
         output.putInt("SpinSteps", this.spinPlan.steps());
         output.putLong("SpinStart", this.spinStart);
      }

      ValueOutput.TypedOutputList<ItemStack> list = output.<ItemStack>list("Payouts", ItemStack.CODEC);

      for(ItemStack stack : this.payouts) {
         list.add(stack);
      }

      for(int i = 0; i < 9; ++i) {
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
      if (this.spinPlan != null) {
         output.putInt("SpinInitial", this.spinPlan.initial());
         output.putInt("SpinSteps", this.spinPlan.steps());
         output.putLong("SpinStart", this.spinStart);
      }

      for(int i = 0; i < 9; ++i) {
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

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      this.onTableBroken();
      CasinoSettings.forget(this);
      super.preRemoveSideEffects(pos, state);
   }
}
