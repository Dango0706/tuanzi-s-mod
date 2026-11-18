package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntToDoubleFunction;

public final class BlackjackShoe {
   public static final int DECKS = 6;
   public static final int RESHUFFLE_BELOW = 60;
   private final Random random;
   private final int decks;
   private final List<Integer> cards;
   private final List<Integer> rigged;

   public BlackjackShoe(Random random) {
      this(random, 6);
   }

   public BlackjackShoe(Random random, int decks) {
      this.cards = new ArrayList();
      this.rigged = new ArrayList();
      this.random = random;
      this.decks = Math.max(1, decks);
      this.shuffle();
   }

   public static BlackjackShoe rigged(Random random, int... first) {
      return riggedDecks(random, 6, first);
   }

   public static BlackjackShoe riggedDecks(Random random, int decks, int... first) {
      BlackjackShoe shoe = new BlackjackShoe(random, decks);

      for(int card : first) {
         shoe.rigged.add(card);
      }

      return shoe;
   }

   public void shuffle() {
      this.cards.clear();

      for(int deck = 0; deck < this.decks; ++deck) {
         for(int card = 0; card < 52; ++card) {
            this.cards.add(card);
         }
      }

      for(int i = this.cards.size() - 1; i > 0; --i) {
         int j = this.random.nextInt(i + 1);
         int swap = (Integer)this.cards.get(i);
         this.cards.set(i, (Integer)this.cards.get(j));
         this.cards.set(j, swap);
      }

   }

   public boolean needsShuffle() {
      return this.cards.size() < 60;
   }

   public int remaining() {
      return this.cards.size();
   }

   public int draw() {
      if (!this.rigged.isEmpty()) {
         return (Integer)this.rigged.removeFirst();
      } else {
         if (this.cards.isEmpty()) {
            this.shuffle();
         }

         return (Integer)this.cards.removeLast();
      }
   }

   public int drawFavouring(int percent, IntToDoubleFunction score) {
      if (this.rigged.isEmpty() && this.cards.size() >= 2) {
         if (this.random.nextInt(100) >= percent) {
            return (Integer)this.cards.removeLast();
         } else {
            int top = (Integer)this.cards.get(this.cards.size() - 1);
            int second = (Integer)this.cards.get(this.cards.size() - 2);
            if (score.applyAsDouble(second) <= score.applyAsDouble(top)) {
               return (Integer)this.cards.removeLast();
            } else {
               this.cards.remove(this.cards.size() - 2);
               return second;
            }
         }
      } else {
         return this.draw();
      }
   }
}
