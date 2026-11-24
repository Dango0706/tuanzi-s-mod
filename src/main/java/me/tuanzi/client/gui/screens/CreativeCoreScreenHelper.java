package me.tuanzi.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

@Environment(EnvType.CLIENT)
public class CreativeCoreScreenHelper {
    public static void openScreen(InteractionHand hand, int intervalTicks, int shotsPerCycle) {
        Minecraft.getInstance().setScreenAndShow(new CreativeCoreScreen(hand, intervalTicks, shotsPerCycle));
    }
}
