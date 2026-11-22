package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlackjackRound {
   public static final int DEAL_ACTION = 22;
   public static final int RELEASE = 14;
   public static final int FLIGHT = 9;
   public static final int LANDING = 23;
   public static final int FLIP = 16;
   public static final int PEEK = 28;
   public static final int OPENING = 14;
   public static final int SHUFFLE = 36;
   public static final int TURN_TIMEOUT = 600;
   public static final int CLEAR = 18;
   public static final int END_HOLD = 14;
   public static final int REACTION = 34;
   private final BlackjackShoe shoe;
   private final Listener listener;
   private final double bias;
   private final List<Integer> player;
   private final List<Integer> dealer;
   private final List<Step> steps;
   private int sequence;
   private int now;
   private int deadline;
   private Phase phase;
   private Result result;
   private boolean holeRevealed;
   private boolean doubled;
   private boolean playerBust;

   public static int resultHold(Reaction reaction) {
      byte var10000;
      switch (reaction.ordinal()) {
         case 0 -> var10000 = 36;
         case 1 -> var10000 = 34;
         case 2 -> var10000 = 32;
         case 3 -> var10000 = 34;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public BlackjackRound(BlackjackShoe shoe, Listener listener) {
      this(shoe, listener, 0);
   }

   public BlackjackRound(BlackjackShoe shoe, Listener listener, int bias) {
      this(shoe, listener, (double) bias);
   }

   public BlackjackRound(BlackjackShoe shoe, Listener listener, double bias) {
      this.player = new ArrayList();
      this.dealer = new ArrayList();
      this.steps = new ArrayList();
      this.phase = BlackjackRound.Phase.DEALING;
      this.result = BlackjackRound.Result.NONE;
      this.shoe = shoe;
      this.listener = listener;
      this.bias = Math.max(-100.0D, Math.min(100.0D, bias));
      int t = 0;
      if (shoe.needsShuffle()) {
         shoe.shuffle();
         listener.shuffled(0);
         t += 36;
      }

      t += 14;
      this.at(t, () -> this.deal(false, true));
      t += 22;
      this.at(t, () -> this.deal(true, true));
      t += 22;
      this.at(t, () -> this.deal(false, true));
      t += 22;
      this.at(t, () -> this.deal(true, false));
      this.at(t + 23 + 4, this::checkNaturals);
   }

   public Phase phase() {
      return this.phase;
   }

   public Result result() {
      return this.result;
   }

   public int now() {
      return this.now;
   }

   public int deadline() {
      return this.deadline;
   }

   public boolean doubled() {
      return this.doubled;
   }

   public boolean holeRevealed() {
      return this.holeRevealed;
   }

   public boolean isOver() {
      return this.phase == BlackjackRound.Phase.DONE;
   }

   public List<Integer> player() {
      return Collections.unmodifiableList(this.player);
   }

   public List<Integer> dealer() {
      return Collections.unmodifiableList(this.dealer);
   }

   public List<Integer> dealerVisible() {
      List<Integer> out = new ArrayList(this.dealer);
      if (!this.holeRevealed && out.size() >= 2) {
         out.set(1, -1);
      }

      return out;
   }

   public int playerTotal() {
      return BlackjackCards.total(this.player);
   }

   public int dealerVisibleTotal() {
      return BlackjackCards.total(this.dealerVisible());
   }

   public boolean canHit() {
      return this.phase == BlackjackRound.Phase.PLAYER_TURN;
   }

   public boolean canStand() {
      return this.phase == BlackjackRound.Phase.PLAYER_TURN;
   }

   public boolean canDouble() {
      return this.phase == BlackjackRound.Phase.PLAYER_TURN && this.player.size() == 2 && !this.doubled;
   }

   public boolean hit() {
      if (!this.canHit()) {
         return false;
      } else {
         this.drawForPlayer();
         return true;
      }
   }

   public boolean stand() {
      if (!this.canStand()) {
         return false;
      } else {
         this.dealerTurn();
         return true;
      }
   }

   public boolean doubleDown() {
      if (!this.canDouble()) {
         return false;
      } else {
         this.doubled = true;
         this.drawForPlayer();
         return true;
      }
   }

   public void abort() {
      this.steps.clear();
      this.phase = BlackjackRound.Phase.DONE;
   }

   public void tick() {
      if (this.phase != BlackjackRound.Phase.DONE) {
         ++this.now;
         if (this.phase == BlackjackRound.Phase.PLAYER_TURN && this.now >= this.deadline) {
            this.listener.turnTimedOut(this.now);
            this.stand();
         }

         do {
            Step due = null;

            for(Step s : this.steps) {
               if (s.at <= this.now && (due == null || s.at < due.at || s.at == due.at && s.order < due.order)) {
                  due = s;
               }
            }

            if (due == null) {
               return;
            }

            this.steps.remove(due);
            due.run.run();
         } while(this.phase != BlackjackRound.Phase.DONE);

      }
   }

   private void at(int time, Runnable run) {
      this.steps.add(new Step(time, this.sequence++, run));
   }

   private void after(int delay, Runnable run) {
      this.at(this.now + delay, run);
   }

   private void setPhase(Phase next) {
      this.phase = next;
      this.listener.phaseChanged(next, this.now);
   }

   private void deal(boolean toDealer, boolean faceUp) {
      int card = this.bias == 0 ? this.shoe.draw() : this.shoe.drawFavouring(Math.abs(this.bias), (c) -> {
         double value = BlackjackBias.playerValue(this.player, this.dealer, toDealer, this.doubled, c);
         return this.bias > 0 ? value : -value;
      });
      List<Integer> hand = toDealer ? this.dealer : this.player;
      hand.add(card);
      this.listener.cardDealt(toDealer, hand.size() - 1, faceUp ? card : -1, faceUp, this.now);
   }

   private void checkNaturals() {
      boolean playerNatural = BlackjackCards.isBlackjack(this.player);
      boolean dealerNatural = BlackjackCards.isBlackjack(this.dealer);
      int up = (Integer)this.dealer.get(0);
      if (!BlackjackCards.isAce(up) && !BlackjackCards.isTenValue(up)) {
         if (playerNatural) {
            this.revealThenResolve();
         } else {
            this.openTurn();
         }
      } else {
         this.listener.dealerPeek(dealerNatural, this.now);
         this.after(28, () -> {
            if (!dealerNatural && !playerNatural) {
               this.openTurn();
            } else {
               this.revealThenResolve();
            }

         });
      }

   }

   private void openTurn() {
      this.setPhase(BlackjackRound.Phase.PLAYER_TURN);
      this.deadline = this.now + 600;
      this.listener.turnOpened(this.deadline, this.now);
   }

   private void drawForPlayer() {
      this.setPhase(BlackjackRound.Phase.PLAYER_DRAW);
      this.deal(false, true);
      this.after(27, () -> {
         int total = this.playerTotal();
         if (total > 21) {
            this.playerBust = true;
            this.revealThenResolve();
         } else if (total != 21 && !this.doubled) {
            this.openTurn();
         } else {
            this.dealerTurn();
         }

      });
   }

   private void reveal() {
      if (!this.holeRevealed) {
         this.holeRevealed = true;
         this.listener.holeCardRevealed((Integer)this.dealer.get(1), this.now);
      }
   }

   private void revealThenResolve() {
      this.setPhase(BlackjackRound.Phase.DEALER_TURN);
      this.reveal();
      this.after(24, this::resolve);
   }

   private void dealerTurn() {
      this.setPhase(BlackjackRound.Phase.DEALER_TURN);
      this.reveal();
      this.after(24, this::dealerPlay);
   }

   private void dealerPlay() {
      if (BlackjackCards.total(this.dealer) < 17) {
         this.deal(true, true);
         this.after(29, this::dealerPlay);
      } else {
         this.resolve();
      }

   }

   private void resolve() {
      boolean playerNatural = BlackjackCards.isBlackjack(this.player);
      boolean dealerNatural = BlackjackCards.isBlackjack(this.dealer);
      int p = BlackjackCards.total(this.player);
      int d = BlackjackCards.total(this.dealer);
      if (this.playerBust) {
         this.result = BlackjackRound.Result.LOSE_BUST;
      } else if (playerNatural && dealerNatural) {
         this.result = BlackjackRound.Result.PUSH_BLACKJACKS;
      } else if (dealerNatural) {
         this.result = BlackjackRound.Result.LOSE_DEALER_BLACKJACK;
      } else if (playerNatural) {
         this.result = BlackjackRound.Result.BLACKJACK;
      } else if (d > 21) {
         this.result = BlackjackRound.Result.WIN_DEALER_BUST;
      } else if (p <= d && (p != 21 || d != 21)) {
         if (p < d) {
            this.result = BlackjackRound.Result.LOSE;
         } else {
            this.result = BlackjackRound.Result.PUSH;
         }
      } else {
         this.result = BlackjackRound.Result.WIN;
      }

      Reaction reaction = this.result == BlackjackRound.Result.BLACKJACK ? BlackjackRound.Reaction.CELEBRATE : (this.result.isWin() ? BlackjackRound.Reaction.PAY : (this.result.isPush() ? BlackjackRound.Reaction.PUSH : BlackjackRound.Reaction.COLLECT));
      this.setPhase(BlackjackRound.Phase.RESULT);
      this.listener.decided(this.result, this.now);
      this.listener.reaction(reaction, this.now);
      this.after(resultHold(reaction), () -> {
         this.setPhase(BlackjackRound.Phase.CLEARING);
         this.listener.cleared(this.now);
         this.after(18, () -> this.setPhase(BlackjackRound.Phase.DONE));
      });
   }

   public static enum Phase {
      DEALING,
      PLAYER_TURN,
      PLAYER_DRAW,
      DEALER_TURN,
      RESULT,
      CLEARING,
      DONE;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{DEALING, PLAYER_TURN, PLAYER_DRAW, DEALER_TURN, RESULT, CLEARING, DONE};
      }
   }

   public static enum Result {
      NONE(0, 0),
      WIN(1, 4),
      WIN_DEALER_BUST(2, 4),
      BLACKJACK(3, 5),
      LOSE(4, 0),
      LOSE_BUST(5, 0),
      LOSE_DEALER_BLACKJACK(6, 0),
      PUSH(7, 2),
      PUSH_BLACKJACKS(8, 2);

      public final int code;
      public final int halves;

      private Result(int code, int halves) {
         this.code = code;
         this.halves = halves;
      }

      public boolean isWin() {
         return this.halves > 2;
      }

      public boolean isPush() {
         return this.halves == 2;
      }

      public boolean isLoss() {
         return this != NONE && this.halves == 0;
      }

      public static Result byCode(int code) {
         for(Result r : values()) {
            if (r.code == code) {
               return r;
            }
         }

         return NONE;
      }

      // $FF: synthetic method
      private static Result[] $values() {
         return new Result[]{NONE, WIN, WIN_DEALER_BUST, BLACKJACK, LOSE, LOSE_BUST, LOSE_DEALER_BLACKJACK, PUSH, PUSH_BLACKJACKS};
      }
   }

   public static enum Reaction {
      PAY,
      COLLECT,
      PUSH,
      CELEBRATE;

      // $FF: synthetic method
      private static Reaction[] $values() {
         return new Reaction[]{PAY, COLLECT, PUSH, CELEBRATE};
      }
   }

   public interface Listener {
      default void shuffled(int at) {
      }

      void cardDealt(boolean var1, int var2, int var3, boolean var4, int var5);

      void holeCardRevealed(int var1, int var2);

      default void dealerPeek(boolean blackjack, int at) {
      }

      void turnOpened(int var1, int var2);

      default void turnTimedOut(int at) {
      }

      void phaseChanged(Phase var1, int var2);

      void decided(Result var1, int var2);

      void reaction(Reaction var1, int var2);

      void cleared(int var1);
   }

   private static record Step(int at, int order, Runnable run) {
   }
}
