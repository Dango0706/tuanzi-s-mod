package me.tuanzi.client.gui.screens;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

@Environment(EnvType.CLIENT)
public class ChromaticSkullScreenHelper {
    public static void openScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreenAndShow(new ChromaticSkullScreen(hand));
    }
}
