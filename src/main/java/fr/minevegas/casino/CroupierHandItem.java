package fr.minevegas.casino;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CroupierHandItem extends Item {
   private static final ResourceKey<Item> KEY;
   public static final Item ITEM;

   private CroupierHandItem(Item.Properties properties) {
      super(properties);
   }

   static void init() {
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (player.isCreative()) {
         if (player.isShiftKeyDown() && player instanceof ServerPlayer) {
            ServerPlayer server = (ServerPlayer)player;
            CasinoSettings.edit(server, new CasinoSettings.EditPayload("world", 0L, "forgetcopy", "", "", 0, false));
         }

         return InteractionResult.SUCCESS;
      } else {
         if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.roulettecasino.croupier_hand.creative_only"));
         }

         return InteractionResult.FAIL;
      }
   }

   public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity player) {
      return CasinoMachine.at(level, pos) == null && super.canDestroyBlock(stack, state, level, pos, player);
   }

   public InteractionResult useOn(UseOnContext context) {
      return (InteractionResult)(context.getPlayer() == null ? InteractionResult.PASS : CasinoSettings.interact(context.getPlayer(), context.getLevel(), context.getClickedPos()));
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, lines, flag);
      CompoundTag data = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      String type = (String)data.getString("MineVegasType").orElse("");
      if (CasinoMachine.TYPES.contains(type)) {
         BlockPos pos = BlockPos.of((Long)data.getLong("MineVegasCopyPosition").orElse(0L));
         int number = (Integer)data.getInt("MineVegasCopyNumber").orElse(0);
         Component source = number > 0 ? CasinoMachine.displayName(type, (String)data.getString("MineVegasCopyName").orElse(""), number) : CasinoMachine.typeName(type);
         lines.accept(Component.translatable("item.roulettecasino.croupier_hand.copied", source, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY));
         lines.accept(Component.translatable("item.roulettecasino.croupier_hand.copied_help").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   static {
      KEY = ResourceKey.create(Registries.ITEM, RouletteMod.id("croupier_hand"));
      ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)KEY, new CroupierHandItem((new Item.Properties()).stacksTo(1).rarity(Rarity.UNCOMMON).component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.roulettecasino.croupier_hand.lore").withStyle((style) -> style.withColor(ChatFormatting.GRAY).withItalic(false))))).setId(KEY)));
   }
}

