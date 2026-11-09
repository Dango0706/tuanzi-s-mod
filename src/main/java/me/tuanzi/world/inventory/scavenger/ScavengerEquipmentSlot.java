package me.tuanzi.world.inventory.scavenger;

import me.tuanzi.client.scavenger.ScavengerClientState;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 战术背囊槽与黑匣安全箱槽位。
 * 支持类型校验与“非空不可卸下”拾荒防掉落保护。
 */
public class ScavengerEquipmentSlot extends Slot {

    private final EquipmentGridSpec.SlotType expectedType;
    private final Player player;

    public ScavengerEquipmentSlot(
            Container container,
            int slot,
            int x,
            int y,
            EquipmentGridSpec.SlotType expectedType,
            Player player
    ) {
        super(container, slot, x, y);
        this.expectedType = expectedType;
        this.player = player;
    }

    public EquipmentGridSpec.SlotType getExpectedType() {
        return this.expectedType;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (this.expectedType == EquipmentGridSpec.SlotType.SAFEBOX) {
            // 安全箱不可随意拖拽放入覆盖，只能通过点击安全箱槽呼出选择器进行规格选择切换
            return false;
        }
        return PlayerCorridorProfile.resolveEquipmentSpec(stack, this.expectedType).isPresent();
    }

    @Override
    public boolean mayPickup(Player player) {
        // 安全箱不可被取出，死亡绝对不掉落，只能在槽位点击时进行规格选择
        if (this.expectedType == EquipmentGridSpec.SlotType.SAFEBOX) {
            return false;
        }
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer && serverPlayer instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
            return !profile.hasBackpack() || profile.backpackGrid().isEmpty();
        }
        return ScavengerClientState.getBackpackEntries().isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
