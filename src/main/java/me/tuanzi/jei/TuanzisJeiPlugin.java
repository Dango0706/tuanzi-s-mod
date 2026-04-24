package me.tuanzi.jei;

import me.tuanzi.util.ModLog;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import me.tuanzi.Tuanzis_mod;

@JeiPlugin
public class TuanzisJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ModLog.info("JEI support initialized for tuanzis_mod!");
    }
}
