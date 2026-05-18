package me.tuanzi;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.ModInitializer;

public class Tuanzis_mod implements ModInitializer {
	public static final String MOD_ID = "tuanzis_mod";

	@Override
	public void onInitialize() {
		ModEnchantments.initialize();
		me.tuanzi.init.ModItems.initialize();
		me.tuanzi.init.ModItemGroups.initialize();
		me.tuanzi.init.ModStatusEffects.initialize();
		me.tuanzi.init.ModPotions.initialize();
		ModLog.info("Hello Fabric world!");
		ModLog.debug("Mod initialization started in development mode!");
	}
}
