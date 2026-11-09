package me.tuanzi.client.gui.screens.scavenger;

import me.tuanzi.client.scavenger.ScavengerClientState;
import me.tuanzi.init.ModItems;
import me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 战术黑匣安全箱规格切换悬浮选择面板。
 * 当玩家点击安全箱槽位时呼出，支持在持有或解锁的安全箱规格间一键切换。
 */
public class SafeboxSelectorOverlay {

    public record SafeboxTier(int tierIndex, String name, String spec, Item item, int colorRgb) {}

    public static final List<SafeboxTier> TIERS = List.of(
            new SafeboxTier(0, "原型黑匣保险舱", "1×2 规格", ModItems.PROTO_LOCKBOX_1X2, 0xFFAAAAAA),
            new SafeboxTier(1, "强化黑匣保险舱", "2×2 规格", ModItems.REINFORCED_SAFEBOX_2X2, 0xFF55FF55),
            new SafeboxTier(2, "保密黑匣保险舱", "2×3 规格", ModItems.CIPHER_SAFEBOX_2X3, 0xFF55FFFF),
            new SafeboxTier(3, "宙斯盾核心保险舱", "3×3 规格", ModItems.AEGIS_CORE_VAULT_3X3, 0xFFFF5555)
    );

    private boolean open = false;
    private int x;
    private int y;
    private final int width = 190;
    private final int height = 112;

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void toggle(int anchorX, int anchorY, int screenWidth, int screenHeight) {
        this.open = !this.open;
        if (this.open) {
            // 计算最佳弹出位置（保证不超出屏幕）
            this.x = Math.max(10, Math.min(anchorX, screenWidth - this.width - 10));
            this.y = Math.max(10, Math.min(anchorY, screenHeight - this.height - 10));
        }
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (!this.open) return;

        // 1. 半透明遮罩底色与科技边框
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0xF5101620);
        graphics.fill(this.x, this.y, this.x + this.width, this.y + 1, 0xFF00E5FF);
        graphics.fill(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0xFF00E5FF);
        graphics.fill(this.x, this.y, this.x + 1, this.y + this.height, 0xFF00E5FF);
        graphics.fill(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, 0xFF00E5FF);

        // 2. 标题与关闭按钮
        graphics.text(font, "🔒 黑匣安全箱规格切换", this.x + 8, this.y + 6, 0xFFFFD700, true);
        boolean closeHovered = mouseX >= this.x + this.width - 16 && mouseX <= this.x + this.width - 4 && mouseY >= this.y + 4 && mouseY <= this.y + 16;
        graphics.text(font, "✕", this.x + this.width - 14, this.y + 6, closeHovered ? 0xFFFF4444 : 0xFF888888, false);

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        boolean isCreative = player != null && player.isCreative();
        ItemStack equippedSafebox = ScavengerClientState.getEquippedSafebox();

        // 3. 四阶安全箱选项列表
        int rowY = this.y + 20;
        for (SafeboxTier tier : TIERS) {
            boolean isCurrent = !equippedSafebox.isEmpty() && equippedSafebox.is(tier.item());
            boolean isOwned = isCreative || isPlayerHoldingSafebox(tier.item(), player);

            boolean isHovered = mouseX >= this.x + 4 && mouseX <= this.x + this.width - 4 && mouseY >= rowY && mouseY <= rowY + 20;

            // 背景条
            int bgCol = isCurrent ? 0x4000E5FF : (isHovered && isOwned ? 0x30FFFFFF : 0x18FFFFFF);
            graphics.fill(this.x + 4, rowY, this.x + this.width - 4, rowY + 20, bgCol);
            graphics.fill(this.x + 4, rowY, this.x + this.width - 4, rowY + 1, isCurrent ? 0x8000E5FF : 0x20384656);

            // 物品图标
            graphics.item(new ItemStack(tier.item()), this.x + 6, rowY + 2);

            // 名称与规格
            graphics.text(font, tier.name(), this.x + 26, rowY + 3, tier.colorRgb(), false);
            graphics.text(font, tier.spec(), this.x + 26, rowY + 12, 0xFF8899A6, false);

            // 状态 / 更换按钮
            if (isCurrent) {
                graphics.text(font, "● 当前装配", this.x + this.width - 56, rowY + 6, 0xFF2ECC71, false);
            } else if (isOwned) {
                int btnCol = isHovered ? 0xFF00E5FF : 0xFF3498DB;
                graphics.fill(this.x + this.width - 44, rowY + 4, this.x + this.width - 8, rowY + 17, isHovered ? 0x5000E5FF : 0x303498DB);
                graphics.text(font, "更换", this.x + this.width - 36, rowY + 6, btnCol, false);
            } else {
                graphics.text(font, "未持有", this.x + this.width - 44, rowY + 6, 0xFF666666, false);
            }

            rowY += 22;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.open) return false;

        // 点击关闭按钮
        if (mouseX >= this.x + this.width - 16 && mouseX <= this.x + this.width - 4 && mouseY >= this.y + 4 && mouseY <= this.y + 16) {
            this.open = false;
            return true;
        }

        // 检查点击哪一项
        int rowY = this.y + 20;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        boolean isCreative = player != null && player.isCreative();
        ItemStack equippedSafebox = ScavengerClientState.getEquippedSafebox();

        for (SafeboxTier tier : TIERS) {
            if (mouseX >= this.x + 4 && mouseX <= this.x + this.width - 4 && mouseY >= rowY && mouseY <= rowY + 20) {
                boolean isCurrent = !equippedSafebox.isEmpty() && equippedSafebox.is(tier.item());
                boolean isOwned = isCreative || isPlayerHoldingSafebox(tier.item(), player);

                if (!isCurrent && isOwned) {
                    // 发送安全箱切换包
                    ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                            ScavengerGridActionC2SPacket.ActionType.SWITCH_SAFEBOX,
                            ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                            ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                            0, tier.tierIndex(), 0, false, null
                    ));
                    this.open = false;
                    return true;
                }
            }
            rowY += 22;
        }

        // 点击在弹窗范围外 -> 关闭弹窗
        if (mouseX < this.x || mouseX > this.x + this.width || mouseY < this.y || mouseY > this.y + this.height) {
            this.open = false;
            return false; // 允许外界事件继续冒泡
        }

        return true; // 消费弹窗内的其他空白点击
    }

    private boolean isPlayerHoldingSafebox(Item targetItem, Player player) {
        if (player == null) return false;
        // 1. 快捷栏
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(targetItem)) return true;
        }
        // 2. 随身背包
        for (GridItemEntry entry : ScavengerClientState.getBackpackEntries()) {
            if (entry.copyStack().is(targetItem)) return true;
        }
        // 3. 基地仓库
        for (RedactedGridEntryView view : ScavengerClientState.getContainerEntries().values()) {
            if (view.stack().is(targetItem)) return true;
        }
        return false;
    }
}
