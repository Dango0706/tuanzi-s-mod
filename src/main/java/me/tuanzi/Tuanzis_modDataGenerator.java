package me.tuanzi;

import me.tuanzi.datagen.ModEnchantmentGenerator;
import me.tuanzi.datagen.ModEnchantmentTagProvider;
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
		pack.addProvider(ModEnchantmentTagProvider::new);
		pack.addProvider(me.tuanzi.datagen.ModRecipeProvider::new);
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
			translationBuilder.add("enchantment.tuanzis_mod.smelting", "熔炼");
			translationBuilder.add(ModItems.RAINBOW_SPONGE, "彩虹海绵");
			translationBuilder.add(ModItems.YURIS_REVENGE, "尤里的复仇");
			translationBuilder.add(ModItems.IMMORTAL_TALISMAN, "不朽圣符");
			translationBuilder.add("item.tuanzis_mod.immortal_talisman.tooltip", "持有此符，死亡时保留所有物品与经验值。");
			translationBuilder.add(ModItems.WARDEN_HEART, "坚守者的心脏");
			translationBuilder.add(ModItems.ECHO_BREAKER, "回响破障者");
			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "飞行");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "不死");

			translationBuilder.add("jei.tuanzis_mod.smelting.description", "【熔炼附魔】\n省去回家烧炉子的繁琐步骤。使用该工具挖掘“可在熔炉中烧炼的方块”时，直接掉落其烧炼后的产物。\n\n§e功能：§r\n1. §b自动熔炼§r：挖掘矿石或原木等可熔炼方块时直接掉落成品。\n2. §b完美兼容§r：与 §b时运 (Fortune)§r 完美兼容，可以获得更多掉落物。\n\n§e限制与惩罚：§r\n1. §b互斥机制§r：与 §b精准采集 (Silk Touch)§r 互斥。\n2. §b耐久消耗§r：每挖掘一个可熔炼方块，额外消耗 §c1§r 点耐久（共计消耗 2 点）。\n\n§7获取方式：无法从附魔台中直接获得。属于“宝藏附魔”，仅能通过搜刮下界要塞遗迹箱子、沙漠神殿宝箱或高等级图书管理员村民交易（极罕见，需 40-64 绿宝石）获取。");

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
			
			String spongeDesc = "【彩虹海绵：高级定制指南】\n" +
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
			translationBuilder.add("jei.tuanzis_mod.rainbow_sponge.description", spongeDesc);

			translationBuilder.add("item.tuanzis_mod.yuris_revenge.jei.description", "【尤里的复仇】\n一把拥有神秘力量的工具。它能够以前所未有的速度粉碎坚不可摧的基岩。\n\n§e功能：§r\n1. §b破坏基岩§r：它是唯一能够挖掘并掉落基岩工具。\n2. §b极速挖掘§r：在面对基岩时拥有惊人的破坏速度。\n\n§7注：除了基岩，它在面对其他方块时几乎毫无用处。§r");
			translationBuilder.add("jei.tuanzis_mod.immortal_talisman.description", "【不朽圣符】\n一件保命的神器。只要它存在于你的物品栏中，死亡将不再是终点。\n\n§e效果：§r\n1. §b死亡不掉落§r：保留所有物品、盔甲和经验。\n2. §b自我牺牲§r：在发挥作用后，圣符会消耗掉一个。");
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "【附魔兼容性增强】\n现在 §b无限 (Infinity)§r 与 §b经验修补 (Mending)§r 互相兼容，可以同时附魔在同一把弓上。");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "【监守者】\n一种极其危险的生物，通常潜伏在深暗之域的远古城市中。\n\n§e奖励增强：§r\n1. §b经验提升§r：现在被玩家或被驯服的狼杀死时，会掉落 §a275§r 点经验值（原为 5 点）。\n2. §b稀有掉落§r：若玩家对单个监守者累计造成超过 §c50%§r 最大生命值的伤害，该监守者死亡时有 §a10%§r 的概率掉落§6《灵魂绑定》§r附魔书（受抢夺影响，每级增加 2.5%）。\n\n§7提示：虽然经验变多了，但它依然非常危险，建议做好充分准备再进行挑战。§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "【阅历附魔】\n增加击杀生物时获得的经验收益。\n\n§e效果：§r\n1. §b收益提升§r：每一级增加 §a25%§r 的经验收益（向上取整）。\n2. §b生效条件§r：仅在主手持有该附魔武器击杀生物时生效。\n3. §b范围§r：对除击杀玩家外的所有生物有效。\n\n§7说明：最高等级为 4 级。30 级附魔台最高只能刷出 3 级，4 级需通过铁砧合并或特殊战利品获得。§r");
			translationBuilder.add("jei.tuanzis_mod.warden_heart.description", "【坚守者的心脏】\n击杀监守者 (Warden) 获得的核心材料，蕴含着其顽强生命力与声波能量。\n\n§e获取方式：§r\n1. §b稀有掉落§r：击杀监守者 (Warden) 时有 §a10%§r 的概率掉落（受抢夺附魔影响，每级抢夺增加 2.5% 掉落概率）。\n\n§e用途：§r\n1. §b合成原料§r：与 8 个 §b回响碎片 (Echo Shard)§r 合成强力防御武器——§6回响破障者§r。\n2. §b装备修复§r：可用于在铁砧中修复§6回响破障者§r武器。");
			translationBuilder.add("jei.tuanzis_mod.echo_breaker.description", "【回响破障者】\n使用坚守者的心脏与回响碎片锻造的终极防御型防具/武器。蕴藏着能抵御甚至反弹声波伤害的神奇力量。\n\n§e被动效果：§r\n1. §b声波吸收§r：仅在主手持有生效，副手及快捷栏不生效。在受到监守者音波爆破伤害时，§a自动减免 30%§r 伤害。\n\n§e主动效果 (右键使用)：§r\n1. §b使用限制§r：仅在§c副手无物品§r时才可以使用右键格挡。\n2. §b完美格挡§r：右键格挡持续 §e0.5 秒§r。格挡期间§a免疫 100% 音波爆破伤害§r。\n3. §b声波反弹§r：在完美格挡音波爆破时，将声波完美反弹给发射源，对目标造成 §c15 点§r 真实伤害并附加 §9强烈击退§r 效果。\n4. §b格挡代价§r：每次格挡成功将额外扣除 §c3 点§r 耐久值。\n5. §b冷却机制§r：格挡§c失败§r则触发 §e5.0 秒§r 冷却，若格挡§a成功§r则触发 §68.0 秒§r 冷却。\n\n§e修复与维护：§r\n可以使用§b坚守者的心脏§r在铁砧中修复其耐久度。");
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
			translationBuilder.add("enchantment.tuanzis_mod.smelting", "Smelting");
			translationBuilder.add(ModItems.RAINBOW_SPONGE, "Rainbow Sponge");
			translationBuilder.add(ModItems.YURIS_REVENGE, "Yuri's Revenge");
			translationBuilder.add(ModItems.IMMORTAL_TALISMAN, "Immortal Talisman");
			translationBuilder.add("item.tuanzis_mod.immortal_talisman.tooltip", "Retain all items and experience on death while in inventory.");
			translationBuilder.add(ModItems.WARDEN_HEART, "Warden Heart");
			translationBuilder.add(ModItems.ECHO_BREAKER, "Echo Breaker");
			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "Flight");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "Undying");

			translationBuilder.add("jei.tuanzis_mod.smelting.description", "[Smelting Enchantment]\nSkip the hassle of returning home to use a furnace. When mining blocks that can be smelted, it directly drops the smelted result.\n\n§eFeatures:§r\n1. §bAuto-Smelt§r: Directly drops products when mining ores, logs, or other smeltable blocks.\n2. §bFull Compatibility§r: Perfectly compatible with §bFortune§r, allowing for increased drops.\n\n§eConstraints & Penalties:§r\n1. §bMutual Exclusion§r: Incompatible with §bSilk Touch§r.\n2. §bDurability Cost§r: Each smeltable block mined consumes §c1§r extra durability (total 2).\n\n§7Acquisition: Cannot be obtained from the enchantment table. A \"Treasure Enchantment\" found only in Nether Fortress/Desert Temple chests or through high-level Librarian trades (rare, 40-64 emeralds).");

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

			String spongeDesc = "[Rainbow Sponge: Advanced Customization Guide]\n" +
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
			translationBuilder.add("jei.tuanzis_mod.rainbow_sponge.description", spongeDesc);

			translationBuilder.add("item.tuanzis_mod.yuris_revenge.jei.description", "[Yuri's Revenge]\nA tool imbued with mysterious power, capable of shattering indestructible bedrock at unprecedented speeds.\n\n§eFeatures:§r\n1. §bBedrock Breaker§r: The only tool that can mine and drop bedrock.\n2. §bExtreme Speed§r: Amazing destruction speed when facing bedrock.\n\n§7Note: It is nearly useless against any block other than bedrock.§r");
			translationBuilder.add("jei.tuanzis_mod.immortal_talisman.description", "[Immortal Talisman]\nA life-saving artifact. As long as it's in your inventory, death is no longer the end.\n\n§eEffects:§r\n1. §bKeep Inventory§r: Retain all items, armor, and experience upon death.\n2. §bSelf-Sacrifice§r: One talisman is consumed each time it saves you.\n\n§cWarning: If all talismans are consumed, death protection will fail!§r");
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "[Enchantment Compatibility]\n§bInfinity§r and §bMending§r are now compatible and can be applied to the same bow simultaneously.");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "[Warden]\nAn extremely dangerous creature that typically lurks within the Ancient Cities of the Deep Dark.\n\n§eReward Boost:§r\n1. §bXP Increase§r: Now drops §a275§r experience points when killed by a player or a tamed wolf (was 5).\n2. §bRare Drop§r: If a player deals more than §c50%§r of its max health as damage, it has a §a10%§r chance to drop a §6Soulbound§r enchanted book (affected by Looting, +2.5% per level).\n\n§7Tip: Even with the increased reward, it remains highly dangerous. Prepare thoroughly before engaging.§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "[Experience Enchantment]\nIncreases the experience points gained from killing mobs.\n\n§eEffects:§r\n1. §bXP Boost§r: Each level increases experience gain by §a25%§r (rounded up).\n2. §bCondition§r: Only effective when killing mobs with the enchanted weapon in the main hand.\n3. §bScope§r: Effective against all mobs except players.\n\n§7Note: Max level is 4. Level 30 enchantment table can yield up to level 3. Level 4 requires anvil merging or special loot.§r");
			translationBuilder.add("jei.tuanzis_mod.warden_heart.description", "[Warden Heart]\nA core material obtained from defeating the Warden, containing its resilient life force and sonic energy.\n\n§eAcquisition:§r\n1. §bRare Drop:§r Defeating a Warden has a §a10%§r chance to drop it (affected by Looting, +2.5% probability per level).\n\n§eUsage:§r\n1. §bCrafting Ingredient:§r Combine with 8 §bEcho Shards§r to craft the powerful defensive weapon — §6Echo Breaker§r.\n2. §bEquipment Repair:§r Can be used to repair the §6Echo Breaker§r in an anvil.");
			translationBuilder.add("jei.tuanzis_mod.echo_breaker.description", "[Echo Breaker]\nAn ultimate defensive weapon forged using a Warden Heart and Echo Shards. It holds the mysterious power to resist and even deflect sonic damage.\n\n§ePassive Effects:§r\n1. §bSonic Absorption:§r Effective only when held in the main hand. Automatically §areduces Sonic Boom damage by 30%§r.\n\n§eActive Effects (Right-click):§r\n1. §bRestrictions:§r Right-click block can only be used when the §coffhand is empty§r.\n2. §bPerfect Block:§r Right-click to block for §e0.5 seconds§r. During blocking, grants §a100% immunity to Sonic Boom damage§r.\n3. §bSonic Deflection:§r Blocking a Sonic Boom perfectly deflects the sound wave back to its source, dealing §c15 true damage§r and applying §9strong knockback§r.\n4. §bDurability Cost:§r Each successful block consumes an extra §c3 durability points§r.\n5. §bCooldown Mechanics:§r Triggers a §c5.0-second§r cooldown on §cfailure§r (no damage blocked); triggers an §a8.0-second§r cooldown on §asuccess§r (perfectly deflected).\n\n§eRepair & Maintenance:§r\nCan be repaired in an anvil using a §bWarden Heart§r.");
		}
	}
}
