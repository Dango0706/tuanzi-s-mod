package me.tuanzi.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ClientTooltipHelper {
    public static boolean isShiftDown() {
        try {
            long handle = Minecraft.getInstance().getWindow().handle();
            return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS 
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }

    public static int getBeeStingEchoColor(double wear) {
        if (wear < 0.0) wear = 0.0;
        if (wear > 1.0) wear = 1.0;

        double t;
        int rStart = 255, gStart = 255, bStart = 255;
        int rEnd = 255, gEnd = 255, bEnd = 255;

        if (wear < 0.2) {
            t = wear / 0.2;
            rEnd = 255;
            gEnd = 224;
            bEnd = 160;
        } else if (wear < 0.4) {
            t = (wear - 0.2) / 0.2;
            rEnd = 252;
            gEnd = 208;
            bEnd = 128;
        } else if (wear < 0.6) {
            t = (wear - 0.4) / 0.2;
            rEnd = 221;
            gEnd = 165;
            bEnd = 112;
        } else if (wear < 0.8) {
            t = (wear - 0.6) / 0.2;
            rEnd = 162;
            gEnd = 141;
            bEnd = 117;
        } else {
            t = (wear - 0.8) / 0.2;
            rEnd = 138;
            gEnd = 112;
            bEnd = 192;
        }

        int r = (int) (rStart + t * (rEnd - rStart));
        int g = (int) (gStart + t * (gEnd - gStart));
        int b = (int) (bStart + t * (bEnd - bStart));

        return (r << 16) | (g << 8) | b;
    }
}
