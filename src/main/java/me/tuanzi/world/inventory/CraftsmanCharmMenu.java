package me.tuanzi.world.inventory;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModMenuTypes;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;

import java.util.Optional;

public class CraftsmanCharmMenu extends AbstractContainerMenu {
    private final ItemStack charmStack;
    private final InteractionHand hand;
    private final Player player;
    
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();

    private boolean isLoading = false;

    public CraftsmanCharmMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public CraftsmanCharmMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.CRAFTSMAN_CHARM, containerId);
        this.isLoading = true;
        this.player = playerInventory.player;

        ItemStack foundCharm = ItemStack.EMPTY;
        InteractionHand foundHand = hand;

        if (hand != null) {
            foundCharm = this.player.getItemInHand(hand);
        } else {
            if (this.player.getMainHandItem().is(ModItems.CRAFTSMAN_CHARM)) {
                foundCharm = this.player.getMainHandItem();
                foundHand = InteractionHand.MAIN_HAND;
            } else if (this.player.getOffhandItem().is(ModItems.CRAFTSMAN_CHARM)) {
                foundCharm = this.player.getOffhandItem();
                foundHand = InteractionHand.OFF_HAND;
            }
        }
        
        this.charmStack = foundCharm;
        this.hand = foundHand;

        // 1. 添加结果槽 (Slot 0)
        this.addSlot(new ResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 124, 35));

        // 2. 添加 3x3 合成缓存槽 (Slot 1 到 9)
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int slotIdx = x + y * 3;
                this.addSlot(new Slot(this.craftSlots, slotIdx, 30 + x * 18, 17 + y * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // 护符本身无法放入自己的缓存格
                        return !stack.is(ModItems.CRAFTSMAN_CHARM);
                    }
                });
            }
        }

        // 3. 玩家背包物品栏 (Slot 10 到 36)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }

        // 4. 玩家快捷栏物品栏 (Slot 37 到 45)
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(playerInventory, c, 8 + c * 18, 142));
        }

        // 从护符的 CUSTOM_DATA 中加载已有的 9 个缓存格子物品 (此时所有 Slots 已经就绪)
        if (!this.charmStack.isEmpty()) {
            CustomData customData = this.charmStack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                ListTag listTag = tag.getListOrEmpty("Items");
                for (int i = 0; i < listTag.size(); i++) {
                    CompoundTag itemTag = listTag.getCompoundOrEmpty(i);
                    int slotIdx = itemTag.getByteOr("Slot", (byte) 0) & 255;
                    if (slotIdx >= 0 && slotIdx < 9) {
                        ItemStack loadedStack = ItemStackNbtHelper.read(playerInventory.player.registryAccess(), itemTag.getCompoundOrEmpty("Item"));
                        this.craftSlots.setItem(slotIdx, loadedStack);
                    }
                }
            }
        }

        this.isLoading = false;
        // 完成加载后，手动刷新一次合成结果与同步
        this.slotsChanged(this.craftSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.charmStack.isEmpty() || this.hand == null) {
            return false;
        }
        // 当护符由于各种原因不在玩家原本的格子中时，关闭 GUI。
        // 即校验当前对应手部的物品实例是否还是原来那个
        return player.getItemInHand(this.hand) == this.charmStack;
    }

    @Override
    public void slotsChanged(Container container) {
        if (this.isLoading) {
            return;
        }
        if (this.player.level() instanceof ServerLevel serverLevel && this.player instanceof ServerPlayer serverPlayer) {
            ItemStack result = ItemStack.EMPTY;
            CraftingInput input = this.craftSlots.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> maybeRecipe = serverLevel.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, serverLevel, (RecipeHolder<CraftingRecipe>) null);
            if (maybeRecipe.isPresent()) {
                RecipeHolder<CraftingRecipe> recipeHolder = maybeRecipe.get();
                CraftingRecipe craftingRecipe = recipeHolder.value();
                if (this.resultSlots.setRecipeUsed(serverPlayer, recipeHolder)) {
                    ItemStack recipeResult = craftingRecipe.assemble(input);
                    if (recipeResult.isItemEnabled(serverLevel.enabledFeatures())) {
                        result = recipeResult;
                    }
                }
            }
            this.resultSlots.setItem(0, result);
            this.setRemoteSlot(0, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, result));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (this.charmStack.isEmpty()) return;

        // 将 9 个缓存格子物品序列化保存至护符的 CUSTOM_DATA 中
        CompoundTag tag = new CompoundTag();
        CustomData customData = this.charmStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }

        ListTag listTag = new ListTag();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.craftSlots.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                itemTag.put("Item", ItemStackNbtHelper.write(player.registryAccess(), stack));
                listTag.add(itemTag);
            }
        }
        tag.put("Items", listTag);

        this.charmStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ModLog.debug(player, null, "工匠护符界面关闭，已将 9 个合成格缓存的物品安全保存回护符 NBT 中。");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 0) {
                // 从结果槽取物品
                stack.getItem().onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= 10 && slotIndex < 46) {
                // 从玩家背包/快捷栏移入合成缓存格
                if (stack.is(ModItems.CRAFTSMAN_CHARM)) {
                    // 禁止把工匠护符本身移入合成缓存格
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, 1, 10, false)) {
                    // 如果放不下，在背包与快捷栏之间转移
                    if (slotIndex < 37) {
                        if (!this.moveItemStackTo(stack, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, 10, 46, false)) {
                // 从合成缓存格移入玩家背包/快捷栏
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            if (slotIndex == 0) {
                player.drop(stack, false);
            }
        }
        return clicked;
    }
}
