package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class PokerTableBlockEntity extends BlockEntity implements MenuProvider, CasinoConfiguredMachine, CasinoPayoutMachine, CasinoDealerTable {
   private static final DustParticleOptions RED_CONFETTI = new DustParticleOptions(14629707, 1.15F);
   private static final DustParticleOptions GOLD_CONFETTI = new DustParticleOptions(16765288, 1.25F);
   private static final DustParticleOptions GREEN_CONFETTI = new DustParticleOptions(3594374, 1.15F);
   private static final int BET_TIMEOUT = 2400;
   private static final int PAYOUT_DELAY = 22;
   private static final int PAYOUT_STEP = 3;
   private static final double AIM_X = (double)32.0F;
   private static final double AIM_Y = (double)18.0F;
   private static final double AIM_Z = (double)12.0F;
   private BlackjackShoe shoe;
   private PokerRound round;
   private UUID owner;
   private String ownerName = "";
   private final List<BlackjackTableBlockEntity.Stake> wager = new ArrayList();
   private final List<CasinoExchange.Rule> wagerExchanges = new ArrayList();
   private final ArrayDeque<ItemStack> payouts = new ArrayDeque();
   private final List<PokerMenu.ResultDelta> resultDeltas = new ArrayList();
   private final List<ScheduledSound> sounds = new ArrayList();
   private PokerRound.Result decided;
   private int payoutWait;
   private int betIdle;
   private int endHold;
   private boolean dirty;
   private boolean recoveryPending;
   private boolean broken;
   private boolean aimSet;
   private int aimYaw;
   private int aimPitch;
   private int aimSpread;
   private boolean snapshotStale;
   private final int[] snapshot;
   private PokerTableState view;
   private final List<BlackjackTableBlockEntity.Stake> shownWager;
   private final Events events;
   private CasinoMachineIdentity machineIdentity;
   private CasinoPayoutTarget payoutTarget;
   private CasinoPlayerDisplay panelPlayer;
   private CroupierSettings croupierSettings;
   private DealerAppearance dealerAppearance;

   public PokerTableBlockEntity(BlockPos pos, BlockState state) {
      super(RouletteMod.POKER_ENTITY, pos, state);
      this.decided = PokerRound.Result.NONE;
      this.snapshotStale = true;
      this.snapshot = new int[51];
      this.view = new PokerTableState();
      this.shownWager = new ArrayList();
      this.events = new Events();
      this.machineIdentity = CasinoMachineIdentity.EMPTY;
      this.payoutTarget = CasinoPayoutTarget.EMPTY;
      this.panelPlayer = CasinoPlayerDisplay.EMPTY;
      this.croupierSettings = CroupierSettings.EMPTY;
      this.dealerAppearance = DealerAppearance.EMPTY;
   }

   public PokerTableState view() {
      return this.view;
   }

   public List<BlackjackTableBlockEntity.Stake> shownWager() {
      return this.shownWager;
   }

   public String ownerName() {
      return this.ownerName;
   }

   public boolean isRoundOn() {
      return this.round != null;
   }

   private int turns() {
      return ((Direction)this.getBlockState().getValue(PokerTableBlock.FACING)).get2DDataValue();
   }

   private double[] world(double x, double y, double z) {
      double[] b = BlackjackLayout.toBlock(x, y, z, this.turns());
      return new double[]{(double)this.worldPosition.getX() + b[0], (double)this.worldPosition.getY() + b[1], (double)this.worldPosition.getZ() + b[2]};
   }

   private double[] worldVector(double dx, double dz) {
      for(int i = 0; i < this.turns(); ++i) {
         double nx = -dz;
         dz = dx;
         dx = nx;
      }

      return new double[]{dx, dz};
   }

   public boolean isInRange(Player player) {
      double[] c = this.world((double)32.0F, (double)14.0F, (double)16.0F);
      return player.distanceToSqr(c[0], c[1], c[2]) <= (double)64.0F;
   }

   public CasinoPayoutTarget payoutTarget() {
      return this.payoutTarget;
   }

   public void payoutTarget(CasinoPayoutTarget target) {
      this.payoutTarget = target;
      this.setChanged();
   }

   public CasinoPlayerDisplay panelPlayer() {
      return !this.ownerName.isEmpty() ? this.panelPlayer : CasinoPlayerDisplay.EMPTY;
   }

   public CasinoMachineIdentity machineIdentity() {
      return this.machineIdentity;
   }

   public void machineIdentity(CasinoMachineIdentity value) {
      this.machineIdentity = value;
   }

   public DealerAppearance dealerAppearance() {
      return this.dealerAppearance;
   }

   public void dealerAppearance(DealerAppearance appearance) {
      this.dealerAppearance = appearance;
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }

   }

   public CroupierSettings croupierSettings() {
      return this.croupierSettings;
   }

   public void croupierSettings(CroupierSettings value) {
      this.croupierSettings = value;
   }

   public Component getDisplayName() {
      return Component.translatable("block.roulettecasino.poker_table");
   }

   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      if (player instanceof ServerPlayer server) {
         CasinoSettings.menuOpened(server, id, this);
      }

      if (this.owner != null && this.owner.equals(player.getUUID())) {
         SimpleContainer bets = new SimpleContainer(3);
         if (this.round != null) {
            List<ItemStack> antes = this.anteStakes();

            for(int i = 0; i < antes.size() && i < 3; ++i) {
               bets.setItem(i, (ItemStack)antes.get(i));
            }
         }

         ContainerData data = new ContainerData() {
            {
               Objects.requireNonNull(PokerTableBlockEntity.this);
            }

            public int get(int index) {
               return PokerTableBlockEntity.this.menuData(index);
            }

            public void set(int index, int value) {
            }

            public int getCount() {
               return 51;
            }
         };
         return new PokerMenu(id, inventory, this, bets, data);
      } else {
         return null;
      }
   }

   public void use(ServerPlayer player) {
      if (!this.broken && !this.recoveryPending) {
         if (this.round != null && this.round.isOver() && this.payouts.isEmpty() && player.getUUID().equals(this.owner)) {
            Level var3 = this.level;
            if (var3 instanceof ServerLevel) {
               ServerLevel server = (ServerLevel)var3;
               this.finishRound(server);
            }
         }

         if (this.owner != null && !this.owner.equals(player.getUUID())) {
            boolean var10000;
            label62: {
               ServerPlayer holder = this.ownerPlayer();
               if (holder != null) {
                  AbstractContainerMenu var5 = holder.containerMenu;
                  if (var5 instanceof PokerMenu) {
                     PokerMenu menu = (PokerMenu)var5;
                     if (menu.table() == this) {
                        var10000 = true;
                        break label62;
                     }
                  }
               }

               var10000 = false;
            }

            boolean holds = var10000;
            if (this.round != null || !this.payouts.isEmpty() || holds) {
               player.sendOverlayMessage(Component.translatable("message.roulettecasino.poker.busy", this.ownerName));
               return;
            }

            this.owner = null;
            this.ownerName = "";
         }

         this.owner = player.getUUID();
         this.ownerName = player.getName().getString();
         this.panelPlayer = CasinoPlayerDisplay.of(player);
         this.payoutTarget = CasinoPayoutTarget.of(player);
         this.betIdle = 0;
         if (this.round != null) {
            this.computeAim(player, this.view.seat);
            this.snapshotStale = true;
         }

         label42: {
            player.openMenu(this);
            AbstractContainerMenu var9 = player.containerMenu;
            if (var9 instanceof PokerMenu) {
               PokerMenu menu = (PokerMenu)var9;
               if (menu.table() == this) {
                  break label42;
               }
            }

            if (this.round == null) {
               this.owner = null;
               this.ownerName = "";
            }
         }

         this.sync();
      }
   }

   private ServerPlayer ownerPlayer() {
      if (this.owner != null && this.level != null) {
         Player var2 = this.level.getPlayerByUUID(this.owner);
         ServerPlayer var10000;
         if (var2 instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)var2;
            var10000 = player;
         } else {
            var10000 = null;
         }

         return var10000;
      } else {
         return null;
      }
   }

   public void rigShoe(int... cards) {
      this.shoe = BlackjackShoe.riggedDecks(new Random(7L), 1, cards);
   }

   boolean menuButton(ServerPlayer player, PokerMenu menu, int button) {
      if (!this.broken && this.owner != null && this.owner.equals(player.getUUID())) {
         switch (button) {
            case 0:
               return this.round == null && this.payouts.isEmpty() && this.beginRound(player, menu);
            case 1:
               if (this.round != null && this.round.canCall()) {
                  if (!this.takeCallStake(player)) {
                     player.sendOverlayMessage(Component.translatable("message.roulettecasino.poker.cannot_call"));
                     return false;
                  }

                  if (!this.round.call()) {
                     return false;
                  }

                  return this.changed();
               }

               return false;
            case 2:
               return this.round != null && this.round.fold() && this.changed();
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   private boolean changed() {
      this.dirty = true;
      this.snapshotStale = true;
      return true;
   }

   private List<ItemStack> anteStakes() {
      List<ItemStack> out = new ArrayList();
      boolean called = this.round != null && this.round.called();

      for(BlackjackTableBlockEntity.Stake s : this.wager) {
         out.add(s.proto().copyWithCount(called ? s.count() / 3 : s.count()));
      }

      return out;
   }

   private boolean beginRound(ServerPlayer player, PokerMenu menu) {
      Level var4 = this.level;
      if (!(var4 instanceof ServerLevel server)) {
         return false;
      } else {
         CasinoSettings.returnRejected(this, menu.bets(), player);
         ArrayList<BlackjackTableBlockEntity.Stake> var8 = new ArrayList<>();

         for(int i = 0; i < 3; ++i) {
            ItemStack stack = menu.bets().getItem(i);
            if (!stack.isEmpty() && CasinoSettings.accepts((BlockEntity)this, stack)) {
               var8.add(new BlackjackTableBlockEntity.Stake(stack.copyWithCount(1), stack.getCount()));
            }
         }

         if (var8.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.roulettecasino.poker.no_bet"));
            return false;
         } else {
            for(int i = 0; i < 3; ++i) {
               ItemStack left = menu.bets().removeItemNoUpdate(i);
               if (!left.isEmpty() && !CasinoSettings.accepts((BlockEntity)this, left)) {
                  this.giveBack(player, left);
               }
            }

            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty()) {
               menu.setCarried(ItemStack.EMPTY);
               this.giveBack(player, carried);
            }

            this.wager.clear();
            this.wager.addAll(var8);
            this.wagerExchanges.clear();

            for(BlackjackTableBlockEntity.Stake stake : var8) {
               this.wagerExchanges.add(CasinoSettings.exchange(this, stake.proto()));
            }

            this.shownWager.clear();
            this.shownWager.addAll(var8);
            this.decided = PokerRound.Result.NONE;
            this.payouts.clear();
            this.resultDeltas.clear();
            this.sounds.clear();
            this.endHold = 0;
            this.snapshotStale = true;
            if (this.shoe == null) {
               this.shoe = new BlackjackShoe(new Random(server.getRandom().nextLong()), 1);
            }

            double[] p = this.tablePosition(player);
            int seat = PokerLayout.nearestSeat(p[0], p[1]);
            this.computeAim(player, seat);
            this.view = new PokerTableState(server.getGameTime(), seat);
            this.view.phaseChanged(PokerRound.Phase.DEALING, 0);
            this.round = new PokerRound(this.shoe, this.events, CasinoSettings.bias(this));
            menu.showWager(this.anteStakes());
            this.setChanged();
            this.sync();
            return true;
         }
      }
   }

   private void computeAim(Player player, int seat) {
      double[] target = this.world((double)32.0F, (double)18.0F, (double)12.0F);
      Vec3 eye = player.getEyePosition();
      double dx = target[0] - eye.x;
      double dy = target[1] - eye.y;
      double dz = target[2] - eye.z;
      double flat = Math.sqrt(dx * dx + dz * dz);
      this.aimSet = flat > (double)0.25F;
      if (this.aimSet) {
         double yaw = Math.toDegrees(Math.atan2(-dx, dz));
         this.aimYaw = (int)Math.round(Mth.wrapDegrees(yaw) * (double)10.0F);
         this.aimPitch = (int)Math.round(Math.toDegrees(-Math.atan2(dy, flat)) * (double)10.0F);
         double spread = (double)0.0F;

         for(double[] point : essentialPoints(seat)) {
            double[] w = this.world(point[0], point[1], point[2]);
            double to = Math.toDegrees(Math.atan2(-(w[0] - eye.x), w[2] - eye.z));
            spread = Math.max(spread, Mth.wrapDegrees(to - yaw));
         }

         this.aimSpread = (int)Math.round(Math.min(spread, (double)60.0F) * (double)10.0F);
      }
   }

   private static List<double[]> essentialPoints(int seat) {
      List<double[]> points = new ArrayList();
      points.add(new double[]{(double)50.5F, 15.3, (double)3.0F});

      for(int i = 0; i < 7; ++i) {
         BlackjackLayout.Slot card = PokerLayout.dealerCard(i);
         points.add(new double[]{card.x(), card.y(), card.z()});
      }

      for(int i = 0; i < 2; ++i) {
         BlackjackLayout.Slot card = PokerLayout.playerCard(seat, i);
         points.add(new double[]{card.x(), card.y(), card.z()});
      }

      return points;
   }

   private double[] tablePosition(Player player) {
      double dx = player.getX() - ((double)this.worldPosition.getX() + (double)0.5F);
      double dz = player.getZ() - ((double)this.worldPosition.getZ() + (double)0.5F);

      for(int i = 0; i < this.turns(); ++i) {
         double nx = dz;
         dz = -dx;
         dx = nx;
      }

      return new double[]{(double)24.0F + dx * (double)16.0F, (double)24.0F + dz * (double)16.0F};
   }

   private boolean takeCallStake(ServerPlayer player) {
      List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
      List<ItemStack> kinds = new ArrayList();
      List<Integer> needs = new ArrayList();

      for(BlackjackTableBlockEntity.Stake s : this.wager) {
         int at = -1;

         for(int i = 0; i < kinds.size(); ++i) {
            if (ItemStack.isSameItemSameComponents((ItemStack)kinds.get(i), s.proto())) {
               at = i;
            }
         }

         if (at < 0) {
            kinds.add(s.proto());
            needs.add(2 * s.count());
         } else {
            needs.set(at, (Integer)needs.get(at) + 2 * s.count());
         }
      }

      for(int k = 0; k < kinds.size(); ++k) {
         int have = 0;

         for(ItemStack stack : inventory) {
            if (ItemStack.isSameItemSameComponents((ItemStack)kinds.get(k), stack)) {
               have += stack.getCount();
            }
         }

         if (have < (Integer)needs.get(k)) {
            return false;
         }
      }

      for(int k = 0; k < kinds.size(); ++k) {
         int left = (Integer)needs.get(k);

         for(ItemStack stack : inventory) {
            if (left > 0 && ItemStack.isSameItemSameComponents((ItemStack)kinds.get(k), stack)) {
               int take = Math.min(left, stack.getCount());
               stack.shrink(take);
               left -= take;
            }
         }
      }

      player.getInventory().setChanged();
      player.containerMenu.broadcastChanges();
      List<BlackjackTableBlockEntity.Stake> tripled = new ArrayList();

      for(BlackjackTableBlockEntity.Stake s : this.wager) {
         tripled.add(new BlackjackTableBlockEntity.Stake(s.proto(), s.count() * 3));
      }

      this.wager.clear();
      this.wager.addAll(tripled);
      this.shownWager.clear();
      this.shownWager.addAll(tripled);
      this.setChanged();
      return true;
   }

   void menuClosed(ServerPlayer player, PokerMenu menu) {
      if (this.round == null) {
         for(int i = 0; i < 3; ++i) {
            ItemStack stack = menu.bets().removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
               this.giveBack(player, stack);
            }
         }

         if (this.owner != null && this.owner.equals(player.getUUID()) && this.payouts.isEmpty()) {
            this.owner = null;
            this.ownerName = "";
            this.sync();
         }

      }
   }

   private void giveBack(ServerPlayer player, ItemStack stack) {
      boolean gone = player.isRemoved() && player.getRemovalReason() != RemovalReason.CHANGED_DIMENSION;
      if (!gone && !player.hasDisconnected() && !player.isDeadOrDying()) {
         player.getInventory().add(stack);
         if (!stack.isEmpty()) {
            player.spawnAtLocation(player.level(), stack);
         }

      } else {
         player.drop(stack, false, Prediction.SERVER_ONLY);
      }
   }

   public static void tick(Level world, BlockPos pos, PokerTableBlockEntity table) {
      if (world instanceof ServerLevel server) {
         table.serverTick(server);
      }

   }

   private void serverTick(ServerLevel server) {
      CasinoSettings.identify(this);
      if (this.recoveryPending) {
         this.recoveryPending = false;
         this.recoverInterrupted(server);
      }

      if (this.round != null) {
         this.round.tick();
         this.snapshotStale = true;
         this.playScheduledSounds(server);
         if (this.decided != PokerRound.Result.NONE && !this.payouts.isEmpty() && --this.payoutWait <= 0) {
            this.eject(server, (ItemStack)this.payouts.removeFirst());
            server.playSound((Entity)null, this.worldPosition, RouletteMod.POKER_PAYOUT, SoundSource.BLOCKS, 0.43F, 0.95F + server.getRandom().nextFloat() * 0.2F);
            this.payoutWait = 3;
            this.setChanged();
         }

         if (this.round.isOver() && this.payouts.isEmpty() && ++this.endHold > 14) {
            this.finishRound(server);
         }

         if (this.dirty) {
            this.dirty = false;
            this.sync();
         }

      } else {
         if (this.owner != null && ++this.betIdle > 2400) {
            ServerPlayer holder = this.ownerPlayer();
            this.betIdle = 0;
            if (holder != null) {
               AbstractContainerMenu var4 = holder.containerMenu;
               if (var4 instanceof PokerMenu) {
                  PokerMenu menu = (PokerMenu)var4;
                  if (menu.table() == this) {
                     holder.closeContainer();
                     return;
                  }
               }
            }

            this.owner = null;
            this.ownerName = "";
            this.sync();
         }

      }
   }

   private void finishRound(ServerLevel server) {
      ServerPlayer holder;
      PokerMenu var10000;
      label18: {
         holder = this.ownerPlayer();
         if (holder != null) {
            AbstractContainerMenu var5 = holder.containerMenu;
            if (var5 instanceof PokerMenu) {
               PokerMenu menu = (PokerMenu)var5;
               if (menu.table() == this) {
                  var10000 = menu;
                  break label18;
               }
            }
         }

         var10000 = null;
      }

      PokerMenu open = var10000;
      this.round = null;
      this.wager.clear();
      this.wagerExchanges.clear();
      this.shownWager.clear();
      this.resultDeltas.clear();
      this.decided = PokerRound.Result.NONE;
      this.sounds.clear();
      this.endHold = 0;
      this.aimSet = false;
      this.snapshotStale = true;
      this.view = new PokerTableState();
      this.owner = null;
      this.ownerName = "";
      if (open != null) {
         open.clearBets();
         holder.closeContainer();
      }

      this.setChanged();
      this.sync();
   }

   private void schedule(int at, SoundEvent sound, float volume, float pitch) {
      this.sounds.add(new ScheduledSound(at, sound, volume, pitch));
   }

   private void playScheduledSounds(ServerLevel server) {
      int now = this.round.now();

      for(int i = this.sounds.size() - 1; i >= 0; --i) {
         ScheduledSound s = (ScheduledSound)this.sounds.get(i);
         if (s.at <= now) {
            this.sounds.remove(i);
            server.playSound((Entity)null, this.worldPosition, s.sound, SoundSource.BLOCKS, s.volume, s.pitch);
         }
      }

   }

   private void sync() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel server) {
         server.getChunkSource().blockChanged(this.worldPosition);
      }

   }

   private void addResultDelta(ItemStack prototype, int count) {
      for(int i = 0; i < this.resultDeltas.size(); ++i) {
         PokerMenu.ResultDelta existing = (PokerMenu.ResultDelta)this.resultDeltas.get(i);
         if (ItemStack.isSameItemSameComponents(existing.prototype(), prototype)) {
            this.resultDeltas.set(i, new PokerMenu.ResultDelta(existing.prototype(), existing.count() + count));
            return;
         }
      }

      if (this.resultDeltas.size() >= 6) {
         throw new IllegalStateException("Too many poker balance items");
      } else {
         this.resultDeltas.add(new PokerMenu.ResultDelta(prototype.copyWithCount(1), count));
      }
   }

   private void celebrate(ServerLevel server, boolean big) {
      double[] seat = PokerLayout.seatCenter(this.view.seat);
      double[] p = this.world(seat[0], (double)18.0F, seat[1]);
      int n = big ? 26 : 14;
      server.sendParticles(RED_CONFETTI, p[0], p[1], p[2], n, 0.42, 0.18, 0.42, 0.18);
      server.sendParticles(GOLD_CONFETTI, p[0], p[1], p[2], n, 0.42, 0.18, 0.42, 0.18);
      server.sendParticles(GREEN_CONFETTI, p[0], p[1], p[2], n, 0.42, 0.18, 0.42, 0.18);
      if (big) {
         server.sendParticles(ParticleTypes.FIREWORK, p[0], p[1], p[2], 26, 0.36, 0.22, 0.36, 0.2);
      }

   }

   private void eject(ServerLevel server, ItemStack stack) {
      double[] seat = PokerLayout.seatCenter(this.view.seat);
      double[] p = this.world(seat[0], (double)15.5F, seat[1]);
      ItemEntity item = new ItemEntity(server, p[0], p[1], p[2], stack);
      if (this.owner != null) {
         item.setTarget(this.owner);
      }

      double[] v = this.worldVector((double)0.0F, (double)1.0F);
      double jitter = server.getRandom().nextDouble() * 0.06 - 0.03;
      item.setDeltaMovement(v[0] * 0.13 + jitter, 0.26, v[1] * 0.13 + jitter);
      this.payoutTarget.aim(server, item);
      server.addFreshEntity(item);
   }

   private void ejectAtCenter(ServerLevel server, ItemStack stack) {
      double[] p = this.world((double)32.0F, (double)15.5F, (double)18.0F);
      ItemEntity item = new ItemEntity(server, p[0], p[1], p[2], stack);
      double angle = server.getRandom().nextDouble() * Math.PI * (double)2.0F;
      item.setDeltaMovement(Math.cos(angle) * 0.15, 0.28, Math.sin(angle) * 0.15);
      this.payoutTarget.aim(server, item);
      server.addFreshEntity(item);
   }

   private void ejectStack(ServerLevel server, ItemStack proto, int count) {
      int n;
      for(int size = Math.max(1, proto.getMaxStackSize()); count > 0; count -= n) {
         n = Math.min(size, count);
         this.ejectAtCenter(server, proto.copyWithCount(n));
      }

   }

   public void onTableBroken() {
      if (!this.broken) {
         this.broken = true;
         Level var2 = this.level;
         if (var2 instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)var2;
            if (this.recoveryPending) {
               this.recoveryPending = false;
               this.recoverInterrupted(server);
            }

            for(ServerPlayer player : server.players()) {
               AbstractContainerMenu var5 = player.containerMenu;
               if (var5 instanceof PokerMenu) {
                  PokerMenu menu = (PokerMenu)var5;
                  if (menu.table() == this) {
                     player.closeContainer();
                  }
               }
            }

            this.resultDeltas.clear();
            if (this.round != null) {
               this.round.abort();
            }

            if (this.round != null && this.decided == PokerRound.Result.NONE) {
               for(BlackjackTableBlockEntity.Stake s : this.wager) {
                  this.ejectStack(server, s.proto(), s.count());
               }
            }

            for(ItemStack stack : this.payouts) {
               this.ejectAtCenter(server, stack);
            }

            this.payouts.clear();
            this.wager.clear();
            this.wagerExchanges.clear();
            this.round = null;
            this.owner = null;
            this.setChanged();
         }
      }
   }

   private void recoverInterrupted(ServerLevel server) {
      if (!this.wager.isEmpty() || !this.payouts.isEmpty()) {
         if (this.decided == PokerRound.Result.NONE) {
            for(BlackjackTableBlockEntity.Stake s : this.wager) {
               this.ejectStack(server, s.proto(), s.count());
            }
         }

         for(ItemStack stack : this.payouts) {
            this.ejectAtCenter(server, stack);
         }

         this.payouts.clear();
         this.wager.clear();
         this.wagerExchanges.clear();
         this.shownWager.clear();
         this.resultDeltas.clear();
         this.decided = PokerRound.Result.NONE;
         this.setChanged();
      }
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      this.onTableBroken();
      CasinoSettings.forget(this);
      super.preRemoveSideEffects(pos, state);
   }

   int menuData(int index) {
      if (index >= 0 && index < this.snapshot.length) {
         if (this.snapshotStale) {
            this.snapshotStale = false;
            this.refreshSnapshot();
         }

         return this.snapshot[index];
      } else {
         return 0;
      }
   }

   private void refreshSnapshot() {
      Arrays.fill(this.snapshot, 0);
      int phase = 0;
      if (this.round != null) {
         byte var10000;
         switch (this.round.phase()) {
            case DEALING -> var10000 = 1;
            case DECISION -> var10000 = 2;
            case BOARD -> var10000 = 3;
            case SHOWDOWN -> var10000 = 4;
            default -> var10000 = 5;
         }

         phase = var10000;
         List<Integer> player = this.round.player();
         List<Integer> dealer = this.round.dealerVisible();
         List<Integer> board = this.round.board();
         this.snapshot[1] = Math.min(2, player.size());

         for(int i = 0; i < Math.min(2, player.size()); ++i) {
            this.snapshot[2 + i] = (Integer)player.get(i);
         }

         this.snapshot[4] = Math.min(2, dealer.size());

         for(int i = 0; i < Math.min(2, dealer.size()); ++i) {
            this.snapshot[5 + i] = (Integer)dealer.get(i);
         }

         this.snapshot[7] = Math.min(5, board.size());

         for(int i = 0; i < Math.min(5, board.size()); ++i) {
            this.snapshot[8 + i] = (Integer)board.get(i);
         }

         this.snapshot[13] = this.round.phase() == PokerRound.Phase.DECISION ? (Math.max(0, this.round.deadline() - this.round.now()) + 3) / 4 : 0;
         this.snapshot[14] = this.round.result().code;
         int flags = 0;
         if (this.round.canCall()) {
            flags |= 1;
         }

         if (this.round.canFold()) {
            flags |= 2;
         }

         if (this.round.called()) {
            flags |= 4;
         }

         if (this.aimSet) {
            flags |= 16;
            this.snapshot[17] = this.aimYaw;
            this.snapshot[18] = this.aimPitch;
            this.snapshot[19] = this.aimSpread;
         }

         this.snapshot[15] = flags;
         this.snapshot[16] = this.round.payoutThirds();
      }

      this.snapshot[20] = this.resultDeltas.size();

      for(int i = 0; i < this.resultDeltas.size(); ++i) {
         PokerMenu.ResultDelta delta = (PokerMenu.ResultDelta)this.resultDeltas.get(i);
         int at = 21 + i * 5;

         for(int slot = 0; slot < this.wager.size(); ++slot) {
            if (ItemStack.isSameItemSameComponents(((BlackjackTableBlockEntity.Stake)this.wager.get(slot)).proto(), delta.prototype())) {
               this.snapshot[at] = slot + 1;
               break;
            }
         }

         int item = BuiltInRegistries.ITEM.getId(delta.prototype().getItem());
         this.snapshot[at + 1] = item & '\uffff';
         this.snapshot[at + 2] = item >>> 16;
         this.snapshot[at + 3] = delta.count() & '\uffff';
         this.snapshot[at + 4] = delta.count() >>> 16;
      }

      this.snapshot[0] = phase;
   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.dealerAppearance = (DealerAppearance)input.read("MineVegasDealer", DealerAppearance.CODEC).orElse(DealerAppearance.EMPTY);
      this.machineIdentity = (CasinoMachineIdentity)input.read("MachineIdentity", CasinoMachineIdentity.CODEC).orElse(CasinoMachineIdentity.EMPTY);
      this.panelPlayer = (CasinoPlayerDisplay)input.read("PanelPlayer", CasinoPlayerDisplay.CODEC).orElse(CasinoPlayerDisplay.EMPTY);
      this.payoutTarget = (CasinoPayoutTarget)input.read("PayoutTarget", CasinoPayoutTarget.CODEC).orElse(CasinoPayoutTarget.EMPTY);
      this.croupierSettings = (CroupierSettings)input.read("Croupier", CroupierSettings.CODEC).orElse(CroupierSettings.EMPTY);
      this.resultDeltas.clear();
      this.wagerExchanges.clear();
      this.wager.clear();

      for(BlackjackTableBlockEntity.Stake stake : input.listOrEmpty("Wager", BlackjackTableBlockEntity.Stake.CODEC)) {
         this.wager.add(stake);
      }

      this.payouts.clear();

      for(ItemStack stack : input.listOrEmpty("Payouts", ItemStack.CODEC)) {
         this.payouts.add(stack);
      }

      this.decided = PokerRound.Result.byCode(input.getIntOr("Decided", 0));
      this.recoveryPending = !this.wager.isEmpty() || !this.payouts.isEmpty();
      PokerTableState received = input.read("State", Codec.LONG.listOf()).map(PokerTableState::decode).orElse(null);
      this.view = received != null ? received : new PokerTableState();
      this.ownerName = input.getStringOr("OwnerName", "");
      this.shownWager.clear();

      for(BlackjackTableBlockEntity.Stake stake : input.listOrEmpty("Shown", BlackjackTableBlockEntity.Stake.CODEC)) {
         this.shownWager.add(stake);
      }

   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      if (!this.dealerAppearance.isEmpty()) {
         output.store("MineVegasDealer", DealerAppearance.CODEC, this.dealerAppearance);
      }

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

      ValueOutput.TypedOutputList<BlackjackTableBlockEntity.Stake> stakes = output.<BlackjackTableBlockEntity.Stake>list("Wager", BlackjackTableBlockEntity.Stake.CODEC);

      for(BlackjackTableBlockEntity.Stake stake : this.wager) {
         stakes.add(stake);
      }

      ValueOutput.TypedOutputList<ItemStack> owed = output.<ItemStack>list("Payouts", ItemStack.CODEC);

      for(ItemStack stack : this.payouts) {
         owed.add(stack);
      }

      output.putInt("Decided", this.decided.code);
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
      if (!this.dealerAppearance.isEmpty()) {
         output.store("MineVegasDealer", DealerAppearance.CODEC, this.dealerAppearance);
      }

      output.store("State", Codec.LONG.listOf(), this.view.encode());
      output.putString("OwnerName", this.ownerName);
      ValueOutput.TypedOutputList<BlackjackTableBlockEntity.Stake> stakes = output.<BlackjackTableBlockEntity.Stake>list("Shown", BlackjackTableBlockEntity.Stake.CODEC);

      for(BlackjackTableBlockEntity.Stake stake : this.shownWager) {
         stakes.add(stake);
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

   private static record ScheduledSound(int at, SoundEvent sound, float volume, float pitch) {
   }

   private final class Events implements PokerRound.Listener {
      private Events() {
         Objects.requireNonNull(PokerTableBlockEntity.this);
         super();
      }

      public void shuffled(int at) {
         PokerTableBlockEntity.this.view.shuffled(at);
         PokerTableBlockEntity.this.schedule(at, RouletteMod.POKER_SHUFFLE, 0.9F, 1.0F);
         PokerTableBlockEntity.this.dirty = true;
      }

      public void cardDealt(boolean toDealer, int index, int card, boolean faceUp, int at) {
         PokerTableBlockEntity.this.view.cardDealt(toDealer, index, card, faceUp, at);
         float pitch = 0.94F + (float)Math.min(index, 4) * 0.04F + (toDealer ? 0.05F : 0.0F);
         PokerTableBlockEntity.this.schedule(at + 7, RouletteMod.POKER_FLIP, 0.3F, 0.75F);
         PokerTableBlockEntity.this.schedule(at + 14, RouletteMod.POKER_DEAL, 0.8F, pitch);
         PokerTableBlockEntity.this.dirty = true;
      }

      public void holeCardRevealed(int index, int card, int at) {
         PokerTableBlockEntity.this.view.holeCardRevealed(index, card, at);
         PokerTableBlockEntity.this.schedule(at + 4 + 2, RouletteMod.POKER_FLIP, 0.9F, 1.0F);
         PokerTableBlockEntity.this.dirty = true;
      }

      public void turnOpened(int deadline, int at) {
         PokerTableBlockEntity.this.view.turnOpened(deadline, at);
         PokerTableBlockEntity.this.schedule(at, RouletteMod.POKER_TURN, 0.5F, 1.5F);
         PokerTableBlockEntity.this.dirty = true;
      }

      public void turnTimedOut(int at) {
         ServerPlayer holder = PokerTableBlockEntity.this.ownerPlayer();
         if (holder != null) {
            holder.sendOverlayMessage(Component.translatable("message.roulettecasino.poker.timeout"));
         }

         PokerTableBlockEntity.this.dirty = true;
      }

      public void phaseChanged(PokerRound.Phase phase, int at) {
         PokerTableBlockEntity.this.view.phaseChanged(phase, at);
         PokerTableBlockEntity.this.dirty = true;
      }

      public void decided(PokerRound.Result result, int at) {
         PokerTableBlockEntity.this.view.decided(result, at);
         PokerTableBlockEntity.this.decided = result;
         PokerTableBlockEntity.this.payouts.clear();
         int thirds = PokerTableBlockEntity.this.round != null ? PokerTableBlockEntity.this.round.payoutThirds() : 0;

         for(int i = 0; i < PokerTableBlockEntity.this.wager.size(); ++i) {
            BlackjackTableBlockEntity.Stake stake = (BlackjackTableBlockEntity.Stake)PokerTableBlockEntity.this.wager.get(i);
            int roll = PokerTableBlockEntity.this.level == null ? 0 : PokerTableBlockEntity.this.level.getRandom().nextInt(3);
            CasinoExchange.Rule exchange = i < PokerTableBlockEntity.this.wagerExchanges.size() ? (CasinoExchange.Rule)PokerTableBlockEntity.this.wagerExchanges.get(i) : null;
            if (result.isPush()) {
               PokerTableBlockEntity.this.payouts.addAll(CasinoExchange.payout(stake.display(), 1, 1, roll, exchange, true));
            } else if (result.isWin()) {
               PokerTableBlockEntity.this.payouts.addAll(CasinoExchange.payout(stake.display(), thirds, 3, roll, exchange, false));
            }
         }

         PokerTableBlockEntity.this.resultDeltas.clear();

         for(BlackjackTableBlockEntity.Stake stake : PokerTableBlockEntity.this.wager) {
            PokerTableBlockEntity.this.addResultDelta(stake.proto(), -stake.count());
         }

         for(ItemStack stack : PokerTableBlockEntity.this.payouts) {
            PokerTableBlockEntity.this.addResultDelta(stack, stack.getCount());
         }

         if (PokerTableBlockEntity.this.resultDeltas.stream().anyMatch((d) -> d.count() != 0)) {
            PokerTableBlockEntity.this.resultDeltas.removeIf((d) -> d.count() == 0);
         }

         PokerTableBlockEntity.this.payoutWait = 22;
         Level var13 = PokerTableBlockEntity.this.level;
         if (var13 instanceof ServerLevel server) {
            if (result.isWin()) {
               PokerTableBlockEntity.this.schedule(at + 6, RouletteMod.POKER_WIN, 1.1F, 1.0F);
               PokerTableBlockEntity.this.celebrate(server, PokerTableBlockEntity.this.round != null && PokerHands.antePays(PokerTableBlockEntity.this.round.playerScore()) >= 3);
            } else if (result.isPush()) {
               PokerTableBlockEntity.this.schedule(at + 6, RouletteMod.POKER_PUSH, 0.95F, 1.0F);
            } else {
               PokerTableBlockEntity.this.schedule(at + 6, RouletteMod.POKER_LOSE, 0.95F, 1.0F);
            }
         }

         PokerTableBlockEntity.this.setChanged();
         PokerTableBlockEntity.this.dirty = true;
      }

      public void reaction(BlackjackRound.Reaction reaction, int at) {
         PokerTableBlockEntity.this.view.reaction(reaction, at);
         PokerTableBlockEntity.this.dirty = true;
      }

      public void cleared(int at) {
         PokerTableBlockEntity.this.view.cleared(at);
         PokerTableBlockEntity.this.schedule(at + 7, RouletteMod.POKER_DEAL, 0.55F, 0.8F);
         PokerTableBlockEntity.this.dirty = true;
      }
   }
}


