package me.tuanzi;

import me.tuanzi.datagen.ModEnchantmentGenerator;
import me.tuanzi.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

import java.util.concurrent.CompletableFuture;

public class Tuanzis_modDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(ModEnchantmentGenerator::new);
		pack.addProvider(TuanziChineseLanguageProvider::new);
		pack.addProvider(TuanziEnglishLanguageProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.ENCHANTMENT, context -> {});
	}

	private static class TuanziChineseLanguageProvider extends FabricLanguageProvider {
		protected TuanziChineseLanguageProvider(net.fabricmc.fabric.api.datagen.v1.FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
			super(dataOutput, "zh_cn", registryLookup);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translationBuilder) {
			translationBuilder.add("itemGroup.tuanzis_mod.tuanzis_mod_tab", "团子的模组");
			translationBuilder.add("enchantment.tuanzis_mod.soulbound", "灵魂绑定");
			translationBuilder.add(ModItems.COLOR_SPONGE, "颜色海绵");
			
			String desc = "【颜色海绵：高级定制指南】\n" +
				"颜色海绵是一张魔法指令卡。先通过在铁砧中命名来“编程”，再将其与物品合并来应用样式。\n\n" +
				"§n一、编程语法 (重命名海绵)§r\n" +
				"1. §b十六进制§r：'#RRGGBB' (如 #FF5555)。[费: 8级]\n" +
				"2. §b样式代码§r：'&L' (如 &l加粗, &o倾斜, &n下划线)。[费: 每项 +4级]\n" +
				"3. §b分割系统§r：',' 分隔样式 (如 &c,&b,&l)。样式循环应用到每个字。[费: 每个 ',' +2级]\n" +
				"4. §b智能渐变§r：'&qN#Start-#End'。N为字数，End可省。系统生成平滑过渡。[费: 每字 8级]\n" +
				"§7注意：渐变指令 (&q) 预览不显色，其余写法均实时显示染色效果作为示范。§r\n\n" +
				"§n二、应用样式 (合并物品)§r\n" +
				"1. §e普通/样式合并§r：基础 32 级 + (样式项×16 + 逗号数×8)。\n" +
				"2. §e渐变合并§r：每字 32 级。\n" +
				"3. §e重置属性§r：将命名的 '&r' 海绵与物品合并可清除所有颜色样式。[费: 16级]";
			translationBuilder.add("jei.tuanzis_mod.color_sponge.description", desc);
		}
	}

	private static class TuanziEnglishLanguageProvider extends FabricLanguageProvider {
		protected TuanziEnglishLanguageProvider(net.fabricmc.fabric.api.datagen.v1.FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
			super(dataOutput, "en_us", registryLookup);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translationBuilder) {
			translationBuilder.add("itemGroup.tuanzis_mod.tuanzis_mod_tab", "Tuanzi's Mod");
			translationBuilder.add("enchantment.tuanzis_mod.soulbound", "Soulbound");
			translationBuilder.add(ModItems.COLOR_SPONGE, "Color Sponge");

			String desc = "[Color Sponge: Advanced Customization Guide]\n" +
				"The sponge acts as a 'Command Card'. Program it via Anvil naming, then merge with an item to apply styles.\n\n" +
				"§n1. Programming Syntax (Naming)§r\n" +
				"1. §bHex Colors§r: '#RRGGBB' (e.g. #FF5555). [Cost: 8]\n" +
				"2. §bVanilla Styles§r: '&L' (e.g. &l Bold, &o Italic). [Cost: +4 per code]\n" +
				"3. §bSplitting System§r: Use ',' (e.g. &c,&b). Styles apply char-by-char. [Cost: +2 per ',']\n" +
				"4. §bSmart Gradient§r: '&qN#Start-#End'. N=Length. End optional. Smooth shift. [Cost: 8 per char]\n" +
				"§7Note: Gradient codes (&q) show plain text; others show real color as demo.§r\n\n" +
				"§n2. Applying Styles (Merging)§r\n" +
				"1. §eNormal/Style Merge§r: 32 base + (Styles*16 + Commas*8).\n" +
				"2. §eGradient Merge§r: 32 per character.\n" +
				"3. §eReset Styles§r: Merge '&r' sponge to clear item's custom colors. [Cost: 16]";
			translationBuilder.add("jei.tuanzis_mod.color_sponge.description", desc);
		}
	}
}
