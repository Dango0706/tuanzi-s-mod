package me.tuanzi.client.gui.screens.scavenger;

import me.tuanzi.client.scavenger.ScavengerClientState;
import me.tuanzi.network.scavenger.ScavengerBaseUpgradeC2SPacket;
import me.tuanzi.world.inventory.scavenger.BaseUpgradeStationMenu;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 拾荒回廊基地整备升级台界面（视图 D）。
 * 支持跨容器（快捷栏 + 随身背包 + 基地仓库）材料统计，提供负重系统与基地仓库扩容操作。
 */
@Environment(EnvType.CLIENT)
public class BaseUpgradeStationScreen extends AbstractContainerScreen<BaseUpgradeStationMenu> {

    private static final int SCREEN_WIDTH = 340;
    private static final int SCREEN_HEIGHT = 224;

    private Button weightUpgradeBtn;
    private Button stashUpgradeBtn;

    public BaseUpgradeStationScreen(BaseUpgradeStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos;
        int y = this.topPos;

        // 强化负重系统按钮
        this.weightUpgradeBtn = this.addRenderableWidget(Button.builder(Component.literal("立即强化"), btn -> {
            ClientPlayNetworking.send(new ScavengerBaseUpgradeC2SPacket(
                    this.menu.getStationPos(),
                    ScavengerBaseUpgradeC2SPacket.UpgradeTarget.WEIGHT_SYSTEM
            ));
        }).bounds(x + 24, y + 155, 110, 20).build());

        // 扩容基地仓库按钮
        this.stashUpgradeBtn = this.addRenderableWidget(Button.builder(Component.literal("立即扩容"), btn -> {
            ClientPlayNetworking.send(new ScavengerBaseUpgradeC2SPacket(
                    this.menu.getStationPos(),
                    ScavengerBaseUpgradeC2SPacket.UpgradeTarget.BASE_STASH
            ));
        }).bounds(x + 194, y + 155, 110, 20).build());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 禁用原版标签绘制，由自定义 extractBackground 完整绘制
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int x = this.leftPos;
        int y = this.topPos;

        // 1. 深色主背景底框
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xF010141B);
        graphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF384656);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF384656);
        graphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF384656);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF384656);

        // 中间分隔线
        graphics.fill(x + 165, y + 8, x + 166, y + 180, 0x40384656);

        // 标题
        graphics.text(this.font, "拾荒回廊 · 基地整备升级台", x + 12, y + 8, 0xFF00E5FF, true);

        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();

        // 2. 左卡片：外骨骼负重系统
        int weightLv = ScavengerClientState.getWeightLevel();
        int maxWeightLv = cfg.weightProgression().maxLevel();
        int cardLeftX = x + 16;
        int cardY = y + 26;

        graphics.fill(cardLeftX, cardY, cardLeftX + 130, cardY + 122, 0xD0161E28);
        graphics.text(this.font, String.format("🏋 负重系统 [Lv.%d / %d]", weightLv, maxWeightLv), cardLeftX + 6, cardY + 6, 0xFFFFD700, false);

        float curW1 = cfg.getOverweightThresholdKg(weightLv);
        float curW2 = cfg.getOverloadedThresholdKg(weightLv);
        float nextW1 = cfg.getOverweightThresholdKg(Math.min(maxWeightLv, weightLv + 1));
        float nextW2 = cfg.getOverloadedThresholdKg(Math.min(maxWeightLv, weightLv + 1));

        graphics.text(this.font, String.format("负重: %.0fkg ──▶ %.0fkg", curW1, nextW1), cardLeftX + 6, cardY + 22, 0xFFAAAAAA, false);
        graphics.text(this.font, String.format("过载: %.0fkg ──▶ %.0fkg", curW2, nextW2), cardLeftX + 6, cardY + 34, 0xFFAAAAAA, false);

        boolean weightSatisfied = false;
        if (weightLv >= maxWeightLv) {
            graphics.text(this.font, "已达满级，无需材料", cardLeftX + 6, cardY + 54, 0xFF2ECC71, false);
            this.weightUpgradeBtn.active = false;
        } else {
            graphics.text(this.font, "所需材料 (合并统计):", cardLeftX + 6, cardY + 50, 0xFFE0E0E0, false);
            List<ScavengerProgressionConfig.UpgradeCostEntry> costs = cfg.weightProgression().getCostForNextLevel(weightLv + 1);
            weightSatisfied = renderCostList(graphics, cardLeftX + 6, cardY + 64, costs);
            this.weightUpgradeBtn.active = weightSatisfied;
        }

        // 3. 右卡片：基地独立仓库扩容
        int stashLv = ScavengerClientState.getStashLevel();
        int maxStashLv = cfg.stashProgression().maxLevel();
        int cardRightX = x + 180;

        graphics.fill(cardRightX, cardY, cardRightX + 130, cardY + 122, 0xD0161E28);
        graphics.text(this.font, String.format("📦 基地仓库扩容 [Lv.%d / %d]", stashLv, maxStashLv), cardRightX + 6, cardY + 6, 0xFF00E5FF, false);

        int curRows = cfg.getStashRows(stashLv);
        int nextRows = cfg.getStashRows(Math.min(maxStashLv, stashLv + 1));

        graphics.text(this.font, String.format("容量: %d行 ──▶ %d行", curRows, nextRows), cardRightX + 6, cardY + 22, 0xFFAAAAAA, false);
        graphics.text(this.font, String.format("格数: %d格 ──▶ %d格", curRows * 9, nextRows * 9), cardRightX + 6, cardY + 34, 0xFFAAAAAA, false);

        boolean stashSatisfied = false;
        if (stashLv >= maxStashLv) {
            graphics.text(this.font, "已达满级，无需材料", cardRightX + 6, cardY + 54, 0xFF2ECC71, false);
            this.stashUpgradeBtn.active = false;
        } else {
            graphics.text(this.font, "所需材料 (合并统计):", cardRightX + 6, cardY + 50, 0xFFE0E0E0, false);
            List<ScavengerProgressionConfig.UpgradeCostEntry> costs = cfg.stashProgression().getCostForNextLevel(stashLv + 1);
            stashSatisfied = renderCostList(graphics, cardRightX + 6, cardY + 64, costs);
            this.stashUpgradeBtn.active = stashSatisfied;
        }

        // 4. 底部随身快捷栏
        graphics.text(this.font, "随身快捷栏 (1~9)", x + 80, y + 184, 0xFFAAAAAA, false);
    }

    private boolean renderCostList(GuiGraphicsExtractor graphics, int startX, int startY, List<ScavengerProgressionConfig.UpgradeCostEntry> costs) {
        if (costs.isEmpty()) {
            graphics.text(this.font, "暂无材料消耗配置", startX, startY, 0xFF888888, false);
            return false;
        }

        boolean allMet = true;
        int lineY = startY;

        for (ScavengerProgressionConfig.UpgradeCostEntry cost : costs) {
            int owned = countTotalOwned(cost.itemId());
            boolean met = owned >= cost.count();
            if (!met) {
                allMet = false;
            }

            Item item = BuiltInRegistries.ITEM.getValue(cost.itemId());
            ItemStack icon = item != null ? new ItemStack(item) : ItemStack.EMPTY;
            String name = !icon.isEmpty() ? icon.getHoverName().getString() : cost.itemId().getPath();
            if (name.length() > 6) {
                name = name.substring(0, 5) + "..";
            }

            String line = String.format("%s: %d/%d %s", name, owned, cost.count(), met ? "✔" : "✘");
            int col = met ? 0xFF2ECC71 : 0xFFE74C3C;
            graphics.text(this.font, line, startX, lineY, col, false);
            lineY += 12;
        }

        return allMet;
    }

    private int countTotalOwned(Identifier itemId) {
        int count = 0;
        Minecraft mc = Minecraft.getInstance();

        // 1. 快捷栏
        if (mc.player != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                    count += stack.getCount();
                }
            }
        }

        // 2. 随身背包
        for (GridItemEntry entry : ScavengerClientState.getBackpackEntries()) {
            if (BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).equals(itemId)) {
                count += entry.stack().getCount();
            }
        }

        // 3. 基地仓库快照
        for (RedactedGridEntryView view : ScavengerClientState.getContainerEntries().values()) {
            if (!view.stack().isEmpty() && BuiltInRegistries.ITEM.getKey(view.stack().getItem()).equals(itemId)) {
                count += view.stack().getCount();
            }
        }

        return count;
    }
}
