package me.tuanzi;

import me.tuanzi.datagen.ModEnchantmentGenerator;
import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModStatusEffects;
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
			translationBuilder.add("enchantment.tuanzis_mod.experience", "阅历");
			translationBuilder.add(ModItems.RAINBOW_SPONGE, "彩虹海绵");
			translationBuilder.add(ModItems.YURIS_REVENGE, "尤里的复仇");
			translationBuilder.add(ModItems.IMMORTAL_TALISMAN, "不朽圣符");
			translationBuilder.add("item.tuanzis_mod.immortal_talisman.tooltip", "持有此符，死亡时保留所有物品与经验值。");
			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "飞行");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "不死");

			// 药水翻译
			translationBuilder.add("item.minecraft.potion.effect.flight", "飞行药水");
			translationBuilder.add("item.minecraft.splash_potion.effect.flight", "喷溅型飞行药水");
			translationBuilder.add("item.minecraft.lingering_potion.effect.flight", "滞留型飞行药水");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.flight", "飞行药箭");
			translationBuilder.add("item.minecraft.potion.effect.long_flight", "飞行药水 (延长)");
			translationBuilder.add("item.minecraft.splash_potion.effect.long_flight", "喷溅型飞行药水 (延长)");
			translationBuilder.add("item.minecraft.lingering_potion.effect.long_flight", "滞留型飞行药水 (延长)");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.long_flight", "飞行药箭 (延长)");

			translationBuilder.add("item.minecraft.potion.effect.undying", "不死药水");
			translationBuilder.add("item.minecraft.splash_potion.effect.undying", "喷溅型不死药水");
			translationBuilder.add("item.minecraft.lingering_potion.effect.undying", "滞留型不死药水");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.undying", "不死药箭");
			translationBuilder.add("item.minecraft.potion.effect.long_undying", "不死药水 (延长)");
			translationBuilder.add("item.minecraft.splash_potion.effect.long_undying", "喷溅型不死药水 (延长)");
			translationBuilder.add("item.minecraft.lingering_potion.effect.long_undying", "滞留型不死药水 (延长)");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.long_undying", "不死药箭 (延长)");
			
			String desc = "【彩虹海绵：高级定制指南】\n" +
				"彩虹海绵是一张魔法指令卡。先通过在铁砧中命名来“编程”，再将其与物品合并来应用样式。\n\n" +
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
			translationBuilder.add("jei.tuanzis_mod.rainbow_sponge.description", desc);

			translationBuilder.add("item.tuanzis_mod.yuris_revenge.jei.description", "【尤里的复仇】\n一把拥有神秘力量的工具。它能够以前所未有的速度粉碎坚不可摧的基岩。\n\n§e功能：§r\n1. §b破坏基岩§r：它是唯一能够挖掘并掉落基岩工具。\n2. §b极速挖掘§r：在面对基岩时拥有惊人的破坏速度。\n\n§7注：除了基岩，它在面对其他方块时几乎毫无用处。§r");
			translationBuilder.add("jei.tuanzis_mod.immortal_talisman.description", "【不朽圣符】\n一件保命的神器。只要它存在于你的物品栏中，死亡将不再是终点。\n\n§e效果：§r\n1. §b死亡不掉落§r：保留所有物品、盔甲和经验。\n2. §b自我牺牲§r：在发挥作用后，圣符会消耗掉一个。");
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "【附魔兼容性增强】\n现在 §b无限 (Infinity)§r 与 §b经验修补 (Mending)§r 互相兼容，可以同时附魔在同一把弓上。");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "【监守者】\n一种极其危险的生物，通常潜伏在深暗之域的远古城市中。\n\n§e奖励增强：§r\n1. §b经验提升§r：现在被玩家或被驯服的狼杀死时，会掉落 §a275§r 点经验值（原为 5 点）。\n2. §b稀有掉落§r：若玩家对单个监守者累计造成超过 §c50%§r 最大生命值的伤害，该监守者死亡时有 §a10%§r 的概率掉落§6《灵魂绑定》§r附魔书（受抢夺影响，每级增加 2.5%）。\n\n§7提示：虽然经验变多了，但它依然非常危险，建议做好充分准备再进行挑战。§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "【阅历附魔】\n增加击杀生物时获得的经验收益。\n\n§e效果：§r\n1. §b收益提升§r：每一级增加 §a25%§r 的经验收益（向上取整）。\n2. §b生效条件§r：仅在主手持有该附魔武器击杀生物时生效。\n3. §b范围§r：对除击杀玩家外的所有生物有效。\n\n§7说明：最高等级为 4 级。30 级附魔台最高只能刷出 3 级，4 级需通过铁砧合并或特殊战利品获得。§r");
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
			translationBuilder.add("enchantment.tuanzis_mod.experience", "Experience");
			translationBuilder.add(ModItems.RAINBOW_SPONGE, "Rainbow Sponge");
			translationBuilder.add(ModItems.YURIS_REVENGE, "Yuri's Revenge");
			translationBuilder.add(ModItems.IMMORTAL_TALISMAN, "Immortal Talisman");
			translationBuilder.add("item.tuanzis_mod.immortal_talisman.tooltip", "Retain all items and experience on death while in inventory.");
			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "Flight");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "Undying");

			// Potion Translations
			translationBuilder.add("item.minecraft.potion.effect.flight", "Potion of Flight");
			translationBuilder.add("item.minecraft.splash_potion.effect.flight", "Splash Potion of Flight");
			translationBuilder.add("item.minecraft.lingering_potion.effect.flight", "Lingering Potion of Flight");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.flight", "Arrow of Flight");
			translationBuilder.add("item.minecraft.potion.effect.long_flight", "Potion of Flight (Extended)");
			translationBuilder.add("item.minecraft.splash_potion.effect.long_flight", "Splash Potion of Flight (Extended)");
			translationBuilder.add("item.minecraft.lingering_potion.effect.long_flight", "Lingering Potion of Flight (Extended)");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.long_flight", "Arrow of Flight (Extended)");

			translationBuilder.add("item.minecraft.potion.effect.undying", "Potion of Undying");
			translationBuilder.add("item.minecraft.splash_potion.effect.undying", "Splash Potion of Undying");
			translationBuilder.add("item.minecraft.lingering_potion.effect.undying", "Lingering Potion of Undying");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.undying", "Arrow of Undying");
			translationBuilder.add("item.minecraft.potion.effect.long_undying", "Potion of Undying (Extended)");
			translationBuilder.add("item.minecraft.splash_potion.effect.long_undying", "Splash Potion of Undying (Extended)");
			translationBuilder.add("item.minecraft.lingering_potion.effect.long_undying", "Lingering Potion of Undying (Extended)");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.long_undying", "Arrow of Undying (Extended)");
			translationBuilder.add("message.tuanzis_mod.flight_expiring", "§cWarning: Flight effect expires in %d seconds!§r");

			String desc = "[Rainbow Sponge: Advanced Customization Guide]\n" +
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
			translationBuilder.add("jei.tuanzis_mod.rainbow_sponge.description", desc);

			translationBuilder.add("item.tuanzis_mod.yuris_revenge.jei.description", "[Yuri's Revenge]\nA tool imbued with mysterious power, capable of shattering indestructible bedrock at unprecedented speeds.\n\n§eFeatures:§r\n1. §bBedrock Breaker§r: The only tool that can mine and drop bedrock.\n2. §bExtreme Speed§r: Amazing destruction speed when facing bedrock.\n\n§7Note: It is nearly useless against any block other than bedrock.§r");
			translationBuilder.add("jei.tuanzis_mod.immortal_talisman.description", "[Immortal Talisman]\nA life-saving artifact. As long as it's in your inventory, death is no longer the end.\n\n§eEffects:§r\n1. §bKeep Inventory§r: Retain all items, armor, and experience upon death.\n2. §bSelf-Sacrifice§r: One talisman is consumed each time it saves you.\n\n§cWarning: If all talismans are consumed, death protection will fail!§r");
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "[Enchantment Compatibility]\n§bInfinity§r and §bMending§r are now compatible and can be applied to the same bow simultaneously.");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "[Warden]\nAn extremely dangerous creature that typically lurks within the Ancient Cities of the Deep Dark.\n\n§eReward Boost:§r\n1. §bXP Increase§r: Now drops §a275§r experience points when killed by a player or a tamed wolf (was 5).\n2. §bRare Drop§r: If a player deals more than §c50%§r of its max health as damage, it has a §a10%§r chance to drop a §6Soulbound§r enchanted book (affected by Looting, +2.5% per level).\n\n§7Tip: Even with the increased reward, it remains highly dangerous. Prepare thoroughly before engaging.§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "[Experience Enchantment]\nIncreases the experience points gained from killing mobs.\n\n§eEffects:§r\n1. §bXP Boost§r: Each level increases experience gain by §a25%§r (rounded up).\n2. §bCondition§r: Only effective when killing mobs with the enchanted weapon in the main hand.\n3. §bScope§r: Effective against all mobs except players.\n\n§7Note: Max level is 4. Level 30 enchantment table can yield up to level 3. Level 4 requires anvil merging or special loot.§r");
		}
	}
}
