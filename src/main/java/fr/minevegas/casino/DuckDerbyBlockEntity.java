package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public final class DuckDerbyBlockEntity extends BlockEntity implements MenuProvider, CasinoConfiguredMachine {
   public static final int LANES = 4;
   public static final int ROWS = 3;
   public static final int SLOTS = 12;
   public static final int PAYOUT_MULTIPLIER = 3;
   public static final int SECOND_MULTIPLIER = 1;
   private static final int PANEL_AFTER_RESULT = 70;
   private static final DustParticleOptions GOLD_CONFETTI = new DustParticleOptions(16765288, 1.25F);
   private static final DustParticleOptions RED_CONFETTI = new DustParticleOptions(14629707, 1.15F);
   private static final DustParticleOptions GREEN_CONFETTI = new DustParticleOptions(3594374, 1.15F);
   private static final DustParticleOptions[] DUCK_CONFETTI = new DustParticleOptions[]{new DustParticleOptions(16765000, 1.3F), new DustParticleOptions(14170696, 1.3F), new DustParticleOptions(15920342, 1.3F), new DustParticleOptions(8161440, 1.3F)};
   private final SimpleContainer bets = new SimpleContainer(12) {
      {
         Objects.requireNonNull(DuckDerbyBlockEntity.this);
      }

      public void setChanged() {
         super.setChanged();
         DuckDerbyBlockEntity.this.setChanged();
      }
   };
   private final ItemStack[] displayedBets = new ItemStack[12];
   private final List<ItemStack> payouts = new ArrayList();
   private boolean locked;
   private boolean won;
   private boolean refunded;
   private boolean broken;
   private long raceStart = -1L;
   private int winner = -1;
   private long seed;
   private int stage;
   private int displayTicks;
   private int quackIndex = -1;
   private DuckDerbyRace.Plan plan;
   public static final int ENROLL_TICKS = 200;
   public static final int MAX_PLAYERS = 64;
   private final Map<UUID, SimpleContainer> drafts = new LinkedHashMap();
   private final Map<UUID, Participant> participants = new LinkedHashMap();
   private long enrollmentUntil = -1L;
   private boolean personalRound;
   private boolean groupRace;
   private boolean recoverPersonal;
   private int lastCount;
   private List<DerbyLobby.Member> clientMembers = List.of();
   private CasinoMachineIdentity machineIdentity;
   private CroupierSettings croupierSettings;

   public boolean enrolling() {
      return this.enrollmentUntil >= 0L;
   }

   public int enrollmentSeconds() {
      return this.enrolling() && this.level != null ? (int)Math.max(0L, (this.enrollmentUntil - this.level.getGameTime() + 19L) / 20L) : 0;
   }

   public float enrollmentProgress(float partialTick) {
      return this.level == null ? 0.0F : DerbyPanelPresentation.progress(this.enrollmentUntil, this.level.getGameTime(), partialTick, 200);
   }

   public boolean groupAllowed() {
      int chance = CasinoSettings.chance(this);
      return chance < 0 || chance == 25;
   }

   public boolean groupSession() {
      return this.groupRace && (this.locked || this.enrolling() || this.displayTicks > 0);
   }

   public int participantCount() {
      return this.participants.isEmpty() ? this.lastCount : this.participants.size();
   }

   public boolean confirmed(UUID player) {
      return this.participants.containsKey(player);
   }

   public List<DerbyLobby.Member> members() {
      return this.level != null && this.level.isClientSide() ? this.clientMembers : this.participants.values().stream().map((p) -> new DerbyLobby.Member(UUID.fromString(p.owner), p.name, p.lane, p.stakes.stream().map(ItemStack::copy).toList())).toList();
   }

   private SimpleContainer newDraft() {
      return new SimpleContainer(12) {
         {
            Objects.requireNonNull(DuckDerbyBlockEntity.this);
         }

         public void setChanged() {
            super.setChanged();
            DuckDerbyBlockEntity.this.setChanged();
         }
      };
   }

   public void resumeDraft(UUID owner, SimpleContainer container) {
      if (!this.broken && !this.locked) {
         this.drafts.put(owner, container);
      }

   }

   private static List<ItemStack> drain(SimpleContainer container) {
      List<ItemStack> out = new ArrayList();

      for(int i = 0; i < container.getContainerSize(); ++i) {
         ItemStack item = container.removeItemNoUpdate(i);
         if (!item.isEmpty()) {
            out.add(item);
         }
      }

      container.setChanged();
      return out;
   }

   private void refund(UUID owner, SimpleContainer container, ServerLevel server) {
      DerbyCredits.get(server.getServer()).refund(owner, drain(container), server.getServer(), server.dimension().identifier().toString(), this.worldPosition);
   }

   public void closeDraft(ServerPlayer player, SimpleContainer container) {
      Level var4 = this.level;
      if (var4 instanceof ServerLevel server) {
         if (this.confirmed(player.getUUID())) {
            container.clearContent();
         } else if (this.drafts.remove(player.getUUID(), container)) {
            this.refund(player.getUUID(), container, server);
         }

         this.setChanged();
      }
   }

   private void cleanupDrafts(ServerLevel server) {
      Iterator<Map.Entry<UUID, SimpleContainer>> it = this.drafts.entrySet().iterator();

      while(it.hasNext()) {
         Map.Entry<UUID, SimpleContainer> e = (Map.Entry)it.next();
         ServerPlayer player = server.getServer().getPlayerList().getPlayer((UUID)e.getKey());
         if (player != null && player.isAlive()) {
            AbstractContainerMenu var6 = player.containerMenu;
            if (var6 instanceof DuckDerbyMenu) {
               DuckDerbyMenu menu = (DuckDerbyMenu)var6;
               if (menu.machine() == this) {
                  continue;
               }
            }
         }

         it.remove();
         this.refund((UUID)e.getKey(), (SimpleContainer)e.getValue(), server);
      }

   }

   private void returnRejected(ServerPlayer player, DuckDerbyMenu menu) {
      List<ItemStack> rejected = new ArrayList();

      for(int i = 0; i < 12; ++i) {
         ItemStack stack = menu.bets().getItem(i);
         if (!stack.isEmpty() && !CasinoSettings.accepts((BlockEntity)this, stack)) {
            rejected.add(menu.bets().removeItemNoUpdate(i));
         }
      }

      if (!rejected.isEmpty()) {
         menu.bets().setChanged();
         DerbyCredits.get(player.level().getServer()).refund(player, rejected, this.worldPosition);
      }

   }

   public boolean joinGroup(ServerPlayer player, DuckDerbyMenu menu) {
      if (!this.locked && !this.broken && this.bets.isEmpty() && this.groupAllowed() && this.participants.size() < 64 && !this.confirmed(player.getUUID())) {
         if (this.enrolling() && this.level.getGameTime() >= this.enrollmentUntil) {
            return false;
         } else {
            this.returnRejected(player, menu);
            int lane = menu.stakedLane();
            if (lane < 0) {
               return false;
            } else {
               if (!this.enrolling()) {
                  this.enrollmentUntil = this.level.getGameTime() + 200L;
                  this.groupRace = true;
                  this.personalRound = true;
                  this.lastCount = 0;
                  this.winner = -1;
                  this.seed = 0L;
                  this.raceStart = -1L;
                  this.plan = null;
                  this.displayTicks = 0;
                  Arrays.fill(this.displayedBets, ItemStack.EMPTY);
               }

               return this.commit(player, menu, lane);
            }
         }
      } else {
         return false;
      }
   }

   private boolean commit(ServerPlayer player, DuckDerbyMenu menu, int lane) {
      List<ItemStack> stakes = drain(menu.bets());
      if (stakes.isEmpty()) {
         return false;
      } else {
         String escrow = DerbyCredits.get(player.level().getServer()).hold(player, stakes, player.level().getServer().overworld().getGameTime() + (this.enrollmentUntil < 0L ? 1L : Math.max(1L, this.enrollmentUntil - this.level.getGameTime())), this.worldPosition);
         this.participants.put(player.getUUID(), new Participant(player.getUUID().toString(), player.getName().getString(), lane, escrow, stakes.stream().map(ItemStack::copy).toList()));
         this.drafts.remove(player.getUUID(), menu.bets());
         menu.escrowView(true);

         for(int row = 0; row < stakes.size(); ++row) {
            menu.bets().setItem(row * 4 + lane, ((ItemStack)stakes.get(row)).copy());
         }

         this.setChanged();
         this.sync((ServerLevel)this.level);
         return true;
      }
   }

   public boolean withdraw(ServerPlayer player, DuckDerbyMenu menu) {
      if (this.enrolling() && this.level.getGameTime() < this.enrollmentUntil) {
         Participant p = (Participant)this.participants.remove(player.getUUID());
         if (p == null) {
            return false;
         } else {
            menu.bets().clearContent();
            menu.escrowView(false);
            this.drafts.put(player.getUUID(), menu.bets());
            DerbyCredits credits = DerbyCredits.get(player.level().getServer());
            credits.release(p.escrow);
            credits.pay(player, true);
            if (this.participants.isEmpty()) {
               this.enrollmentUntil = -1L;
               this.groupRace = false;
            }

            this.setChanged();
            this.sync((ServerLevel)this.level);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean startSolo(ServerPlayer player, DuckDerbyMenu menu, int forced) {
      if (!this.locked && !this.enrolling() && !this.broken && this.bets.isEmpty()) {
         this.returnRejected(player, menu);
         int lane = menu.stakedLane();
         if (lane < 0) {
            return false;
         } else {
            this.participants.clear();
            this.groupRace = false;
            this.personalRound = true;
            this.lastCount = 0;
            if (!this.commit(player, menu, lane)) {
               return false;
            } else {
               this.launchPersonal((ServerLevel)this.level, forced);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void launchPersonal(ServerLevel server, int forced) {
      DerbyCredits credits = DerbyCredits.get(server.getServer());
      this.participants.entrySet().removeIf((e) -> !credits.held(((Participant)e.getValue()).escrow));
      if (this.participants.isEmpty()) {
         this.enrollmentUntil = -1L;
         this.setChanged();
         this.sync(server);
      } else if (this.groupRace && !this.groupAllowed()) {
         for(Participant p : this.participants.values()) {
            credits.release(p.escrow);
         }

         this.participants.clear();
         this.enrollmentUntil = -1L;
         this.groupRace = false;
         this.setChanged();
         this.sync(server);
      } else {
         for(Participant p : this.participants.values()) {
            credits.revalidate(p.escrow, this);
         }

         this.participants.entrySet().removeIf((e) -> !credits.held(((Participant)e.getValue()).escrow));
         if (this.participants.isEmpty()) {
            this.enrollmentUntil = -1L;
            this.setChanged();
            this.sync(server);
         } else {
            this.participants.replaceAll((id, px) -> new Participant(px.owner, px.name, px.lane, px.escrow, credits.stakes(px.escrow)));
            Participant first = (Participant)this.participants.values().iterator().next();
            int var10001;
            if (forced >= 0) {
               var10001 = Math.floorMod(forced, 4);
            } else if (this.groupRace) {
               var10001 = server.getRandom().nextInt(4);
            } else if (CasinoSettings.chance(this) < 0) {
               var10001 = server.getRandom().nextInt(4);
            } else {
               var10001 = first.lane;
               int var10002 = CasinoSettings.chance(this);
               RandomSource var10003 = server.getRandom();
               Objects.requireNonNull(var10003);
               var10001 = CasinoOdds.derby(var10001, var10002, var10003::nextInt);
            }

            this.winner = var10001;
            this.seed = server.getRandom().nextLong();
            this.plan = null;
            this.raceStart = server.getGameTime();
            this.stage = 0;
            this.quackIndex = 0;
            this.displayTicks = 0;
            this.locked = true;
            this.enrollmentUntil = -1L;
            this.payouts.clear();
            Arrays.fill(this.displayedBets, ItemStack.EMPTY);
            int second = runnerUp(this.plan());
            this.won = false;
            this.refunded = false;

            for(Participant p : this.participants.values()) {
               List<ItemStack> stakes = credits.stakes(p.escrow);
               List<ItemStack> reward = new ArrayList();

               for(int i = 0; i < stakes.size(); ++i) {
                  ItemStack stake = (ItemStack)stakes.get(i);
                  if (!this.groupRace) {
                     this.displayedBets[i * 4 + p.lane] = stake.copy();
                  }

                  if (p.lane == this.winner || p.lane == second) {
                     reward.addAll(CasinoExchange.payout(stake, p.lane == this.winner ? 3 : 1, 1, 0, CasinoSettings.exchange(this, stake), false));
                  }
               }

               credits.settle(p.escrow, reward, server.getServer().overworld().getGameTime() + (long)this.plan().resultTick() + 12L);
               this.won |= p.lane == this.winner;
               this.refunded |= p.lane == second;
            }

            this.lastCount = this.participants.size();
            this.closeMenus(server);
            double[] at = this.world((double)24.0F, (double)14.0F, (double)32.0F);
            server.playSound((Entity)null, at[0], at[1], at[2], RouletteMod.DUCK_START, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.setChanged();
            this.sync(server);
         }
      }
   }

   private void personalTick(ServerLevel server) {
      this.cleanupDrafts(server);
      if (this.recoverPersonal) {
         this.recoverPersonal = false;
         if (this.enrolling()) {
            for(Participant p : this.participants.values()) {
               DerbyCredits.get(server.getServer()).release(p.escrow);
            }

            this.participants.clear();
            this.enrollmentUntil = -1L;
            this.groupRace = false;
            this.setChanged();
            this.sync(server);
         }
      }

      if (this.enrolling()) {
         DerbyCredits credits = DerbyCredits.get(server.getServer());
         this.participants.entrySet().removeIf((e) -> {
            ServerPlayer p = server.getServer().getPlayerList().getPlayer((UUID)e.getKey());
            if (p != null && p.isAlive() && !p.hasDisconnected()) {
               return false;
            } else {
               credits.release(((Participant)e.getValue()).escrow);
               this.setChanged();
               return true;
            }
         });
         if (this.participants.isEmpty()) {
            this.enrollmentUntil = -1L;
            this.groupRace = false;
            this.setChanged();
            this.sync(server);
         } else if (server.getGameTime() >= this.enrollmentUntil) {
            this.launchPersonal(server, -1);
         } else {
            if (server.getGameTime() % 20L == 0L) {
               this.sync(server);
            }

         }
      }
   }

   public DuckDerbyBlockEntity(BlockPos pos, BlockState state) {
      super(RouletteMod.DERBY_ENTITY, pos, state);
      this.machineIdentity = CasinoMachineIdentity.EMPTY;
      this.croupierSettings = CroupierSettings.EMPTY;
      Arrays.fill(this.displayedBets, ItemStack.EMPTY);
   }

   public boolean isRacing() {
      return this.locked;
   }

   public SimpleContainer bets() {
      return this.bets;
   }

   public ItemStack displayedBet(int slot) {
      return this.displayedBets[slot];
   }

   public long raceStart() {
      return this.raceStart;
   }

   public int winner() {
      return this.winner;
   }

   public static int runnerUp(DuckDerbyRace.Plan plan) {
      if (plan == null) {
         return -1;
      } else {
         for(int lane = 0; lane < 4; ++lane) {
            if (plan.rank(lane) == 1) {
               return lane;
            }
         }

         return -1;
      }
   }

   public DuckDerbyRace.Plan plan() {
      if (this.winner < 0) {
         return null;
      } else {
         if (this.plan == null || this.plan.winner() != this.winner || this.plan.seed() != this.seed) {
            this.plan = DuckDerbyRace.plan(this.winner, this.seed);
         }

         return this.plan;
      }
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
      return Component.translatable("block.roulettecasino.duck_derby");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      if (player instanceof ServerPlayer server) {
         CasinoSettings.menuOpened(server, id, this);
      }

      if (this.locked) {
         return null;
      } else if (!this.bets.isEmpty()) {
         return new DuckDerbyMenu(id, inventory, this, this.bets);
      } else {
         Participant p = (Participant)this.participants.get(player.getUUID());
         SimpleContainer own;
         if (p != null) {
            own = this.newDraft();
            List<ItemStack> stakes = DerbyCredits.get(player.level().getServer()).stakes(p.escrow);

            for(int row = 0; row < stakes.size(); ++row) {
               own.setItem(row * 4 + p.lane, (ItemStack)stakes.get(row));
            }
         } else {
            own = (SimpleContainer)this.drafts.computeIfAbsent(player.getUUID(), (key) -> this.newDraft());
         }

         return new DuckDerbyMenu(id, inventory, this, own);
      }
   }

   public void start(ServerPlayer initiator) {
      this.start(initiator, -1);
   }

   public void start(ServerPlayer initiator, int forced) {
      if (this.bets.isEmpty()) {
         if (initiator != null) {
            AbstractContainerMenu var4 = initiator.containerMenu;
            if (var4 instanceof DuckDerbyMenu) {
               DuckDerbyMenu menu = (DuckDerbyMenu)var4;
               if (menu.machine() == this) {
                  this.startSolo(initiator, menu, forced);
               }
            }
         }

      } else {
         this.startLegacy(initiator, forced);
      }
   }

   private void startLegacy(ServerPlayer initiator, int forced) {
      if (!this.locked) {
         Level var4 = this.level;
         if (var4 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var4;
            this.personalRound = false;
            this.groupRace = false;
            CasinoSettings.returnRejected(this, this.bets, initiator);
            if (this.bets.isEmpty()) {
               if (initiator != null) {
                  initiator.sendOverlayMessage(Component.translatable("message.roulettecasino.derby.no_bet"));
               }

               return;
            }

            if (DuckDerbyMenu.stakedLane(this.bets) == -2) {
               if (initiator != null) {
                  initiator.sendOverlayMessage(Component.translatable("message.roulettecasino.derby.one_duck"));
               }

               return;
            }

            int chance = CasinoSettings.chance(this);
            int var10001;
            if (forced >= 0) {
               var10001 = Math.floorMod(forced, 4);
            } else if (chance >= 0) {
               var10001 = DuckDerbyMenu.stakedLane(this.bets);
               RandomSource var10003 = server.getRandom();
               Objects.requireNonNull(var10003);
               var10001 = CasinoOdds.derby(var10001, chance, var10003::nextInt);
            } else {
               var10001 = server.getRandom().nextInt(4);
            }

            this.winner = var10001;
            this.seed = server.getRandom().nextLong();
            this.plan = null;
            this.raceStart = server.getGameTime();
            this.stage = 0;
            this.quackIndex = 0;
            this.displayTicks = 0;
            this.locked = true;
            this.payouts.clear();
            int second = runnerUp(this.plan());
            this.won = false;
            this.refunded = false;

            for(int i = 0; i < 12; ++i) {
               ItemStack stake = this.bets.removeItemNoUpdate(i);
               this.displayedBets[i] = stake.copy();
               if (!stake.isEmpty() && (i % 4 == this.winner || i % 4 == second)) {
                  if (i % 4 == this.winner) {
                     this.won = true;
                  } else {
                     this.refunded = true;
                  }

                  this.payouts.addAll(CasinoExchange.payout(stake, i % 4 == this.winner ? 3 : 1, 1, 0, CasinoSettings.exchange(this, stake), false));
               }
            }

            this.closeMenus(server);
            double[] centre = this.world((double)24.0F, (double)14.0F, (double)32.0F);
            server.playSound((Entity)null, centre[0], centre[1], centre[2], RouletteMod.DUCK_START, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.setChanged();
            this.sync(server);
            return;
         }
      }

   }

   private void closeMenus(ServerLevel server) {
      for(ServerPlayer player : server.players()) {
         AbstractContainerMenu var5 = player.containerMenu;
         if (var5 instanceof DuckDerbyMenu menu) {
            if (menu.machine() == this) {
               player.closeContainer();
            }
         }
      }

   }

   public static void tick(Level world, BlockPos pos, DuckDerbyBlockEntity machine) {
      if (world instanceof ServerLevel server) {
         CasinoSettings.identify(machine);
         machine.personalTick(server);
         if (machine.displayTicks > 0 && --machine.displayTicks == 0) {
            Arrays.fill(machine.displayedBets, ItemStack.EMPTY);
            machine.setChanged();
            machine.sync(server);
         }

         if (machine.locked) {
            DuckDerbyRace.Plan plan = machine.plan();
            if (plan == null) {
               machine.locked = false;
               machine.setChanged();
            } else {
               long elapsed = server.getGameTime() - machine.raceStart;
               if (elapsed < 0L) {
                  machine.raceStart = server.getGameTime();
                  elapsed = 0L;
                  machine.setChanged();
                  machine.sync(server);
               }

               machine.quacks(server, plan, elapsed);
               machine.splashes(server, plan, elapsed);
               if (machine.stage < 1 && elapsed >= (long)plan.winnerFinishTick()) {
                  machine.stage = 1;
                  if (elapsed - (long)plan.winnerFinishTick() <= 10L) {
                     machine.finishLine(server, plan);
                  }

                  machine.setChanged();
               }

               if (machine.stage < 2 && elapsed >= (long)plan.resultTick()) {
                  machine.stage = 2;
                  if (machine.personalRound) {
                     for(Participant p : machine.participants.values()) {
                        ServerPlayer player = server.getServer().getPlayerList().getPlayer(UUID.fromString(p.owner));
                        if (player != null) {
                           player.sendOverlayMessage(Component.translatable(p.lane == machine.winner ? "message.roulettecasino.derby.personal_win" : (p.lane == runnerUp(plan) ? "message.roulettecasino.derby.personal_push" : "message.roulettecasino.derby.personal_loss")));
                        }
                     }
                  }

                  machine.displayTicks = 70;
                  if (elapsed - (long)plan.resultTick() <= 10L) {
                     machine.announce(server, plan);
                  }

                  machine.setChanged();
               }

               if (machine.stage >= 2 && machine.won && elapsed - (long)plan.resultTick() >= 7L && elapsed - (long)plan.resultTick() <= 14L && (elapsed - (long)plan.resultTick()) % 7L == 0L) {
                  machine.confetti(server, plan, elapsed, 0.7);
               }

               if (machine.stage >= 2 && elapsed >= (long)(plan.resultTick() + 12) && !machine.payouts.isEmpty()) {
                  if (server.getGameTime() % 3L == 0L) {
                     machine.eject(server, (ItemStack)machine.payouts.removeFirst());
                     double[] front = machine.world((double)24.0F, (double)14.0F, (double)64.0F);
                     server.playSound((Entity)null, front[0], front[1], front[2], RouletteMod.DUCK_PAYOUT, SoundSource.BLOCKS, 0.43F, 0.95F + server.getRandom().nextFloat() * 0.2F);
                     machine.setChanged();
                  }
               } else if (machine.stage >= 2 && machine.payouts.isEmpty() && elapsed >= (long)plan.endTick()) {
                  machine.locked = false;
                  machine.participants.clear();
                  machine.setChanged();
                  machine.sync(server);
               }

            }
         }
      }
   }

   private double[] world(double x, double y, double z) {
      int turns = ((Direction)this.getBlockState().getValue(DuckDerbyBlock.FACING)).get2DDataValue();
      double[] at = DuckDerbyLayout.toBlock(x, y, z, turns);
      return new double[]{(double)this.worldPosition.getX() + at[0], (double)this.worldPosition.getY() + at[1], (double)this.worldPosition.getZ() + at[2]};
   }

   private void quacks(ServerLevel server, DuckDerbyRace.Plan plan, long elapsed) {
      List<DuckDerbyRace.Quack> quacks = plan.quacks();
      if (this.quackIndex < 0) {
         for(this.quackIndex = 0; this.quackIndex < quacks.size() && (long)((DuckDerbyRace.Quack)quacks.get(this.quackIndex)).tick() < elapsed - 1L; ++this.quackIndex) {
         }
      }

      DuckDerbyRace.Pose pose = new DuckDerbyRace.Pose();

      while(this.quackIndex < quacks.size() && (long)((DuckDerbyRace.Quack)quacks.get(this.quackIndex)).tick() <= elapsed) {
         DuckDerbyRace.Quack quack = (DuckDerbyRace.Quack)quacks.get(this.quackIndex++);
         plan.pose(quack.lane(), (double)quack.tick(), pose);
         double[] at = this.world(DuckDerbyGeometry.LANE_CENTERS[quack.lane()], (double)16.5F, pose.z);
         server.playSound((Entity)null, at[0], at[1], at[2], RouletteMod.DUCK_QUACK, SoundSource.BLOCKS, quack.volume(), quack.pitch());
      }

   }

   private void splashes(ServerLevel server, DuckDerbyRace.Plan plan, long elapsed) {
      if (elapsed >= 30L && elapsed < (long)plan.returnStartTick() && elapsed % 4L == 0L) {
         DuckDerbyRace.Pose pose = new DuckDerbyRace.Pose();

         for(int lane = 0; lane < 4; ++lane) {
            if (!(plan.pace(lane, (double)elapsed) * (double)46.0F < 0.12)) {
               plan.pose(lane, (double)elapsed, pose);
               double[] at = this.world(DuckDerbyGeometry.LANE_CENTERS[lane], 13.1, pose.z + 3.4);
               server.sendParticles(ParticleTypes.SPLASH, at[0], at[1], at[2], 2, 0.05, (double)0.0F, 0.05, 0.01);
            }
         }

      }
   }

   private void finishLine(ServerLevel server, DuckDerbyRace.Plan plan) {
      double[] at = this.world(DuckDerbyGeometry.LANE_CENTERS[this.winner], (double)17.5F, (double)8.5F);
      server.playSound((Entity)null, at[0], at[1], at[2], RouletteMod.DUCK_FINISH, SoundSource.BLOCKS, 1.1F, 1.0F);
      server.sendParticles(ParticleTypes.FIREWORK, at[0], at[1], at[2], 14, 0.12, 0.12, 0.12, 0.12);
      server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 10, 0.15, 0.15, 0.15, 0.08);
   }

   private void announce(ServerLevel server, DuckDerbyRace.Plan plan) {
      double[] middle = this.world((double)24.0F, (double)20.0F, (double)32.0F);
      if (this.won) {
         server.playSound((Entity)null, middle[0], middle[1], middle[2], RouletteMod.DUCK_WIN, SoundSource.BLOCKS, 1.2F, 1.0F);
         this.confetti(server, plan, (long)plan.resultTick(), (double)1.0F);
      } else if (this.refunded) {
         server.playSound((Entity)null, middle[0], middle[1], middle[2], RouletteMod.DUCK_PUSH, SoundSource.BLOCKS, 0.95F, 1.0F);
         double[] at = this.winnerSpot(plan, (long)plan.resultTick(), (double)6.0F);
         server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 8, 0.12, 0.12, 0.12, 0.06);
      } else {
         server.playSound((Entity)null, middle[0], middle[1], middle[2], RouletteMod.DUCK_LOSE, SoundSource.BLOCKS, 0.95F, 1.0F);
         double[] at = this.winnerSpot(plan, (long)plan.resultTick(), (double)6.0F);
         server.sendParticles(ParticleTypes.FIREWORK, at[0], at[1], at[2], 10, 0.12, 0.12, 0.12, 0.1);
         server.sendParticles(ParticleTypes.END_ROD, at[0], at[1], at[2], 8, 0.12, 0.12, 0.12, 0.06);
      }

   }

   private double[] winnerSpot(DuckDerbyRace.Plan plan, long elapsed, double above) {
      DuckDerbyRace.Pose pose = new DuckDerbyRace.Pose();
      plan.pose(this.winner, (double)elapsed, pose);
      return this.world(DuckDerbyGeometry.LANE_CENTERS[this.winner], (double)12.5F + above, pose.z);
   }

   private void confetti(ServerLevel server, DuckDerbyRace.Plan plan, long elapsed, double strength) {
      double[] duck = this.winnerSpot(plan, elapsed, (double)8.0F);
      int n = (int)Math.round((double)30.0F * strength);
      server.sendParticles(DUCK_CONFETTI[this.winner], duck[0], duck[1], duck[2], n, 0.35, 0.3, 0.35, 0.22);
      server.sendParticles(GOLD_CONFETTI, duck[0], duck[1], duck[2], n, 0.4, 0.3, 0.4, 0.22);
      server.sendParticles(ParticleTypes.FIREWORK, duck[0], duck[1], duck[2], (int)Math.round((double)34.0F * strength), 0.3, 0.3, 0.3, 0.26);
      server.sendParticles(ParticleTypes.END_ROD, duck[0], duck[1], duck[2], (int)Math.round((double)24.0F * strength), 0.3, 0.3, 0.3, 0.2);
      if (strength >= (double)1.0F) {
         server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, duck[0], duck[1], duck[2], 26, 0.3, 0.35, 0.3, 0.3);
      }

      double[] rain = this.world((double)24.0F, (double)24.0F, (double)32.0F);
      server.sendParticles(RED_CONFETTI, rain[0], rain[1], rain[2], n, 1.2, (double)0.25F, (double)1.5F, 0.14);
      server.sendParticles(GOLD_CONFETTI, rain[0], rain[1], rain[2], n, 1.2, (double)0.25F, (double)1.5F, 0.14);
      server.sendParticles(GREEN_CONFETTI, rain[0], rain[1], rain[2], n, 1.2, (double)0.25F, (double)1.5F, 0.14);
   }

   private void eject(ServerLevel server, ItemStack stack) {
      int turns = ((Direction)this.getBlockState().getValue(DuckDerbyBlock.FACING)).get2DDataValue();
      double[] at = this.world((double)24.0F, (double)22.0F, (double)66.5F);
      int[] step = DuckDerbyLayout.facingStep(turns);
      RandomSource random = server.getRandom();
      double side = (random.nextDouble() - (double)0.5F) * 0.16;
      ItemEntity item = new ItemEntity(server, at[0], at[1], at[2], stack);
      item.setDeltaMovement((double)step[0] * 0.2 + (double)step[1] * side, 0.26, (double)step[1] * 0.2 + (double)step[0] * side);
      server.addFreshEntity(item);
   }

   public void onBroken() {
      if (!this.broken) {
         this.broken = true;
         Level var2 = this.level;
         if (var2 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var2;
            this.closeMenus(server);
            DerbyCredits var5 = DerbyCredits.get(server.getServer());

            for(Participant p : this.participants.values()) {
               if (this.enrolling()) {
                  var5.release(p.escrow);
               } else {
                  var5.makeDue(p.escrow);
               }
            }

            this.participants.clear();
            this.enrollmentUntil = -1L;

            for(Map.Entry<UUID, SimpleContainer> e : this.drafts.entrySet()) {
               this.refund((UUID)e.getKey(), (SimpleContainer)e.getValue(), server);
            }

            this.drafts.clear();

            for(ItemStack stack : this.payouts) {
               this.eject(server, stack);
            }

            this.payouts.clear();
            Arrays.fill(this.displayedBets, ItemStack.EMPTY);
            this.displayTicks = 0;

            for(int i = 0; i < 12; ++i) {
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
      this.croupierSettings = (CroupierSettings)input.read("Croupier", CroupierSettings.CODEC).orElse(CroupierSettings.EMPTY);
      this.enrollmentUntil = input.getLongOr("EnrollmentUntil", -1L);
      this.personalRound = input.getBooleanOr("PersonalRound", false);
      this.groupRace = input.getBooleanOr("GroupRace", false);
      this.lastCount = input.getIntOr("GroupCount", 0);
      this.participants.clear();

      for(Participant p : input.read("Participants", DuckDerbyBlockEntity.Participant.CODEC.listOf()).orElse(List.of())) {
         this.participants.put(UUID.fromString(p.owner), p);
      }

      this.drafts.clear();

      for(Draft d : input.read("Drafts", DuckDerbyBlockEntity.Draft.CODEC.listOf()).orElse(List.of())) {
         SimpleContainer own = this.newDraft();
         d.slots.forEach((slot, stackx) -> {
            int i = Integer.parseInt(slot);
            if (i >= 0 && i < 12) {
               own.setItem(i, stackx);
            }

         });
         this.drafts.put(UUID.fromString(d.owner), own);
      }

      this.clientMembers = (List)input.read("DerbyMembers", DerbyLobby.MEMBER_CODEC.listOf()).orElse(List.of());
      this.recoverPersonal = true;
      this.locked = input.getBooleanOr("Locked", false);
      this.won = input.getBooleanOr("Won", false);
      this.refunded = input.getBooleanOr("Refunded", false);
      this.raceStart = input.getLongOr("RaceStart", -1L);
      this.winner = input.getIntOr("Winner", -1);
      this.seed = input.getLongOr("Seed", 0L);
      this.stage = input.getIntOr("Stage", 0);
      this.displayTicks = input.getIntOr("DisplayTicks", 0);
      this.quackIndex = -1;
      this.payouts.clear();

      for(ItemStack stack : input.listOrEmpty("Payouts", ItemStack.CODEC)) {
         this.payouts.add(stack);
      }

      for(int i = 0; i < 12; ++i) {
         this.bets.setItem(i, (ItemStack)input.read("Bet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
         this.displayedBets[i] = (ItemStack)input.read("DisplayBet" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
      }

   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      if (this.machineIdentity.number() > 0) {
         output.store("MachineIdentity", CasinoMachineIdentity.CODEC, this.machineIdentity);
      }

      if (!this.croupierSettings.equals(CroupierSettings.EMPTY)) {
         output.store("Croupier", CroupierSettings.CODEC, this.croupierSettings);
      }

      output.putLong("EnrollmentUntil", this.enrollmentUntil);
      output.putBoolean("PersonalRound", this.personalRound);
      output.putBoolean("GroupRace", this.groupRace);
      output.putInt("GroupCount", this.participantCount());
      output.store("Participants", DuckDerbyBlockEntity.Participant.CODEC.listOf(), List.copyOf(this.participants.values()));
      List<Draft> savedDrafts = new ArrayList();

      for(Map.Entry<UUID, SimpleContainer> e : this.drafts.entrySet()) {
         Map<String, ItemStack> items = new LinkedHashMap();

         for(int i = 0; i < 12; ++i) {
            if (!((SimpleContainer)e.getValue()).getItem(i).isEmpty()) {
               items.put(Integer.toString(i), ((SimpleContainer)e.getValue()).getItem(i).copy());
            }
         }

         if (!items.isEmpty()) {
            savedDrafts.add(new Draft(((UUID)e.getKey()).toString(), items));
         }
      }

      output.store("Drafts", DuckDerbyBlockEntity.Draft.CODEC.listOf(), savedDrafts);
      output.putBoolean("Locked", this.locked);
      output.putBoolean("Won", this.won);
      output.putBoolean("Refunded", this.refunded);
      output.putLong("RaceStart", this.raceStart);
      output.putInt("Winner", this.winner);
      output.putLong("Seed", this.seed);
      output.putInt("Stage", this.stage);
      output.putInt("DisplayTicks", this.displayTicks);
      ValueOutput.TypedOutputList<ItemStack> list = output.<ItemStack>list("Payouts", ItemStack.CODEC);

      for(ItemStack stack : this.payouts) {
         list.add(stack);
      }

      for(int i = 0; i < 12; ++i) {
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
      output.putLong("EnrollmentUntil", this.enrollmentUntil);
      output.putBoolean("GroupRace", this.groupRace);
      output.putBoolean("PersonalRound", this.personalRound);
      output.putBoolean("Locked", this.locked);
      output.putInt("GroupCount", this.participantCount());
      output.store("DerbyMembers", DerbyLobby.MEMBER_CODEC.listOf(), this.members());
      output.putLong("RaceStart", this.raceStart);
      output.putInt("Winner", this.winner);
      output.putLong("Seed", this.seed);

      for(int i = 0; i < 12; ++i) {
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

      return output.buildResult();
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   private static record Participant(String owner, String name, int lane, String escrow, List<ItemStack> stakes) {
      static final Codec<Participant> CODEC = RecordCodecBuilder.<Participant>create((i) -> i.group(Codec.STRING.fieldOf("owner").forGetter(Participant::owner), Codec.STRING.fieldOf("name").forGetter(Participant::name), Codec.INT.fieldOf("lane").forGetter(Participant::lane), Codec.STRING.fieldOf("escrow").forGetter(Participant::escrow), ItemStack.CODEC.listOf().optionalFieldOf("stakes", List.of()).forGetter(Participant::stakes)).apply(i, Participant::new));
   }

   private static record Draft(String owner, Map<String, ItemStack> slots) {
      static final Codec<Draft> CODEC = RecordCodecBuilder.<Draft>create((i) -> i.group(Codec.STRING.fieldOf("owner").forGetter(Draft::owner), Codec.unboundedMap(Codec.STRING, ItemStack.CODEC).fieldOf("slots").forGetter(Draft::slots)).apply(i, Draft::new));
   }
}


