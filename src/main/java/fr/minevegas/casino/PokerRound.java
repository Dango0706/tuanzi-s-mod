package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PokerRound {
   public static final int BOARD = 5;
   public static final int FIRST_BOARD = 2;
   public static final int SECOND_FLIP = 22;
   public static final int TURN_TIMEOUT = 600;
   private final BlackjackShoe shoe;
   private final Listener listener;
   private final int bias;
   private final List<Integer> player;
   private final List<Integer> dealer;
   private final List<Integer> board;
   private final List<Step> steps;
   private final boolean[] revealed;
   private int sequence;
   private int now;
   private int deadline;
   private Phase phase;
   private Result result;
   private boolean called;
   private int playerScore;
   private int dealerScore;

   public PokerRound(BlackjackShoe shoe, Listener listener) {
      this(shoe, listener, 0);
   }

   public PokerRound(BlackjackShoe shoe, Listener listener, int bias) {
      this.player = new ArrayList();
      this.dealer = new ArrayList();
      this.board = new ArrayList();
      this.steps = new ArrayList();
      this.revealed = new boolean[2];
      this.phase = PokerRound.Phase.DEALING;
      this.result = PokerRound.Result.NONE;
      this.playerScore = -1;
      this.dealerScore = -1;
      this.shoe = shoe;
      this.listener = listener;
      this.bias = Math.max(-100, Math.min(100, bias));
      int t = 0;
      if (shoe.needsShuffle()) {
         shoe.shuffle();
         listener.shuffled(0);
         t += 36;
      }

      t += 14;

      for(int i = 0; i < 2; ++i) {
         this.at(t, () -> this.deal(PokerRound.Target.PLAYER));
         t += 22;
         this.at(t, () -> this.deal(PokerRound.Target.DEALER));
         t += 22;
      }

      for(int i = 0; i < 3; ++i) {
         this.at(t, () -> this.deal(PokerRound.Target.BOARD));
         if (i < 2) {
            t += 22;
         }
      }

      this.at(t + 23 + 4, this::openDecision);
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

   public boolean called() {
      return this.called;
   }

   public boolean isOver() {
      return this.phase == PokerRound.Phase.DONE;
   }

   public List<Integer> player() {
      return Collections.unmodifiableList(this.player);
   }

   public List<Integer> dealer() {
      return Collections.unmodifiableList(this.dealer);
   }

   public List<Integer> dealerVisible() {
      List<Integer> out = new ArrayList(this.dealer);

      for(int i = 0; i < out.size(); ++i) {
         if (!this.revealed[i]) {
            out.set(i, -1);
         }
      }

      return out;
   }

   public List<Integer> board() {
      return Collections.unmodifiableList(this.board);
   }

   public int playerScore() {
      return this.playerScore;
   }

   public int dealerScore() {
      return this.dealerScore;
   }

   public boolean canCall() {
      return this.phase == PokerRound.Phase.DECISION;
   }

   public boolean canFold() {
      return this.phase == PokerRound.Phase.DECISION;
   }

   public int payoutThirds() {
      int var10000;
      switch (this.result.ordinal()) {
         case 1 -> var10000 = 5 + PokerHands.antePays(this.playerScore);
         case 2 -> var10000 = 3 + PokerHands.antePays(this.playerScore);
         case 3 -> var10000 = 3;
         default -> var10000 = 0;
      }

      return var10000;
   }

   public boolean call() {
      if (!this.canCall()) {
         return false;
      } else {
         this.called = true;
         this.setPhase(PokerRound.Phase.BOARD);
         this.deal(PokerRound.Target.BOARD);
         this.after(22, () -> this.deal(PokerRound.Target.BOARD));
         this.after(49, this::showdown);
         return true;
      }
   }

   public boolean fold() {
      if (!this.canFold()) {
         return false;
      } else {
         this.decide(PokerRound.Result.FOLD, BlackjackRound.Reaction.COLLECT);
         return true;
      }
   }

   public void abort() {
      this.steps.clear();
      this.phase = PokerRound.Phase.DONE;
   }

   public void tick() {
      if (this.phase != PokerRound.Phase.DONE) {
         ++this.now;
         if (this.phase == PokerRound.Phase.DECISION && this.now >= this.deadline) {
            this.listener.turnTimedOut(this.now);
            this.fold();
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
         } while(this.phase != PokerRound.Phase.DONE);

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

   private void deal(Target target) {
      int card = this.bias == 0 ? this.shoe.draw() : this.shoe.drawFavouring(Math.abs(this.bias), (c) -> {
         double value = PokerBias.playerValue(this.player, this.dealer, this.board, target.ordinal(), c);
         return this.bias > 0 ? value : -value;
      });
      switch (target.ordinal()) {
         case 0:
            this.player.add(card);
            this.listener.cardDealt(false, this.player.size() - 1, card, true, this.now);
            break;
         case 1:
            this.dealer.add(card);
            this.listener.cardDealt(true, this.dealer.size() - 1, -1, false, this.now);
            break;
         case 2:
            this.board.add(card);
            this.listener.cardDealt(true, 2 + this.board.size() - 1, card, true, this.now);
      }

   }

   private void openDecision() {
      this.setPhase(PokerRound.Phase.DECISION);
      this.deadline = this.now + 600;
      this.listener.turnOpened(this.deadline, this.now);
   }

   private void reveal(int index) {
      if (!this.revealed[index]) {
         this.revealed[index] = true;
         this.listener.holeCardRevealed(index, (Integer)this.dealer.get(index), this.now);
      }
   }

   private void showdown() {
      this.setPhase(PokerRound.Phase.SHOWDOWN);
      this.reveal(1);
      this.after(22, () -> this.reveal(0));
      this.after(46, this::resolve);
   }

   private void resolve() {
      List<Integer> mine = new ArrayList(this.player);
      mine.addAll(this.board);
      List<Integer> his = new ArrayList(this.dealer);
      his.addAll(this.board);
      this.playerScore = PokerHands.score(mine);
      this.dealerScore = PokerHands.score(his);
      Result r;
      if (!PokerHands.qualifies(this.dealerScore)) {
         r = PokerRound.Result.NO_QUALIFY;
      } else if (this.playerScore > this.dealerScore) {
         r = PokerRound.Result.WIN;
      } else if (this.playerScore < this.dealerScore) {
         r = PokerRound.Result.LOSE;
      } else {
         r = PokerRound.Result.PUSH;
      }

      BlackjackRound.Reaction reaction = r.isWin() ? (PokerHands.antePays(this.playerScore) >= 3 ? BlackjackRound.Reaction.CELEBRATE : BlackjackRound.Reaction.PAY) : (r.isPush() ? BlackjackRound.Reaction.PUSH : BlackjackRound.Reaction.COLLECT);
      this.decide(r, reaction);
   }

   private void decide(Result r, BlackjackRound.Reaction reaction) {
      this.result = r;
      this.setPhase(PokerRound.Phase.RESULT);
      this.listener.decided(r, this.now);
      this.listener.reaction(reaction, this.now);
      this.after(BlackjackRound.resultHold(reaction), () -> {
         this.setPhase(PokerRound.Phase.CLEARING);
         this.listener.cleared(this.now);
         this.after(18, () -> this.setPhase(PokerRound.Phase.DONE));
      });
   }

   public static enum Phase {
      DEALING,
      DECISION,
      BOARD,
      SHOWDOWN,
      RESULT,
      CLEARING,
      DONE;

      // $FF: synthetic method
      private static Phase[] $values() {
         return new Phase[]{DEALING, DECISION, BOARD, SHOWDOWN, RESULT, CLEARING, DONE};
      }
   }

   public static enum Result {
      NONE(0),
      WIN(1),
      NO_QUALIFY(2),
      PUSH(3),
      LOSE(4),
      FOLD(5);

      public final int code;

      private Result(int code) {
         this.code = code;
      }

      public boolean isWin() {
         return this == WIN || this == NO_QUALIFY;
      }

      public boolean isPush() {
         return this == PUSH;
      }

      public boolean isLoss() {
         return this == LOSE || this == FOLD;
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
         return new Result[]{NONE, WIN, NO_QUALIFY, PUSH, LOSE, FOLD};
      }
   }

   public interface Listener {
      default void shuffled(int at) {
      }

      void cardDealt(boolean var1, int var2, int var3, boolean var4, int var5);

      void holeCardRevealed(int var1, int var2, int var3);

      void turnOpened(int var1, int var2);

      default void turnTimedOut(int at) {
      }

      void phaseChanged(Phase var1, int var2);

      void decided(Result var1, int var2);

      void reaction(BlackjackRound.Reaction var1, int var2);

      void cleared(int var1);
   }

   private static record Step(int at, int order, Runnable run) {
   }

   private static enum Target {
      PLAYER,
      DEALER,
      BOARD;

      // $FF: synthetic method
      private static Target[] $values() {
         return new Target[]{PLAYER, DEALER, BOARD};
      }
   }
}
