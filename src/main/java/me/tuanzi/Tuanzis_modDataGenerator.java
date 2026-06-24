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
		pack.addProvider(me.tuanzi.datagen.ModBlockTagProvider::new);
		pack.addProvider(me.tuanzi.datagen.ModItemTagProvider::new);
		pack.addProvider(me.tuanzi.datagen.ModRecipeProvider::new);
		pack.addProvider(me.tuanzi.datagen.ModModelProvider::new);
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
			translationBuilder.add(ModItems.BERSERK_CHARM, "狂暴护符");
			translationBuilder.add("item.tuanzis_mod.berserk_charm.tooltip", "放入副手，当生命值低于 30% 时自动激活。获得 15 秒的力量 II 与速度 II，并消耗一个护符。");
			translationBuilder.add(ModItems.WOLF_COMMAND, "战狼护符");
			translationBuilder.add("item.tuanzis_mod.wolf_command.tooltip", "放入副手时，驯服的狼获得额外生命值、撕裂攻击及致命伤害保护。");
			translationBuilder.add(ModItems.CRAFTSMAN_CHARM, "工匠护符");
			translationBuilder.add("item.tuanzis_mod.craftsman_charm.tooltip", "右键打开便携 3x3 合成，关闭时保留格内材料。");
			translationBuilder.add("container.tuanzis_mod.craftsman_charm", "工匠护符");
			translationBuilder.add("jei.tuanzis_mod.craftsman_charm.description", "【工匠护符】\n一个实用的便携合成道具，主体拥有工作台纹理的浅棕色金属质感，边缘镶嵌着紫水晶和黑曜石。\n\n§e功能机制：\n1. §b便携合成：手持护符并右键点击，可直接打开 3x3 的便携合成界面。\n2. §b缓存保护：护符内部带有 9 个缓存格子。关闭界面时，已放置在合成格里的材料会安全保留在护符内部，不会掉落。\n3. §b物品限制：无法把工匠护符自身放入护符内部的缓存格。且该护符只支持玩家手动操作，无法被漏斗或投掷器等自动化设备交互。\n4. §b防灾防篡关闭：当玩家在 GUI 界面内拖拽或移开正在操作的工匠护符（即护符不在原本手持的格子中时），GUI 界面将自动强制关闭。");
			translationBuilder.add(ModItems.WARDEN_HEART, "坚守者的心脏");
			translationBuilder.add(ModItems.ECHO_BREAKER, "回响破障者");
			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "飞行");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "不死");
			translationBuilder.add(ModStatusEffects.TEARING.value(), "撕裂");
			translationBuilder.add("death.attack.tuanzis_mod.tearing", "%s 因伤口撕裂大出血而亡");
			translationBuilder.add(ModItems.SCARECROW, "稻草人");
			translationBuilder.add(ModItems.DECOY_TOTEM, "假目标");
			translationBuilder.add("entity.tuanzis_mod.decoy", "假目标幻影");

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
			translationBuilder.add("jei.tuanzis_mod.berserk_charm.description", "【狂暴护符】\n一个能够激发潜在力量的特殊护符。\n\n§e触发条件：§r\n1. 必须处于§b副手栏§r。\n2. 玩家生命值低于 §c30%§r。\n\n§e激活效果：§r\n1. 消耗 §e1 个§r 护符。\n2. 获得 15 秒的 §6力量 II§r 与 §b速度 II§r 效果。\n3. 该物品会进入 §e15 秒的冷却状态§r，在此期间无法再次触发（即便生命值依然低于 30%），从而防止同一时间消耗掉多个护符。");
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "【附魔兼容性增强】\n现在 §b无限 (Infinity)§r 与 §b经验修补 (Mending)§r 互相兼容，可以同时附魔在同一把弓上。");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "【监守者】\n一种极其危险的生物，通常潜伏在深暗之域的远古城市中。\n\n§e奖励增强：§r\n1. §b经验提升§r：现在被玩家或被驯服的狼杀死时，会掉落 §a275§r 点经验值（原为 5 点）。\n2. §b稀有掉落§r：若玩家对单个监守者累计造成超过 §c50%§r 最大生命值的伤害，该监守者死亡时有 §a10%§r 的概率掉落§6《灵魂绑定》§r附魔书（受抢夺影响，每级增加 2.5%）。\n\n§7提示：虽然经验变多了，但它依然非常危险，建议做好充分准备再进行挑战。§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "【阅历附魔】\n增加击杀生物时获得的经验收益。\n\n§e效果：§r\n1. §b收益提升§r：每一级增加 §a25%§r 的经验收益（向上取整）。\n2. §b生效条件§r：仅在主手持有该附魔武器击杀生物时生效。\n3. §b范围§r：对除击杀玩家外的所有生物有效。\n\n§7说明：最高等级为 4 级。30 级附魔台最高只能刷出 3 级，4 级需通过铁砧合并或特殊战利品获得。§r");
			translationBuilder.add("jei.tuanzis_mod.warden_heart.description", "【坚守者的心脏】\n击杀监守者 (Warden) 获得的核心材料，蕴含着其顽强生命力与声波能量。\n\n§e获取方式：§r\n1. §b稀有掉落§r：击杀监守者 (Warden) 时有 §a25%§r 的概率掉落（受抢夺附魔影响，每级抢夺增加 10% 掉落概率）。\n\n§e用途：§r\n1. §b合成原料§r：与 3 个 §b回响碎片 (Echo Shard)§r 和 1 根 §b木棍§r 合成强力防御武器——§6回响破障者§r。\n2. §b装备修复§r：可用于在铁砧中修复§6回响破障者§r武器。");
			translationBuilder.add("jei.tuanzis_mod.echo_breaker.description", "【回响破障者】\n使用坚守者的心脏与回响碎片锻造的终极防御型防具/武器。蕴藏着能抵御甚至反弹声波伤害的神奇力量。\n\n§e被动效果：§r\n1. §b声波吸收§r：仅在主手持有生效，副手及快捷栏不生效。在受到监守者音波爆破伤害时，§a自动减免 15%§r 伤害。\n\n§e主动效果 (右键使用)：§r\n1. §b使用限制§r：仅在§c副手无物品§r时才可以使用右键格挡。\n2. §b完美格挡§r：右键格挡持续 §e0.75 秒§r。格挡期间§a免疫 100% 音波爆破伤害§r。\n3. §b声波反弹§r：在完美格挡音波爆破时，将声波完美反弹给发射源，对目标造成 §c18 点§r 真实伤害并附加 §9强烈击退§r 效果。\n4. §b格挡代价§r：每次格挡成功将额外扣除 §c3 点§r 耐久值。\n5. §b冷却机制§r：格挡§c失败§r则触发 §e2.0 秒§r 冷却，若格挡§a成功§r则触发 §64.0 秒§r 冷却。\n\n§e修复与维护：§r\n可以使用§b坚守者的心脏§r在铁砧中修复其耐久度。");
			translationBuilder.add("enchantment.tuanzis_mod.chain_mining", "连锁采掘");
			translationBuilder.add("enchantment.tuanzis_mod.blood_rage", "血怒");
			translationBuilder.add("enchantment.tuanzis_mod.berserker", "狂战士");
			translationBuilder.add("enchantment.tuanzis_mod.buzzing_rhythm", "蜂鸣节律");
			translationBuilder.add("effect.tuanzis_mod.blood_rage", "血怒");
			translationBuilder.add("effect.tuanzis_mod.buzzing_rhythm", "节律");
			translationBuilder.add("death.attack.tuanzis_mod.blood_rage_feedback", "%s 因血怒反噬而亡");
			translationBuilder.add("key.tuanzis_mod.chain_mining", "连锁采掘激活键");
			translationBuilder.add("key.category.tuanzis_mod", "团子的模组");
			translationBuilder.add("key.category.minecraft.tuanzis_mod", "团子的模组");
			translationBuilder.add("key.category.tuanzis_mod.tuanzis_mod", "团子的模组");
			
			String chainDesc = "【连锁采掘附魔】\n" +
				"按住快捷键同时挖掘方块，可瞬间将周围相连的同类方块一并采集。\n\n" +
				"§e功能：§r\n" +
				"1. §b连锁采集§r：挖掘一个方块时，自动破坏周围相连的同类方块，掉落物正常生成。\n" +
				"2. §b开采上限§r：等级 I 连锁 16 个，等级 II 连锁 32 个，等级 III 连锁 48 个，等级 IV 连锁 64 个。\n\n" +
				"§e惩罚与限制：§r\n" +
				"1. §b耐久损耗§r：每次连锁挖掘如常规采掘般，正常扣除工具耐久。若连锁过程中工具损毁，连锁会立刻安全切断。\n" +
				"2. §b饱食度消耗§r：每额外挖掘 1 个方块，额外增加 §c0.25§r 点消耗度。游戏将直接计算并即时扣除您的饱和度和饥饿值。\n\n" +
				"§7获取方式：目前无生存获取方式，仅限创造模式获取。§r";
			translationBuilder.add("jei.tuanzis_mod.chain_mining.description", chainDesc);

			String bloodRageDesc = "【血怒附魔】\n" +
				"将战斗的狂热转化为纯粹的破坏力。这是一把双刃剑。\n\n" +
				"§e功能：§r\n" +
				"1. §b击杀爆发§r：击杀任何生物时，玩家获得持续 §e7 秒§r 的“血怒”buff。\n" +
				"2. §b伤害狂飙§r：血怒期间，玩家造成的攻击伤害提升 §a40%§r (乘算)。\n\n" +
				"§e代价与反噬：§r\n" +
				"1. §b防御崩塌§r：血怒期间，玩家受到的所有伤害也提升 §c30%§r (乘算)。\n" +
				"2. §b命悬一线§r：若在 7 秒内未再次击杀生物刷新时间，状态结束时将受到 §c2 点 (1颗心)§r 无视护甲、不被保护附魔减免的反馈伤害。\n" +
				"3. §b冲突互斥§r：与 §b火焰附加 (Fire Aspect)§r 排斥，但完美兼容 §b锋利 (Sharpness)§r、§b亡灵杀手§r 和 §b节肢杀手§r。\n\n" +
				"§7获取方式：最高等级 I 级。可通过附魔台直接附魔、地牢箱搜刮或村民交易获得。§r";
			String berserkerDesc = "【狂战士附魔】\n" +
				"将濒死的绝境转化为毁天灭地的反击力量。\n\n" +
				"§e功能：§r\n" +
				"1. §b绝境怒火§r：穿戴该附魔胸甲时，近战攻击伤害随已损失生命值比例获得提升。每损失 10% 最大生命值，近战伤害增加 §a5%（等级 I）§r 或 §a8%（等级 II）§r。最多提升至生命值低于 §c5%§r 时达到上限。\n" +
				"2. §b伤势实时变化§r：当生命值因治疗而恢复时，伤害加成实时下降。\n\n" +
				"§e代价与惩罚：§r\n" +
				"1. §b破甲减免§r：作为代价，胸甲本身提供的基础护甲值减少 §c35%（等级 I）§r 或 §c30%（等级 II）§r（即实际保留胸甲基础护甲值的 65% 或 70%，例如 8 点护甲 of 钻石胸甲在 II 级下仅扣除 2 点，仍可提供 6 点护甲，绝非削减到仅剩 30%）。\n" +
				"2. §b冲突互斥§r：与 §b保护§r、§b火焰保护§r、§b爆炸保护§r、§b弹射物保护§r、§b摔落保护§r 等所有保护类附魔互斥。\n\n" +
				"§7获取方式：最高等级 II 级。可通过附魔台直接附魔（稀有级）、战利品箱搜刮或村民交易获得。§r";
			translationBuilder.add("jei.tuanzis_mod.blood_rage.description", bloodRageDesc);
			translationBuilder.add("jei.tuanzis_mod.berserker.description", berserkerDesc);

			translationBuilder.add("hud.tuanzis_mod.chain_mining.active", "连锁挖掘§a已激活");
			translationBuilder.add("jei.tuanzis_mod.scarecrow.description", "【稻草人】\n一个普通的装饰性物品，也是制作高级防御道具——§b假目标 (Decoy Totem)§r 的核心基础组件。\n\n§e获取方式：§r\n1. §b无序合成§r：由 1 个干草块、1 根木棍和 1 个雕刻南瓜合成。");
			translationBuilder.add("jei.tuanzis_mod.decoy_totem.description", "【假目标】\n极其强大的战略防御道具。使用后可以完美吸引周围敌对生物的注意力，为自己创造安全的输出或逃生环境。\n\n§e效果与机制：§r\n1. §b幻影生成§r：放置后产生一个持续 §e15 秒§r 的玩家幻影，吸引半径 §a10 格§r 内所有怪物的仇恨。\n2. §b完美伪装§r：幻影与释放时的玩家一模一样（包括皮肤、身上所穿盔甲与手持物品）。\n3. §b属性继承§r：幻影的血量与护甲值完美继承释放瞬间玩家的数据。\n4. §b拟真行为§r：幻影会随机进行走动、跑动、跳跃等模拟真实玩家的动作。在受到伤害时，会立即切换为奔跑动作大跳着逃跑，远离伤害来源！\n5. §b碎裂余威§r：幻影死亡时不掉落任何物品，但发出玻璃般的碎裂声。激怒半径 §a10 格§r 内所有怪物，使其获得 §c速度 II§r 效果持续 §e10 秒§r。\n\n§e获取方式：§r\n1. §b有序合成§r：上排为[空、幽匿脉络、空]，中排为[幻翼膜、稻草人、幻翼膜]，下排为[萤石、红石、萤石]。\n\n§7注：每次使用会触发 §c15 秒§r 的物品冷却时间。");

			// 缚灵笼翻译
			translationBuilder.add(ModItems.VILLAGER_CAGE, "缚灵笼");
			translationBuilder.add("item.tuanzis_mod.villager_cage.tooltip.empty", "§7空笼");
			translationBuilder.add("item.tuanzis_mod.villager_cage.tooltip.filled", "§b内含: %s - %s（等级%d）");
			translationBuilder.add("message.tuanzis_mod.villager_cage.need_solid", "§c需要固体方块表面才能释放村民！");
			translationBuilder.add("message.tuanzis_mod.villager_cage.golem_aggro", "§c附近守护中的铁傀儡对你产生了敌意！");
			translationBuilder.add("jei.tuanzis_mod.villager_cage.description",
				"【缚灵笼】\n一件神秘的囚禁装置，可将村民封印于其中并跨维度运输。\n\n" +
				"§e捕捉村民：§r\n" +
				"1. §b主手持有§r缚灵笼，§b右键村民§r即可捕捉，播放灵魂沙音效并产生末影粒子。\n" +
				"2. 村民所有数据（职业/等级/交易/命名）均完整保存，§c捕捉后笼子即销毁§r（一次性使用）。\n" +
				"3. §c注意§r：若村民受到铁傀儡主动保护，捕捉将引发半径 §c16格§r 内所有傀儡的仇恨！\n\n" +
				"§e释放村民：§r\n" +
				"1. §b潜行 + 右键固体方块表面§r释放，村民在方块上方生成，数据完整恢复。\n" +
				"2. 释放后笼子消失；若目标位置无固体方块或空间不足则拒绝释放并提示。\n\n" +
				"§e特殊说明：§r\n" +
				"1. §b跨维度运输§r：村民数据存于物品中，可在任意维度释放。\n" +
				"2. 交易锁定状态保留，但床与工作站绑定会清除（需重新绑定）。\n\n" +
				"§e合成：§r\n铁锭 绿宝石块 铁锭\n铁锭  灵魂沙  铁锭\n铁锭  铁锭  铁锭");

			// 试炼假人翻译资源
			translationBuilder.add(me.tuanzi.init.ModItems.TRIAL_DUMMY, "试炼假人");
			translationBuilder.add("entity.tuanzis_mod.trial_dummy", "试炼假人");
			translationBuilder.add("jei.tuanzis_mod.trial_dummy.description", "【试炼假人】\n用于测试玩家伤害输出与DPS的专业实体辅助工具。\n\n§e使用规则：§r\n1. §b放置§r：手持右键方块表面放置，生成假人。只能放置在固体方块上方，且半径 10 格内同一玩家最多只能放置 1 个（放置第二个时第一个瞬间自动无损收回）。\n2. §b回收§r：空手且潜行状态下右键点击假人可将其收回为物品。每次回收消耗 1 点物品耐久度。耐久归零时物品消失，假人消散并掉落木棍和干草块。收回时身上穿戴的全部盔甲也将无损掉落！\n3. §b无敌与防御继承§r：假人实体本身无敌（除创造模式左键一击即碎），不受任何火烧、跌落等环境伤害。允许玩家右键为其穿戴/对换任意盔甲防具与武器，假人将完美继承所有盔甲防御值、韧性、附魔保护及药水减免，提供100%最真实的伤害测试环境！\n4. §b武器与箭矢保护§r：攻击假不消耗玩家武器工具耐久，射击不消耗箭矢。\n5. §bDPS统计§r：受击时头顶生成橙色伤害跳字。Action bar 实时显示累计伤害与DPS。未攻击 4 秒自动归档重置。空手非潜行右键可向聊天栏输出上一轮（或进行中）的极其详尽的伤害测试数据！");
			translationBuilder.add("hud.tuanzis_mod.trial_dummy.actionbar", "⚔ 累计伤害: %s | DPS: %s");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.reset", "§c[试炼假人] 测试结束，DPS 统计已归档重置。");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.no_tests", "§6[试炼假人] 当前没有任何测试数据。攻击假人以开启测试。");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.title_active", "============ §a试炼假人 伤害统计 (进行中)§r ============");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.title_archive", "============ §6试炼假人 伤害统计 (已归档)§r ============");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.total_damage", "▶ 累计输出伤害: §e%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.max_damage", "▶ 单次最大伤害: §e%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.hits", "▶ 有效攻击次数: §e%s§r 次");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.dps", "▶ 平均每秒伤害 (DPS): §c%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.duration", "▶ 伤害测试时长: §a%s§r 秒");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.footer", "================================================");

			// 灵笼贸易站中文翻译
			translationBuilder.add(me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION, "灵笼贸易站");
			translationBuilder.add(me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION.asItem(), "灵笼贸易站");
			translationBuilder.add("item.tuanzis_mod.soul_merchant_station.tooltip", "右键放置，导入村民以进行无AI超高TPS无实体村民交易。");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.imported", "§a村民已入驻！");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.no_villager", "§c当前贸易站尚未入驻村民！");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.already_has_villager", "§c该贸易站中已经入驻了一位村民！");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.extracted", "§a村民已被重新封装！");
			translationBuilder.add("jei.tuanzis_mod.soul_merchant_station.description",
				"【灵笼贸易站】\n一个革命性的无AI无实体村民交易装置。极大优化服务器FPS/TPS，同时保留核心玩法。\n\n" +
				"§e功能：§r\n" +
				"1. §b村民导入§r：手持装有村民的§b缚灵笼§r右键此方块，可将村民导入贸易站。缚灵笼消耗，同时外观上显示村民职业的3D浮空图标。\n" +
				"2. §b村民取出§r：手持§c空缚灵笼§r且§e潜行+右键§r此方块，可安全地将村民重新封入空缚灵笼中，方块本身完好。\n" +
				"3. §b无AI交易§r：右键它会直接打开完整的村民交易界面，经验累积、等级提升与原版完全同步，且村民100%安全，不受伤害。\n" +
				"4. §b自动刷新§r：每天黎明或睡醒时，若贸易站相邻 6 面（东、西、南、北、上、下）贴有该村民所对应且§b未被占用的工作站点§r，锁定的交易会自动刷新（清空红叉）。\n" +
				"5. §b命名支持§r：对贸易站使用§b命名牌§r可修改其名称，显示在交易界面顶部。\n" +
				"6. §b完全防爆§r：方块本身完全免疫任何爆炸，村民处于永久温室中绝对安全。");

			// 旅者手札相关中文翻译
			translationBuilder.add(ModItems.TRAVELERS_NOTEBOOK, "旅者手札");
			translationBuilder.add(ModItems.TELEPORTATION_PAPER, "传送纸");
			translationBuilder.add(ModItems.SIGNPOST_RUNE, "道标符石");
			
			translationBuilder.add("item.tuanzis_mod.travelers_notebook.tooltip.energy", "⏣ 能量：%d/%d");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.unbound", "§8[未绑定]§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.bind_hint", "§7潜行+右键以绑定当前位置§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.bound", "§a[已绑定]§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.dimension", "§7维度：%s§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.coords", "§7坐标：X: %s, Y: %s, Z: %s§r");
			
			translationBuilder.add("message.tuanzis_mod.signpost_rune.bound", "§a位置已成功绑定！§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.insufficient_energy", "§c旅者手札：能量不足§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.interrupted", "§c传送被打断§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.starting", "§d传送开始引导... 请保持静止，持续 3 秒§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.cooldown", "§c旅者手札：冷却中... 剩余 %.1f 秒§r");
			
			translationBuilder.add("container.tuanzis_mod.travelers_notebook", "旅者手札存储");
			
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.teleport", "传送");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.energy_status", "⏣ 能量：%d/64");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.empty_slots", "请在存储界面中放入道标符石进行目的地标记。");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.select_hint", "请在左侧列表中选择一个已绑定的道标符石开始传送。");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.dim_label", "维度：%s");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.energy_cost", "传送消耗：%d 能量");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.no_energy_warning", "能量不足");

			// 维度翻译
			translationBuilder.add("dimension.minecraft.overworld", "主世界");
			translationBuilder.add("dimension.minecraft.the_nether", "下界");
			translationBuilder.add("dimension.minecraft.the_end", "末路之地");

			translationBuilder.add("jei.tuanzis_mod.travelers_notebook.description", "【旅者手札】\n一本凝聚了虚空维度的传送手札。\n\n§e使用方法：§r\n1. §b右键点击§r：打开水晶书本传送界面。选择一个已绑定的符石并点击传送按钮。\n2. §b潜行+右键§r：打开内部存储界面（3x9 共 27格）。\n\n§e存储与充能：§r\n1. 最左上角第一格（Slot 0）为 §e燃料格§r。在此格放入 §b传送纸§r 并在关闭界面时自动将其消耗充能，每张增加 1 点能量，上限 64 点。\n2. 剩余 26 格仅限放置已绑定的 §b道标符石§r。\n3. 手札本身无法被附魔或在砧上修复。\n\n§e传送规则与消耗：§r\n1. §b同维度§r传送消耗 §a1§r 点能量，§b跨维度§r传送消耗 §a2§r 点能量。\n2. 传送伴随 §e3 秒引导时间§r，期间受到伤害或产生移动都将打断传送。\n3. 传送后同维度冷却 3 秒，跨维度冷却 5 秒。\n\n§c安全警告：§r\n若手札物品实体在世界中被火烧、爆炸或被仙人掌摧毁，其内部存储的所有物品将全部在原地掉出！");
			translationBuilder.add("jei.tuanzis_mod.teleportation_paper.description", "【传送纸】\n一种被注入微弱末影能量的羊皮纸。\n\n§e用途：§r\n唯一的充能媒介。将其放进 §b旅者手札§r 存储界面的第一格（燃料格），在关闭界面后手札将消耗纸张进行能量充实，每消耗一张补充 1 点能量。");
			translationBuilder.add("jei.tuanzis_mod.signpost_rune.description", "【道标符石】\n记录世界之维空间坐标的符石。\n\n§e使用方法：§r\n1. §b绑定位置§r：主手或副手手持它并在任意地点右键点击，符石将立即绑定当前坐标与维度，并播放清脆的音效。\n2. §b自定义名称§r：绑定的符石可以使用 §b铁砧§r 进行重命名，该自定义名称将在 §b旅者手札§r 的传送列表里直接显示。\n\n§7注：符石不可堆叠。只有绑定了位置的符石才能放进手札的存储格中用于传送。");

			// 肾上腺素药水效果中文翻译
			translationBuilder.add(ModStatusEffects.ADRENALINE.value(), "肾上腺素");
			translationBuilder.add(ModStatusEffects.ADRENALINE_OVERDRAW.value(), "肾上腺素透支");
			
			translationBuilder.add("item.minecraft.potion.effect.adrenaline", "肾上腺素药水");
			translationBuilder.add("item.minecraft.splash_potion.effect.adrenaline", "喷溅型肾上腺素药水");
			translationBuilder.add("item.minecraft.lingering_potion.effect.adrenaline", "滞留型肾上腺素药水");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.adrenaline", "肾上腺素药箭");
			
			translationBuilder.add("item.minecraft.potion.effect.adrenaline_ii", "肾上腺素药水 II");
			translationBuilder.add("item.minecraft.splash_potion.effect.adrenaline_ii", "喷溅型肾上腺素药水 II");
			translationBuilder.add("item.minecraft.lingering_potion.effect.adrenaline_ii", "滞留型肾上腺素药水 II");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.adrenaline_ii", "肾上腺素药箭 II");
			
			translationBuilder.add("item.minecraft.potion.effect.long_adrenaline", "肾上腺素药水 (延长)");
			translationBuilder.add("item.minecraft.splash_potion.effect.long_adrenaline", "喷溅型肾上腺素药水 (延长)");
			translationBuilder.add("item.minecraft.lingering_potion.effect.long_adrenaline", "滞留型肾上腺素药水 (延长)");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.long_adrenaline", "肾上腺素药箭 (延长)");

			translationBuilder.add("jei.tuanzis_mod.adrenaline_potion.description", "【肾上腺素药水】\n一种能瞬间激发潜能、大幅提升行动与攻击速度的强效药剂，但会导致严重的透支反噬。\n\n§e饮用效果：§r\n1. §b极限加成§r：饮用后攻击速度提升 §a15% * 等级§r，移动速度提升 §a7.5% * 等级§r。\n2. §b持续时间§r：普通版 45 秒，增强版 II 级 30 秒，延长版 1 分 30 秒。\n\n§e透支与惩罚机制：§r\n1. §b立即透支§r：饮用时立即获得一层§c“肾上腺素透支”§r效果（持续至药水Buff结束）。\n2. §b无法消除§r：透支效果§e无法被牛奶清除§r。如果在药水Buff结束时仍处于透支状态，将触发透支副作用。\n3. §b副作用表现§r：每级透支给予 §c30 秒的虚弱 I + 挖掘疲劳 I§r。\n4. §b连续饮用叠加§r：若在透支效果未结束时连续饮用，会§6叠加透支效果的等级与时间§r。每多喝一瓶，透支等级提升 1 级（最高 IV 级），结束时的副作用持续时间也随之线性叠加（I级30秒，II级60秒，III级90秒，IV级120秒）。");

			String wolfCommandDesc = "【战狼护符】\n" +
				"一个能够赐予驯养战狼无穷威能与生命力的护符。\n\n" +
				"§e持有效果（必须置于副手）：§r\n" +
				"1. §b生命加成§r：你所有驯服的狼获得额外 §a+4§r 点最大生命值。\n" +
				"2. §b撕裂利爪§r：狼的攻击有 §a15%§r 概率使目标进入 §c撕裂§r 状态，持续 8 秒。若目标已处于撕裂状态，则刷新时间并增加 1 级（最高 4 级）。\n" +
				"3. §b不死战意§r：当狼受到致命伤害时，不会死亡，而是会获得 §e5 秒的无敌与激怒§r（攻击速度翻倍，移动和攻击力增强），随后狼会力竭倒地并消耗护符 1 点耐久。\n\n" +
				"§e倒地状态：§r\n" +
				"倒地的狼无法移动、无法攻击、免疫一切常规伤害且禁止使用拴绳牵引。喂食其 §e腐肉 10 次§r 即可使其满血复活！\n\n" +
				"§e状态效果 - 撕裂：§r\n" +
				"每 0.5s（10 ticks）判定一次。若在此期间实体移动，则受到 §c0.5 * 等级§r 点的魔法伤害；若不移动则不受伤害。\n\n" +
				"§c注意：护符无法修复，耐久上限为 3，最大堆叠为 1。§r";
			translationBuilder.add("jei.tuanzis_mod.wolf_command.description", wolfCommandDesc);

			String seekingArrowDesc = "【追踪箭附魔】\n" +
				"仅适用于弩的宝藏级附魔。在瞄准与射击时，赋予弩神秘的弹道修正能力。\n\n" +
				"§e附魔效果：§r\n" +
				"1. §b目标高亮§r：手持已装填此附魔的弩瞄准时，被瞄准的生物会在客户端高亮显示（仅限瞄准者本人可见）。\n" +
				"2. §b弹道修正§r：箭矢射出后，在 0.5 秒（10 ticks）后，向射出瞬间准星瞄准的高亮实体（若有）或指向的坐标点（若无实体）进行一次不超过 30 度的自动修正。优先追踪实体！\n\n" +
				"§e使用限制：§r\n" +
				"1. §b单次修正§r：每支箭矢在飞行中仅可且仅会进行一次修正，无法在空中拐大弯。\n" +
				"2. §b移动预判§r：对移动的目标具有预判逻辑（基于发射时目标的移动向量，推算 0.5 秒后的位置），但无法追踪目标的加速度或运动方向改变（若目标在修正后改变方向，箭矢仍会射失）。\n" +
				"3. §b互斥机制§r：与 §b多重射击 (Multishot)§r 互斥。\n\n" +
				"§7获取途径：属于“宝藏附魔”，无法通过附魔台获得，也无法与村民交易。仅能从野外遗迹、要塞等宝箱中开出。";
			translationBuilder.add("jei.tuanzis_mod.seeking_arrow.description", seekingArrowDesc);
			translationBuilder.add("enchantment.tuanzis_mod.seeking_arrow", "追踪箭");

			// 手里剑与地狱炖菜中文翻译
			translationBuilder.add(ModItems.SHURIKEN, "手里剑");
			translationBuilder.add(ModStatusEffects.SHURIKEN_STUCK.value(), "手里剑嵌入");
			
			String shurikenDesc = "【手里剑】\n" +
				"一种轻巧且飞行速度极快的极速远程投掷武器。\n\n" +
				"§e特殊投掷：§r\n" +
				"1. §b极速普通投掷§r：伤害为 2，飞行速度是箭的 1.5 倍，无任何抛物线下坠。单发右键有 0.2 秒的投掷冷却。\n" +
				"2. §b潜行三连发§r：按住潜行 (Shift) + 右键可一次性消耗 3 枚，向前方的扇形区域射出 3 枚手里剑。每发手里剑独立计算并造成伤害。使用后进入 0.6 秒的额外整体冷却。\n\n" +
				"§e手里剑嵌入负面效果：§r\n" +
				"1. §b移动惩罚§r：击中目标时，有 20% 的概率对目标施加“手里剑嵌入”效果。每一枚嵌入的手里剑使目标移动速度 -10%，最多嵌入 3 枚（即移速 -30% 限制）。\n" +
				"2. §b时间刷新§r：再次命中目标时，会将目标身上所有已嵌入的手里剑的持续时间全部刷新回 8 秒。\n" +
				"3. §b物品脱落§r：8秒内未刷新，手里剑会自动从目标身上脱落，掉落在目标脚下成为可拾取实体。\n" +
				"4. §bPVP脱落对抗§r：玩家被嵌入时，每次受到伤害有 15% 概率脱落 1 枚手里剑，每次挥动作战手臂有 10% 概率脱落 1 枚手里剑。";
			translationBuilder.add("jei.tuanzis_mod.shuriken.description", shurikenDesc);

			translationBuilder.add(ModItems.NETHER_STEW, "地狱炖菜");
			translationBuilder.add(ModStatusEffects.HELL_FIRE.value(), "地狱之火");
			translationBuilder.add(ModStatusEffects.BLAZING_HUNGER.value(), "烈焰饥渴");
			translationBuilder.add(ModStatusEffects.HYDROPHOBIA.value(), "惧水");
			translationBuilder.add("death.attack.tuanzis_mod.hydrophobia", "%s 碰到了水，痛苦地蒸发了");

			String netherStewDesc = "【地狱炖菜】\n" +
				"一种由地狱火种提炼而成的魔化热辣炖菜，可堆叠至 64 个，食用后返还空碗。食用后将赋予玩家持续 1 分 30 秒的双刃剑式三大状态效果：\n\n" +
				"§e正面效果 - 地狱之火：§r\n" +
				"1. 获得完全的§6免疫火焰与岩浆伤害§r能力。\n" +
				"2. 近战攻击有 60% 概率点燃目标 2 秒。\n" +
				"3. 攻击处于着火状态的目标时，近战伤害额外 +1 点。\n\n" +
				"§c副作用 1 - 烈焰饥渴：§r\n" +
				"玩家的饱食度与饱和度消耗速度翻倍。\n\n" +
				"§c副作用 2 - 惧水：§r\n" +
				"1. 玩家变得极度惧怕水源。在水中（或雨中）时每 1 秒受到 2 点伤害。\n" +
				"2. 此伤害属于真实伤害，无视任何护甲，且绝对不会因为任何抗性提升或伤害吸收效果而减少！";
			translationBuilder.add("jei.tuanzis_mod.nether_stew.description", netherStewDesc);
			
			// 处决与连锁苦痛中文翻译
			translationBuilder.add("enchantment.tuanzis_mod.execute", "处决");
			translationBuilder.add("death.attack.tuanzis_mod.execute_backlash", "%s 无法承受自身杀意的反噬");
			String executeDesc = "【处决附魔】\n" +
				"一种可附魔于剑和斧上的强力战斗型附魔。通过引导自身的狂乱杀意对低生命值目标进行绝对斩杀！\n\n" +
				"§e附魔效果：§r\n" +
				"当攻击目标的当前生命百分比低于指定阈值时触发，近战伤害获得极大比例的爆发式乘算增幅！\n" +
				"1. §a等级 I§r：生命值低于 §e15%§r 触发，近战伤害提升 §a+25%§r。\n" +
				"2. §a等级 II§r：生命值低于 §e25%§r 触发，近战伤害提升 §a+50%§r。\n" +
				"3. §a等级 III§r：生命值低于 §e35%§r 触发，近战伤害提升 §a+75%§r。\n\n" +
				"§c负面反噬代价 - 嗜血反噬：§r\n" +
				"1. 每次成功触发处决斩杀伤害提升时，自身的杀意也会疯狂反噬自身！玩家自身将立刻受到伤害：\n" +
				"   §c等级 * 1 点（I级1点，II级2点，III级3点）的真实伤害§r。\n" +
				"2. 该反噬伤害为绝对真实伤害，无视任何护甲，且绝对不会因为任何抗性提升或伤害吸收效果而减少！极其致命！\n\n" +
				"§e冲突与限制：§r\n" +
				"与常规增伤附魔 §b锋利 (Sharpness)§r、§b亡灵杀手 (Smite)§r、§b节肢杀手 (Bane of Arthropods)§r 互斥。";
			translationBuilder.add("jei.tuanzis_mod.execute.description", executeDesc);

			translationBuilder.add("enchantment.tuanzis_mod.chain_pain", "连锁苦痛");
			translationBuilder.add("death.attack.tuanzis_mod.chain_pain", "%s 无法承受溢出的苦痛冲击波");
			String chainPainDesc = "【连锁苦痛附魔】\n" +
				"仅可附魔于斧头武器的宝藏级稀有战斗附魔。能将击杀瞬间产生的多余溢出苦痛，以波荡形式无情链击并传导给周围所有的敌人！\n\n" +
				"§e附魔效果：§r\n" +
				"使用附有此效果的斧头击杀任意敌人时，计算这次致死一击产生的溢出伤害（即实际造成的最终伤害减去目标被杀前的剩余生命值），并对其周围 §a5 格§r 范围内的其它所有敌人造成该溢出伤害的 §c50%§r。\n" +
				"1. 苦痛传导会自动从近到远依次选择周围最多 §e5 个§r 敌对目标进行连锁轰击。\n" +
				"2. 苦痛涟漪为溅射类型，能一次性对群体敌人造成毁灭性的清场扫尾！\n\n" +
				"§e规则限制与互斥：§r\n" +
				"1. §b无循环套娃§r：由该涟漪造成的传导伤害，绝对不会再次触发连锁苦痛效果，防范无限套娃崩盘。\n" +
				"2. §b宝藏附魔§r：无法通过附魔台附魔获取，也无法与村民交易，仅能通过搜刮地牢、要塞、远古城市箱子产出。\n" +
				"3. §b冲突互斥§r：与常规工具附魔 §b精准采集 (Silk Touch)§r 互斥。";
			translationBuilder.add("jei.tuanzis_mod.chain_pain.description", chainPainDesc);

			String buzzingRhythmDesc = "【蜂鸣节律附魔】\n" +
				"一种可附魔于剑类武器的稀有战斗型附魔。可以通过极富节奏的连续打击极大地强化自身的攻击伤害！\n\n" +
				"§e附魔效果：§r\n" +
				"1. §b节律增伤§r：当持有者使用带有此附魔的武器连续攻击带有“蜂毒”或“蜂毒冷却”的目标时，每次攻击命中后叠加一层“节律”状态，持续 3 秒，最多叠加 1 + 等级（最高 5）层。每层节律使持有者的攻击伤害额外提升 §a3%§r（最高提升 15% 伤害）。\n" +
				"2. §b时间过期§r：若 3 秒内未命中该带毒目标，节律层数立刻清零。\n" +
				"3. §b触及范围加成§r：每一级附魔将为持有者额外增加 §b0.0625 格§r 的触及距离（最高增加 0.25 格）。\n\n" +
				"§e冲突与限制：§r\n" +
				"与常规战斗附魔 §b横扫之刃 (Sweeping Edge)§r、§b火焰附加 (Fire Aspect)§r、§b击退 (Knockback)§r 互斥。";
			translationBuilder.add("jei.tuanzis_mod.buzzing_rhythm.description", buzzingRhythmDesc);

			// 抽卡物品中文翻译
			translationBuilder.add(ModItems.STAR_TRAVEL_CARD_PACK, "星旅卡牌包");
			translationBuilder.add(ModItems.STAR_TRAVEL_CARD_CHEST, "星旅卡牌箱");
			translationBuilder.add(ModItems.SAKURA_FESTIVAL_CARD_PACK, "樱花祭卡牌包");
			translationBuilder.add(ModItems.SAKURA_FESTIVAL_CARD_CHEST, "樱花祭卡牌箱");

			// 裂虚之痕中文翻译
			translationBuilder.add(ModItems.RIFT_SCAR, "裂虚之痕");
			translationBuilder.add("item.tuanzis_mod.rift_scar.wear", "磨损值: %s");
			translationBuilder.add("item.tuanzis_mod.rift_scar.unidentified", "§7[未鉴定磨损度]");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.1", "外观: §a无形");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.2", "外观: §e涟漪");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.3", "外观: §6蚀痕");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.4", "外观: §c崩解");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.5", "外观: §4残响");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.1", "§7刚从虚空抽出，锋刃无形，完美无瑕。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.2", "§7剑脊浮现虚空涟漪，切割轨迹略带滞涩。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.3", "§7裂痕如蛛网蔓延，剑身呈现半透明紫色。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.4", "§7结构剧烈波动，每次挥砍都崩落细碎虚空尘。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.5", "§7几乎完全透明，仅余一道暗影轮廓，轻触即散。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.1", "§8在末地外沿的虚无夹缝中，一位末影骑士将自身的暗影");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.2", "§8与折断的龙息碎片融合，锻造出这柄剑。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.3", "§8它能察觉敌人护甲的缺失，一旦目标毫无防备，");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.4", "§8剑刃便穿透现实，直接撕扯灵魂。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.5", "§8据说剑中的虚空回响渴求着每次完美的切割。");
			translationBuilder.add("item.tuanzis_mod.rift_scar.shift_hint", "§7[按住 Shift 展示详细故事与磨损详情]");
			
			String riftScarDesc = "【裂虚之痕】\n在末地外沿的虚无夹缝中，一位末影骑士将自身的暗影与折断的龙息碎片融合，锻造出这柄剑。它能察觉敌人护甲的缺失，一旦目标毫无防备，剑刃便穿透现实，直接撕扯灵魂。据说剑中的虚空回响渴求着每次完美的切割。\n\n§e属性：§r\n1. §a攻击力§r: 5.12 点。\n2. §a攻击速度§r: 2.0。\n\n§e虚无切割被动：§r\n1. 当攻击的目标当前护甲值为 0 时，你造成的伤害会提升至 175%。\n2. 若目标拥有护甲，则伤害保持原始值。\n\n§e磨损度外观机制：§r\n此剑的磨损度在诞生的那一刻便已随机决定 (0-1)，永久不可变更，与耐久度无关。不同的磨损度对应五种不同的外观阶段 (无形、涟漪、蚀痕、崩解、残响) 并在挥动/击中时具有独特的粒子和音效。";
			translationBuilder.add("jei.tuanzis_mod.rift_scar.description", riftScarDesc);

			// 钢御壁垒 (Steel Barrier) 与 坚盾之赐 (Steel Shield Gift) 翻译
			translationBuilder.add(ModItems.STEEL_BARRIER, "钢御壁垒");
			translationBuilder.add(ModStatusEffects.STEEL_SHIELD.value(), "钢盾");
			translationBuilder.add("enchantment.tuanzis_mod.steel_shield_gift", "坚盾之赐");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.wear", "磨损值: %s");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.unidentified", "§7[未鉴定磨损度]");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.1", "外观: §a崭新铸成");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.2", "外观: §e初经战阵");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.3", "外观: §6裂痕蔓延");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.4", "外观: §c千钧壁垒");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.5", "外观: §4濒毁残御");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.1", "§7刀身镜面般光洁，钢盾符文流转不息，散发出坚实的金属光泽。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.2", "§7刃口微卷，守护铭文泛起热锻青光，散发战斗初期的温热感。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.3", "§7护手处隐现发丝细纹，剑脊上的钢盾似在低鸣。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.4", "§7剑身斑驳蚀刻，布满裂痕，每道伤痕都记录着守卫的不屈。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.5", "§7符文破碎闪烁，钢盾虚影薄如蝉翼，似乎随时都会消散。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.1", "§8这是最后一位城墙守卫遗留的断刃之剑。其身虽殒，其志犹存。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.2", "§8匠人以秘法将骑士残魂与他的碎盾熔入剑脊，重新锻造。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.3", "§8挥动此剑时，钢盾虚影在身周流转加持，牢不可破。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.4", "§8一如当年，他以身为盾，不退半步。");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.shift_hint", "§7[按住 Shift 展示详细故事与磨损详情]");
			
			String steelBarrierJeiDesc = "【钢御壁垒】\n最后一位城墙守卫的遗剑，其身虽殒，其志犹存。匠人以秘法将骑士残魂与碎盾熔入剑脊，挥动时钢盾虚影加身，一如当年他以身为盾，不退半步。\n\n§e属性：§r\n1. §a攻击力§r: 7.7 点。\n2. §a攻击速度§r: 1.6。\n\n§e钢盾被动：§r\n1. 每次持剑攻击命中目标时，获得1层“钢盾”状态效果，最高叠加至 5 层。\n2. 每层钢盾提供 §a+2 护甲值§r（可突破原版20上限）。\n3. 持续时间（5秒）结束后，钢盾层数衰减 1 层，衰减后仍持续 5 秒，而非直接消失，形成平缓的防御消退。";
			translationBuilder.add("jei.tuanzis_mod.steel_barrier.description", steelBarrierJeiDesc);
			
			String steelShieldGiftJeiDesc = "【坚盾之赐】\n最大等级 IV。对近战武器与工具生效。此附魔与其他归一化附魔互斥。\n\n§e属性：§r\n1. 提供 §a+0.5 × 等级 护甲值§r。\n2. 当持有者的护甲值超过 20 点时，每 1 点溢出护甲值使你的攻击伤害增加 §c0.0625 / 0.125 / 0.1875 / 0.25 × 溢出点数§r 的数值。";
			translationBuilder.add("jei.tuanzis_mod.steel_shield_gift.description", steelShieldGiftJeiDesc);

			// 蜂刺余响翻译
			translationBuilder.add(ModItems.BEE_STING_ECHO, "蜂刺余响");
			translationBuilder.add(ModStatusEffects.BEE_POISON.value(), "蜂毒");
			translationBuilder.add(ModStatusEffects.BEE_POISON_COOLDOWN.value(), "蜂毒冷却");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.wear", "磨损值: %s");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.unidentified", "§7[未鉴定磨损度]");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.1", "外观: §a初振之锋");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.2", "外观: §e微疲之纹");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.3", "外观: §6劳损之痕");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.4", "外观: §c衰振之音");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.5", "外观: §4残响空巢");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.1", "§7剑身薄如蝉翼，呈现半透明的琥珀金色，刃纹呈密集的六边形网格状。挥动时空中残留金色残影。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.2", "§7琥珀金色变淡为蜂蜜色，不再透明。六边形刃纹有几处断续，护手翼膜上现出数道放射状裂纹。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.3", "§7剑身呈现磨砂质感的老蜜色，整体黯淡无光。剑刃边缘有细碎锯齿状缺口，护手一侧完全穿孔。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.4", "§7剑身已呈暗褐色的古旧蜜蜡色，表面布满风化般的微孔。刃纹完全消失，取而代之的是无序分布的细密裂纹。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.5", "§7整把剑呈焦黑色，覆盖着蓝紫色氧化斑。原本的薄刃上布满贯穿孔洞，大小不一，边缘融化成泪滴状。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.1", "§8在某个被遗忘的蜂语者的传说中，这把剑由蜂后的最后一根螫针锻造而成。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.2", "§8当蜂巢覆灭、族群消亡之际，蜂后将毕生未释放的刺注入一块凡铁，于是有了这把薄刃。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.3", "§8每位持剑者在第一次握紧它时，都会感到掌心传来微弱的持续振动。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.4", "§8它在高速交击中将对手拖入无尽的刺痛节奏，直到敌人发现伤口已多到无从包扎。");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.shift_hint", "§7[按住 Shift 展示详细故事与磨损详情]");
			translationBuilder.add("death.attack.tuanzis_mod.bee_sting_explosion", "%s 被引爆的蜂毒噬体而亡");
			translationBuilder.add("death.attack.tuanzis_mod.bee_sting_explosion.player", "%s 在与 %s 的战斗中，被满层蜂毒的爆破真实伤害终结了生命");
			
			String beeStingEchoDesc = "【蜂刺余响】\n由蜂后的最后一根螫针锻造而成的绝影薄刃。拥有极快的攻速与独特的叠刺爆毒被动。\n\n§e属性：§r\n1. §a攻击力§r: 7 点（比下界合金剑低 1 点）。\n2. §a攻击速度§r: 2.1（显著快于下界合金剑的 1.6）。\n3. §a限制§r: 该武器的攻击距离缩短 0.25 格，且无法触发横扫攻击。\n\n§e叠刺被动：§r\n1. 每次成功命中目标时，为其叠加一层 §6蜂毒§r 效果，持续 5 秒，最高叠加 5 层。\n2. 每层蜂毒使目标受到的下一次伤害提升 §a3%§r（对真实伤害同样有效）。如果是非蜂刺余响造成的伤害，会在触发增幅后立即消耗（清空）所有层数。\n3. 当蜂毒叠满 5 层时，会立刻引爆所有层数，造成合计 §c15%§r 该次命中最终伤害的§4额外真实伤害§r，并清空层数。\n4. 引爆后，目标进入 §e8 秒的引爆冷却§r 状态，期间无法再被叠加蜂毒。\n\n§e磨损度外观机制：§r\n此剑的磨损度在诞生的那一刻便已随机决定 (0-1)，永久不可变更，与耐久度无关。不同的磨损度对应五种不同的外观阶段 (初振之锋、微疲之纹、劳损之痕、衰振之音、残响空巢) 以及不同频率的蜂鸣声。";
			translationBuilder.add("jei.tuanzis_mod.bee_sting_echo.description", beeStingEchoDesc);

			// 虚无共鸣 (Void Resonance) 翻译
			translationBuilder.add("enchantment.tuanzis_mod.void_resonance", "虚无共鸣");
			String voidResonanceJeiDesc = "【虚无共鸣】\n最大等级 V。攻击护甲为0的目标时，攻击力增加 (0.8×等级) × (攻击速度 ÷ 武器基础攻击力) 点。此效果对所有武器生效方式完全相同。此附魔与其他归一化附魔互斥。";
			translationBuilder.add("jei.tuanzis_mod.void_resonance.description", voidResonanceJeiDesc);

			// 幽匿裂片（Scully Shard）与 共振脉冲（Resonance Pulse）翻译
			translationBuilder.add(ModItems.SCULLY_SHARD, "幽匿裂片");
			translationBuilder.add(ModStatusEffects.RESONANCE.value(), "共鸣");
			translationBuilder.add("enchantment.tuanzis_mod.resonance_pulse", "共振脉冲");
			translationBuilder.add("item.tuanzis_mod.scully_shard.wear", "磨损值: %s");
			translationBuilder.add("item.tuanzis_mod.scully_shard.unidentified", "§7[未鉴定磨损度]");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.1", "外观: §a深空回响");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.2", "外观: §e微纹共鸣");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.3", "外观: §6脉动裂痕");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.4", "外观: §c衰变尖啸");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.5", "外观: §4静默残片");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.1", "§7剑身如深空水晶般透彻，内部可见缓慢流动的幽蓝色星云状光雾。剑格回响碎片晶莹饱满，散发柔和的蓝白色脉冲光晕。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.2", "§7透明剑身开始出现细微的银色冰裂纹，光泽略微变暗，星云光雾的流速加快且略显紊乱。剑格碎片边缘出现细小的暗色蚀点，光晕转为不稳定的间歇式闪烁。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.3", "§7剑身内部裂纹交织成网状，幽蓝光雾已混入暗紫色紊流，仿佛被困的能量在挣扎。剑格碎片表面布满深浅不一的蚀痕，脉冲光晕变得暗淡且失去规律，偶尔会出现短暂的暗灭。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.4", "§7剑身浑浊发黑，只有少数裂纹间隙仍透出垂死挣扎的微弱蓝光，大部分区域呈现幽匿块原生的暗绿色。剑格碎片边缘崩缺，蚀痕扩散如霉斑。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.5", "§7剑身几乎完全被暗色裂隙覆盖，仅剩极细如发丝的光线证明幽能尚未彻底消散。剑格碎片色泽浑浊且严重缺损，光晕已彻底消失。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.1", "§8由深暗之域深处的幽匿核心碎片与凝聚的回响结晶炼制。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.2", "§8它能捕捉并放大空气中细微的声波振动，与目标的灵魂产生不可磨灭的共鸣。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.3", "§8当共鸣被打破时，积压的音波脉冲将如超声轰击般，在虚空中撕碎一切敌人的防御。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.4", "§8然而，幽能的反噬也在无情地侵蚀着剑身，直至万籁俱寂，空留残片。");
			translationBuilder.add("item.tuanzis_mod.scully_shard.shift_hint", "§7[按住 Shift 展示详细故事与磨损详情]");
			
			String scullyShardJeiDesc = "【幽匿裂片】\n来自深暗之域的幽能魔刃，拥有专属的音波共鸣被动与独特的磨损特性。\n\n" +
				"§e属性：§r\n" +
				"1. §a攻击力§r: 10 点。\n" +
				"2. §a攻击速度§r: 1.25。\n\n" +
				"§e固有机制——“幽匿共鸣”：§r\n" +
				"1. 每次命中目标时，对其施加持续 2.5 秒的 §b“共鸣”§r 状态（不叠加层数，可刷新持续时间）。\n" +
				"2. 若命中的目标已带“共鸣”，则消耗并移除共鸣，对其额外造成 §c2 点音波魔法伤害§r（无视护甲，触发时可与共振脉冲附魔产生强力联动）。\n\n" +
				"§e磨损度外观机制：§r\n" +
				"1. 本剑在诞生时会随机生成磨损度 (0-1)，永久不可变更。不同的磨损度对应五种外观阶段，并在击中时播放不同音效。";
			translationBuilder.add("jei.tuanzis_mod.scully_shard.description", scullyShardJeiDesc);

			String resonancePulseJeiDesc = "【共振脉冲附魔】\n仅可附魔于剑类武器的音波附魔，能够与幽匿裂片的共鸣被动完美联动。\n\n" +
				"§e附魔效果：§r\n" +
				"1. §b共鸣施加§r：每次命中时，有 §a10% * 等级§r 的概率对目标施加持续 2.5 秒的 §b“共鸣”§r 状态。\n" +
				"2. §b脉冲爆发§r：当攻击命中已带有“共鸣”的目标时，立即消耗共鸣并产生一次共振脉冲，对目标及周围 §a2格§r 内所有敌人造成 §c0.2 + 0.4 * 等级§r 点音波魔法伤害（无视护甲，无冷却，可多次连环触发）。";
			translationBuilder.add("jei.tuanzis_mod.resonance_pulse.description", resonancePulseJeiDesc);

			String tideCleaverJeiDesc = "【潮汐切割者】\n一把凝结了海洋能量的奇特武器，具有独特的“潮汐节拍”被动与磨损机制。\n\n§e潮汐节拍：§r\n1. §b合拍窗口§r：攻击冷却在 §a80% 至 100%§r，或完全充满后的 §e0.2 秒 (4 ticks)§r 内。在此期间攻击为“合拍攻击”，获得 1 层潮汐充能（最高 5 层），每层提供 §a+2% 近战伤害§r。\n2. §b失拍攻击§r：在冷却低于 §c80%§r 时攻击，伤害降低 §c15%§r 并失去 1 层充能。\n3. §b正常攻击§r：冷却满超过 0.2 秒后攻击，伤害无修正，层数不变。\n\n§e被动效果：§r\n1. §b潮汐侵蚀§r：当持有的充能层数 §a>= 2 层§r 时，每次近战攻击会使目标获得 2 秒“潮汐侵蚀”效果，每秒造成 §c1 点魔法伤害§r。\n2. §b潮涌爆发§r：充能达到 5 层时，下一次合拍攻击额外造成 §c3点无视护甲魔法伤害§r，随后充能重置为 1。该爆发有 §e7 秒内置冷却§r。\n\n§e修复与维护：§r\n可在铁砧中使用§b海洋之心§r修复其耐久度。";
			translationBuilder.add(ModItems.TIDE_CLEAVER, "潮汐切割者");
			translationBuilder.add(ModStatusEffects.TIDE_EROSION.value(), "潮汐侵蚀");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.wear", "磨损值: %s");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.unidentified", "§7[未鉴定磨损度]");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.1", "外观: §a永澜之锋");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.2", "外观: §e浅湾余波");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.3", "外观: §6暗礁潜流");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.4", "外观: §c退潮裂礁");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.5", "外观: §4枯潮死心");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.1", "§7剑身呈现完美的深海钴蓝至浅海碧色渐变，虹彩珍珠光泽流转不息。剑格内海洋之心饱满剔透，液态能量缓缓涨落，边缘清晰。剑刃无任何划痕，在阳光下可见波浪纹理的反光。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.2", "§7剑刃边缘出现极细微的磨损痕迹，渐变色彩稍微变得柔和，虹彩减弱。海洋之心依旧充盈，但液态能量涨落时偶尔出现微小的暗色波痕，如浅湾底部的沙纹。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.3", "§7剑身出现数道可见划痕，刃部有轻微卷口，整体色调偏向灰蓝，仿佛被海底暗流侵蚀。海洋之心内部出现絮状浑浊物，能量流动不再顺畅，偶尔停滞。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.4", "§7剑身裂纹扩大，几处刃口崩缺，渐变涂层大面积剥落，露出暗沉的金属底色。海洋之心表面出现明显裂痕，液态能量持续向外泄漏为微弱光点。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.5", "§7剑身多处结构性断裂，仅靠残余的魔法能量勉强维系在一起，断裂处透出微弱的蓝光。海洋之心彻底暗淡，几乎停止旋转，内部只剩下最后一点微弱的荧光。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.1", "§8凝聚了满月下深海的潮汐之力与深渊的心脏。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.2", "§8每一次剑刃的划过都如同潮汐的涨落，跟随不可名状的节律而动。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.3", "§8在完美的节拍中攻击，涌动的浪潮将撕裂敌人的防线。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.4", "§8但若节奏混乱，失控的洋流亦会反噬武器本身，致其磨损破败。");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.shift_hint", "§7[按住 Shift 展示详细故事与磨损详情]");
			translationBuilder.add("jei.tuanzis_mod.tide_cleaver.description", tideCleaverJeiDesc);

			translationBuilder.add("enchantment.tuanzis_mod.abyssal_rhythm", "深渊律动");
			String abyssalRhythmDesc = "【深渊律动附魔】\n" +
				"一种可附魔于剑类武器的通用战斗型附魔。可以通过与潮汐力量产生共鸣造成额外的魔法伤害！\n\n" +
				"§e附魔效果：§r\n" +
				"1. §b深渊震击§r：每次用剑攻击命中目标时，必定触发一次深渊律动，对主目标造成额外魔法伤害。无内置冷却。\n" +
				"2. §b伤害计算§r：\n" +
				"   额外伤害 = 基础伤害 + 潮汐共鸣加成\n" +
				"   §a基础伤害§r：根据等级而定 (I级0.1, II级0.2, III级0.25, IV级0.3)\n" +
				"   §a潮汐共鸣加成§r：仅在持有潮汐充能层数（来自潮汐切割者）时生效，加成 = 0.05 × 附魔等级 × 当前层数。\n" +
				"   在 5 层充能下，IV 级附魔的共鸣加成可额外提供 1.0 点伤害！\n\n" +
				"§e获取方式：§r\n" +
				"最高等级 IV 级。通用剑附魔，可与其它增伤附魔兼容。";
			translationBuilder.add("jei.tuanzis_mod.abyssal_rhythm.description", abyssalRhythmDesc);

			// 潮汐织靴翻译
			translationBuilder.add(ModItems.TIDAL_WEAVE_BOOTS, "潮汐织靴");
			translationBuilder.add("item.tuanzis_mod.tidal_weave_boots.tooltip", "由海晶砂与幻翼膜编织，流淌着海洋之心的脉动光晕。");
			translationBuilder.add("jei.tuanzis_mod.tidal_weave_boots.description", "【潮汐织靴】\n一双由海晶砂与幻翼膜编织的轻盈长靴，靴面流淌着海洋之心的脉动光晕。\n\n§e功能与效果：\n1. §b凌波微步：装备在靴子栏时，能如踩在陆地上一般在水面上自由行走、疾跑和跳跃。\n2. §b潜水切换：在水面行走时，按住潜行键（Shift）可主动沉入水中进行游泳.放开并游回水面后，会自动重新恢复在水面行走的能力。\n3. §b摔落免伤：从高空落下触及水面时，免受所有摔落伤害。");

			// 不死药水翻译
			translationBuilder.add("jei.tuanzis_mod.undying_potion.update_103", "【不死药水】\n赋予“不死”状态效果的药水，拥有普通与延长版变体。\n\n§e1.0.3 更新说明：§r\n现在“不死”状态效果仅对 §b玩家§r 以及 §b被玩家驯服的生物§r（如驯服后的狼、猫等）生效。对其它野生生物或敌对生物不再起作用，防止意外“保护”了敌人。");

			// 物品标签翻译
			translationBuilder.add("tag.item.tuanzis_mod.repairs_tidal_weave_boots", "潮汐织靴修补材料");
			translationBuilder.add(ModItems.WORLD_SCULPTORS_PEN, "塑世之笔");
			translationBuilder.add("tooltip.tuanzis_mod.world_sculptors_pen.mode", "当前填充模式: %s");
			translationBuilder.add("tuanzis_mod.mode.full_replace", "完全替换");
			translationBuilder.add("tuanzis_mod.mode.air_only", "填充空气");
			translationBuilder.add("tuanzis_mod.mode.semi_replace", "半替换");
			translationBuilder.add("tuanzis_mod.mode.replace_air_only", "仅替换空气");
			translationBuilder.add("message.tuanzis_mod.world_sculptors_pen.mode_toggled", "§6塑世之笔切换为: %s 模式");
			translationBuilder.add("jei.tuanzis_mod.world_sculptors_pen.description", "【塑世之笔】\n一支由下界合金笔尖与紫水晶笔杆制成的奢华书写工具，笔身缠绕着幽匿脉络般的墨线。\n\n§e功能机制：\n1. §b批量填充：手持本笔并右键点击方块表面记录起点。在 64 格最大直线距离及 2048 个方块体积范围内，右键点击另一位置，即可瞬间在长方体区域填充选定方块。\n2. §b选定填充方块：按住 Shift + 右键点击任意方块，即可将其设为填充类型。选中的方块将显示在笔贴图的右下角。\n3. §b智能消耗：填充方块直接从主背包以及主背包中的潜影盒中扣除。若方块数量不足或笔耐久不够，操作将取消并提示。\n4. §b防刷撤销：在填充后 60 秒内，潜行 (Shift) + 右键（空气或方块）可撤销本次填充。仅还原并退回区域内“当前依然匹配的方块”，返还对应的方块及扣除的笔耐久，防范刷取漏洞！\n5. §b耐久与维护：拥有 8192 点高耐久。不支持附魔。可在铁砧中以单个虚空墨锭为材料修复其 1000 点耐久。");
			translationBuilder.add(ModItems.VOID_INK_INGOT, "虚空墨锭");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.desc_line1", "一方由龙息凝华与回响粉末压制的深紫色墨锭，");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.desc_line2", "表面流淌着末影虚空般的涡纹，边缘泛着下界合金粉末的暗金微光。");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.desc_line3", "持之凑近塑世之笔，笔身会微微震颤，仿佛墨锭在呼唤它的笔锋。");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.incompatible", "墨不相容");
			translationBuilder.add("jei.tuanzis_mod.void_ink_ingot.description", "【虚空墨锭】\n一方由龙息凝华与回响粉末压制的深紫色墨锭，表面流淌着末影虚空般的涡纹，边缘泛着下界合金粉末的暗金微光。\n\n§e墨笔一体：§r\n虚空墨锭仅匹配塑世之笔的紫水晶笔尖与回响墨囊系统，对其他工具、武器、护甲均无修复效果。放入非塑世之笔的物品时，铁砧会显示“墨不相容”的暗红提示并阻止拿取。");
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
			translationBuilder.add(ModItems.BERSERK_CHARM, "Berserk Charm");
			translationBuilder.add("item.tuanzis_mod.berserk_charm.tooltip", "Place in off-hand to activate automatically when health falls below 30%. Grants Strength II and Speed II for 15 seconds, and consumes one charm.");
			translationBuilder.add(ModItems.WOLF_COMMAND, "Wolf Command");
			translationBuilder.add("item.tuanzis_mod.wolf_command.tooltip", "Equip in off-hand to grant tamed wolves bonus health, tearing attacks, and death protection.");
			translationBuilder.add(ModItems.CRAFTSMAN_CHARM, "Craftsman Charm");
			translationBuilder.add("item.tuanzis_mod.craftsman_charm.tooltip", "Right-click to open 3x3 crafting grid. Retains items inside on close.");
			translationBuilder.add("container.tuanzis_mod.craftsman_charm", "Craftsman Charm");
			translationBuilder.add("jei.tuanzis_mod.craftsman_charm.description", "【Craftsman Charm】\nA portable crafting table. Light brown wood texture with amethyst and obsidian trims.\n\n§eFeatures:\n1. §bPortable Crafting: Right-click to open a 3x3 crafting screen directly.\n2. §bBuffer Retention: Retains all grid items inside when closed instead of dropping them.\n3. §bInteraction Restriction: The Craftsman Charm cannot be placed inside its own buffer slots. It does not interact with automation devices.\n4. §bSafety Closing: If the item is moved away from the slot that opened the screen, the GUI will close automatically.");
			translationBuilder.add(ModItems.WARDEN_HEART, "Warden Heart");
			translationBuilder.add(ModItems.ECHO_BREAKER, "Echo Breaker");

			// Gacha items translations
			translationBuilder.add(ModItems.STAR_TRAVEL_CARD_PACK, "Star Travel Card Pack");
			translationBuilder.add(ModItems.STAR_TRAVEL_CARD_CHEST, "Star Travel Card Chest");
			translationBuilder.add(ModItems.SAKURA_FESTIVAL_CARD_PACK, "Sakura Festival Card Pack");
			translationBuilder.add(ModItems.SAKURA_FESTIVAL_CARD_CHEST, "Sakura Festival Card Chest");

			// Rift Scar English translations
			translationBuilder.add(ModItems.RIFT_SCAR, "Fractured Void Scar");
			translationBuilder.add("item.tuanzis_mod.rift_scar.wear", "Wear Value: %s");
			translationBuilder.add("item.tuanzis_mod.rift_scar.unidentified", "§7[Unidentified Wear]");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.1", "Appearance: §aIntangible");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.2", "Appearance: §eRipple");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.3", "Appearance: §6Corrosion");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.4", "Appearance: §cDecaying");
			translationBuilder.add("item.tuanzis_mod.rift_scar.status.5", "Appearance: §4Echo");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.1", "§7Freshly drawn from the void, the edge is intangible and flawless.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.2", "§7Void ripples emerge on the spine, making the cutting path slightly sluggish.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.3", "§7Cracks spread like a spiderweb, and the blade is translucent purple.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.4", "§7Structure fluctuates violently, shedding fine void dust with every slash.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.desc.5", "§7Almost completely transparent, leaving only a shadow outline that dissipates at a touch.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.1", "§8In the rift of nothingness at the outer edge of the End, an End Knight fused");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.2", "§8their own shadow with shattered dragon breath fragments to forge this blade.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.3", "§8It senses the lack of armor on enemies, and once the target is defenseless,");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.4", "§8the blade pierces reality to directly tear the soul.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.lore.5", "§8It is said the void echo within the sword craves every perfect cut.");
			translationBuilder.add("item.tuanzis_mod.rift_scar.shift_hint", "§7[Hold Shift for details & lore]");
			translationBuilder.add("jei.tuanzis_mod.rift_scar.description", "A void sword forged by an End Knight. Deals 175% damage when the target has 0 armor. Random wear appearance on birth.");

			// Bee Sting Echo translations
			translationBuilder.add(ModItems.BEE_STING_ECHO, "Bee Sting Echo");
			translationBuilder.add(ModStatusEffects.BEE_POISON.value(), "Bee Poison");
			translationBuilder.add(ModStatusEffects.BEE_POISON_COOLDOWN.value(), "Bee Poison Cooldown");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.wear", "Wear Value: %s");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.unidentified", "§7[Unidentified Wear]");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.1", "Appearance: §aFirst Vibration");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.2", "Appearance: §eSlight Fatigue");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.3", "Appearance: §6Worn Scratches");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.4", "Appearance: §cDecaying Buzz");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.status.5", "Appearance: §4Echoing Empty Hive");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.1", "§7The blade is as thin as a cicada's wing, showing a translucent amber-gold color with a dense hexagonal grid pattern. Gold trail left in midair.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.2", "§7Amber-gold fades to opaque honey color. Hexagonal blade pattern has some breaks, guard wing has radial cracks.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.3", "§7The blade shows a matte old-honey color, overall dim. The blade edge has tiny jagged notches, one guard wing is perforated.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.4", "§7Opaque dark brown color. The blade surface is full of weathered pores. Blade patterns completely disappear, replaced by unordered cracks.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.desc.5", "§7The blade is charred black, covered in blue-purple burn stains. The blade is full of holes, and the guard is just charred frames.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.1", "§8In the legend of a forgotten bee-whisperer, this blade was forged from the queen bee's last stinger.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.2", "§8When the hive fell and the colony died, the queen injected her unspent sting into a piece of common iron.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.3", "§8Every wielder feels a slight constant vibration in their palm—not the sword shaking, but it following the ancient rhythm.");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.lore.4", "§8It drags opponents into a rhythm of endless pain in high-speed clashes, until they find too many wounds to bandage.");
			translationBuilder.add("death.attack.tuanzis_mod.bee_sting_explosion", "%s was destroyed by detonating bee poison");
			translationBuilder.add("death.attack.tuanzis_mod.bee_sting_explosion.player", "%s was terminated by the detonated bee poison in a battle with %s");
			translationBuilder.add("item.tuanzis_mod.bee_sting_echo.shift_hint", "§7[Hold Shift for details & lore]");
			translationBuilder.add("jei.tuanzis_mod.bee_sting_echo.description", "A thin blade forged from the queen bee's last stinger. Extremely fast attack speed and poison accumulation.");

			// Void Resonance translations
			translationBuilder.add("enchantment.tuanzis_mod.void_resonance", "Void Resonance");
			translationBuilder.add("jei.tuanzis_mod.void_resonance.description", "Void Resonance: Max Level V. When attacking a target with 0 armor, increases attack damage by (0.8 * level) * (attack speed / base weapon damage). Works identically on all weapons. Exclusive with other normalization enchantments.");

			// Steel Barrier & Steel Shield Gift translations
			translationBuilder.add(ModItems.STEEL_BARRIER, "Steel Barrier");
			translationBuilder.add(ModStatusEffects.STEEL_SHIELD.value(), "Steel Shield");
			translationBuilder.add("enchantment.tuanzis_mod.steel_shield_gift", "Gift of the Shield");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.wear", "Wear Value: %s");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.unidentified", "§7[Unidentified Wear]");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.1", "Appearance: §aPristine Forged");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.2", "Appearance: §eFirst Battle");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.3", "Appearance: §6Spreading Cracks");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.4", "Appearance: §cMottled Rampart");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.status.5", "Appearance: §4Near Ruined Shield");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.1", "§7The blade is clean and reflective, with steel shield runes flowing constantly, radiating a sturdy metallic luster.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.2", "§7The edge is slightly rolled, and the protection inscription glows with hot forged cyan light.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.3", "§7Fine hairline cracks appear on the guard, and the steel shield inside the spine hums softly.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.4", "§7Mottled and scarred blade, full of cracks, each recording the guard's unyielding spirit.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.desc.5", "§7Runes flicker and fracture, and the steel shield phantom is thin as a wing, about to fade.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.1", "§8The remnant sword of the last wall defender. Though he fell, his will remains.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.2", "§8A smith forged the knight's soul and his broken shield into the blade spine.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.3", "§8When swinging it, a phantom steel shield wraps the wielder, impenetrable.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.lore.4", "§8Just like back then, he stood as a shield, never retreating a single step.");
			translationBuilder.add("item.tuanzis_mod.steel_barrier.shift_hint", "§7[Hold Shift for details & lore]");
			translationBuilder.add("jei.tuanzis_mod.steel_barrier.description", "The sword of the last wall defender. Every hit grants a stacking Steel Shield effect (max 5 layers), each layer giving +2 armor. On expiration, layers decay one by one every 5 seconds.");
			translationBuilder.add("jei.tuanzis_mod.steel_shield_gift.description", "Gift of the Shield: Max Level IV. Grants +0.5 armor per level. When wielder's armor exceeds 20, each point of overflow armor increases attack damage by 0.0625/0.125/0.1875/0.25 point.");

			// Scully Shard & Resonance Pulse translations
			translationBuilder.add(ModItems.SCULLY_SHARD, "Scully Shard");
			translationBuilder.add(ModStatusEffects.RESONANCE.value(), "Resonance");
			translationBuilder.add("enchantment.tuanzis_mod.resonance_pulse", "Resonance Pulse");
			translationBuilder.add("item.tuanzis_mod.scully_shard.wear", "Wear Value: %s");
			translationBuilder.add("item.tuanzis_mod.scully_shard.unidentified", "§7[Unidentified Wear]");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.1", "Appearance: §aPristine Echo");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.2", "Appearance: §eFaint Resonance");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.3", "Appearance: §6Pulsing Fracture");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.4", "Appearance: §cDecaying Wail");
			translationBuilder.add("item.tuanzis_mod.scully_shard.status.5", "Appearance: §4Silent Remnant");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.1", "§7The blade is as clear as deep-space crystal, with a slow-flowing cyan nebula mist inside. Crystallized echo fragments on the guard glow softly with white-blue pulses.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.2", "§7Fine silver hairline cracks appear on the transparent blade, slightly dimming its luster. The nebula flows faster and irregularly. Tiny dark spots appear on the guard fragments.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.3", "§7Internal cracks interweave into a web. Dark purple turbulence mixes into the cyan mist. Guard pulses are dim, irregular, and briefly turn off.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.4", "§7The blade is cloudy and black, with only small teal cracks still glowing. Guard fragments are broken at the edges, and corrosion spreads like mold.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.desc.5", "§7The blade is almost entirely covered by dark fissures, with only thread-like glow lines left. The guard is heavily ruined, and its glow has vanished.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.1", "§8Forged from deep sculk core fragments and crystallized echo remnants found in the deep dark.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.2", "§8It captures and amplifies micro-vibrations in the air, creating an indelible resonance with the target's soul.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.3", "§8When the resonance breaks, the pent-up sonic waves tear through enemy defenses in a sudden supersonic burst.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.lore.4", "§8However, the sculk energy ruthlessly corrodes the blade, leaving only a silent remnant.");
			translationBuilder.add("item.tuanzis_mod.scully_shard.shift_hint", "§7[Hold Shift for details & lore]");
			translationBuilder.add("jei.tuanzis_mod.scully_shard.description", "A sculk-energy blade from the deep dark. Deals extra armor-penetrating sonic damage to resonated targets. Even when worn down to a Silent Remnant (Stage V), it remains fully functional without breaking.");
			translationBuilder.add("jei.tuanzis_mod.resonance_pulse.description", "Enchantment for swords. Grants a chance to apply Resonance on hit, and triggers a sonic boom dealing armor-penetrating damage to nearby enemies when attacking a resonated target.");

			String tideCleaverJeiDescEn = "【Tide Cleaver】\nAn exotic weapon condensed with ocean energy, featuring a unique \"Tide Beat\" passive and wear mechanism.\n\n" +
				"§eTide Beat:§r\n" +
				"1. §bOn-Beat Window§r: Attack cooldown is between §a80% and 100%§r, or within §e0.2 seconds (4 ticks)§r after fully charged. Attacking during this window is an \"On-Beat Attack\", granting 1 layer of Tide Charge (up to 5), each layer providing §a+2% Melee Damage§r.\n" +
				"2. §bOff-Beat Attack§r: Attacking when cooldown is below §c80%§r reduces damage by §c15%§r and loses 1 layer of charge.\n" +
				"3. §bNormal Attack§r: Attacking more than 0.2 seconds after fully charged has no damage modifier and does not change charge layers.\n\n" +
				"§ePassive Effects:§r\n" +
				"1. §bTide Erosion§r: When holding §a>= 2 layers§r of charge, melee attacks apply Tide Erosion to the target for 2 seconds, dealing §c1 magic damage per second§r.\n" +
				"2. §bSurging Burst§r: Upon reaching 5 layers of charge, the next On-Beat Attack deals §c3.0 armor-penetrating magic damage§r, resetting charge to 1. Cooldown: §e7 seconds§r.\n\n" +
				"§eRepair & Maintenance:§r\n" +
				"Can be repaired with the §bHeart of the Sea§r in an anvil.";
			translationBuilder.add(ModItems.TIDE_CLEAVER, "Tide Cleaver");
			translationBuilder.add(ModStatusEffects.TIDE_EROSION.value(), "Tide Erosion");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.wear", "Wear Value: %s");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.unidentified", "§7[Unidentified Wear]");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.1", "Appearance: §aBlade of Eternal Waves");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.2", "Appearance: §eBay Aftermath");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.3", "Appearance: §6Reef Undercurrent");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.4", "Appearance: §cLow Tide Fissure");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.status.5", "Appearance: §4Dead Heart of Dried Tide");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.1", "§7The blade exhibits a perfect gradient from deep-sea cobalt to shallow-sea teal, with shifting pearlescent luster. The Heart of the Sea in the guard is full and clear, liquid energy waxing and waning smoothly. No scratches on the blade.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.2", "§7Tiny signs of wear appear at the blade edge. The gradient color softens and pearlescence decreases. The Heart of the Sea is still full, but tiny dark ripple marks appear occasionally.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.3", "§7Visible scratches appear on the blade, and the edge is slightly rolled. The overall tone shifts to gray-blue, eroded by undercurrents. Flocculent turbidity appears inside the Heart of the Sea, disrupting energy flow.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.4", "§7Fissures expand on the blade, with chipped edges. The gradient coat peels off significantly, exposing dark metal underneath. The Heart of the Sea surface cracks, and energy leaks as faint sparks.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.desc.5", "§7Severe structural fractures across the blade, held together only by residual magic. The Heart of the Sea is completely dim, almost static, leaving only a tiny spark of light.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.1", "§8Condenses the tide power under full moon and the heart of the abyss.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.2", "§8Every swing flows like the tide, following an unspeakable rhythm.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.3", "§8Attack in perfect beat, and the surging waves will breach enemy lines.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.lore.4", "§8But if the rhythm breaks, the out-of-control currents will backlash and ruin the blade.");
			translationBuilder.add("item.tuanzis_mod.tide_cleaver.shift_hint", "§7[Hold Shift for details & lore]");
			translationBuilder.add("jei.tuanzis_mod.tide_cleaver.description", tideCleaverJeiDescEn);

			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "Flight");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "Undying");
			translationBuilder.add(ModStatusEffects.TEARING.value(), "Tearing");
			translationBuilder.add("death.attack.tuanzis_mod.tearing", "%s bled to death from torn wounds");
			translationBuilder.add(ModItems.SCARECROW, "Scarecrow");
			translationBuilder.add(ModItems.DECOY_TOTEM, "Decoy Totem");
			translationBuilder.add("entity.tuanzis_mod.decoy", "Decoy Phantom");

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
			translationBuilder.add("jei.tuanzis_mod.berserk_charm.description", "[Berserk Charm]\nA special charm that unleashes potential power under extreme conditions.\n\n§eTrigger Conditions:§r\n1. Must be equipped in the §boff-hand§r.\n2. Player health falls below §c30%§r.\n\n§eActivation Effects:§r\n1. Consumes §e1§r charm.\n2. Grants §6Strength II§r and §bSpeed II§r for 15 seconds.\n3. Enters a §e15-second cooldown§r to prevent multiple charms from being consumed rapidly if health remains low.");
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "[Enchantment Compatibility]\n§bInfinity§r and §bMending§r are now compatible and can be applied to the same bow simultaneously.");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "[Warden]\nAn extremely dangerous creature that typically lurks within the Ancient Cities of the Deep Dark.\n\n§eReward Boost:§r\n1. §bXP Increase§r: Now drops §a275§r experience points when killed by a player or a tamed wolf (was 5).\n2. §bRare Drop§r: If a player deals more than §c50%§r of its max health as damage, it has a §a10%§r chance to drop a §6Soulbound§r enchanted book (affected by Looting, +2.5% per level).\n\n§7Tip: Even with the increased reward, it remains highly dangerous. Prepare thoroughly before engaging.§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "[Experience Enchantment]\nIncreases the experience points gained from killing mobs.\n\n§eEffects:§r\n1. §bXP Boost§r: Each level increases experience gain by §a25%§r (rounded up).\n2. §bCondition§r: Only effective when killing mobs with the enchanted weapon in the main hand.\n3. §bScope§r: Effective against all mobs except players.\n\n§7Note: Max level is 4. Level 30 enchantment table can yield up to level 3. Level 4 requires anvil merging or special loot.§r");
			translationBuilder.add("jei.tuanzis_mod.warden_heart.description", "[Warden Heart]\nA core material obtained from defeating the Warden, containing its resilient life force and sonic energy.\n\n§eAcquisition:§r\n1. §bRare Drop:§r Defeating a Warden has a §a25%§r chance to drop it (affected by Looting, +10% probability per level).\n\n§eUsage:§r\n1. §bCrafting Ingredient:§r Combine with 3 §bEcho Shards§r and 1 §bStick§r to craft the powerful defensive weapon — §6Echo Breaker§r.\n2. §bEquipment Repair:§r Can be used to repair the §6Echo Breaker§r in an anvil.");
			translationBuilder.add("jei.tuanzis_mod.echo_breaker.description", "[Echo Breaker]\nAn ultimate defensive weapon forged using a Warden Heart and Echo Shards. It holds the mysterious power to resist and even deflect sonic damage.\n\n§ePassive Effects:§r\n1. §bSonic Absorption:§r Effective only when held in the main hand. Automatically §areduces Sonic Boom damage by 30%§r.\n\n§eActive Effects (Right-click):§r\n1. §bRestrictions:§r Right-click block can only be used when the §coffhand is empty§r.\n2. §bPerfect Block:§r Right-click to block for §e0.5 seconds§r. During blocking, grants §a100% immunity to Sonic Boom damage§r.\n3. §bSonic Deflection:§r Blocking a Sonic Boom perfectly deflects the sound wave back to its source, dealing §c15 true damage§r and applying §9strong knockback§r.\n4. §bDurability Cost:§r Each successful block consumes an extra §c3 durability points§r.\n5. §bCooldown Mechanics:§r Triggers a §c5.0-second§r cooldown on §cfailure§r (no damage blocked); triggers an §a8.0-second§r cooldown on §asuccess§r (perfectly deflected).\n\n§eRepair & Maintenance:§r\nCan be repaired in an anvil using a §bWarden Heart§r.");
			translationBuilder.add("enchantment.tuanzis_mod.chain_mining", "Chain Mining");
			translationBuilder.add("enchantment.tuanzis_mod.blood_rage", "Blood Rage");
			translationBuilder.add("enchantment.tuanzis_mod.berserker", "Berserker");
			translationBuilder.add("enchantment.tuanzis_mod.buzzing_rhythm", "Buzzing Rhythm");
			translationBuilder.add("effect.tuanzis_mod.blood_rage", "Blood Rage");
			translationBuilder.add("effect.tuanzis_mod.buzzing_rhythm", "Rhythm");
			translationBuilder.add("death.attack.tuanzis_mod.blood_rage_feedback", "%s died of Blood Rage backlash");
			translationBuilder.add("key.tuanzis_mod.chain_mining", "Chain Mining Activation");
			translationBuilder.add("key.category.tuanzis_mod", "Tuanzi's Mod");
			translationBuilder.add("key.category.minecraft.tuanzis_mod", "Tuanzi's Mod");
			translationBuilder.add("key.category.tuanzis_mod.tuanzis_mod", "Tuanzi's Mod");
			
			String chainDescEn = "[Chain Mining Enchantment]\n" +
				"Hold the activation key while mining a block to instantly mine surrounding connected blocks of the same type.\n\n" +
				"§eFeatures:§r\n" +
				"1. §bVein Mining§r: Break a block to automatically break connected identical blocks and drop items normally.\n" +
				"2. §bMining Limits§r: Level I mines up to 16 blocks, Level II up to 32, Level III up to 48, Level IV up to 64.\n\n" +
				"§eConstraints & Penalties:§r\n" +
				"1. §bDurability Cost§r: Each mined block consumes tool durability normally. If the tool breaks during chain mining, the chain will immediately and safely stop.\n" +
				"2. §bHunger Cost§r: Each extra block mined adds §c0.25§r exhaustion. The exhaustion is accumulated and applied instantly to deduct your saturation and food level.\n\n" +
				"§7Acquisition: Currently unavailable in survival mode; obtain via Creative Mode only.§r";
			translationBuilder.add("jei.tuanzis_mod.chain_mining.description", chainDescEn);

			String bloodRageDescEn = "[Blood Rage Enchantment]\n" +
				"Turn battle frenzy into sheer destructive power. A truly double-edged sword.\n\n" +
				"§eFeatures:§r\n" +
				"1. §bKill Outbreak§r: Killing any mob grants \"Blood Rage\" buff for §e7 seconds§r.\n" +
				"2. §bDamage Surge§r: During Blood Rage, player's attack damage is boosted by §a40%§r (multiplicative).\n\n" +
				"§eCost & Backlash:§r\n" +
				"1. §bDefense Collapse§r: During Blood Rage, all incoming damage is increased by §c30%§r (multiplicative).\n" +
				"2. §bFateful Backlash§r: If no mobs are killed within 7 seconds to refresh the timer, natural expiration inflicts §c2 damage (1 heart)§r that bypasses armor and Protection enchantments.\n" +
				"3. §bExclusion§r: Mutually exclusive with §bFire Aspect§r, but perfectly compatible with §bSharpness§r, §bSmite§r, and §bBane of Arthropods§r.\n\n" +
				"§7Acquisition: Max level I. Obtainable via Enchanting Table, random loot, or Villager trades.§r";
			String berserkerDescEn = "[Berserker Enchantment]\n" +
				"Convert your near-death crisis into a devastating counter-attack.\n\n" +
				"§eFeatures:§r\n" +
				"1. §bDesperate Rage§r: When wearing the enchanted chestplate, melee attack damage is boosted based on the proportion of lost health. For every 10% max health lost, melee damage increases by §a5% (Level I)§r or §a8% (Level II)§r. The boost reaches its cap when health falls below §c5%§r.\n" +
				"2. §bReal-time Adjustment§r: When health is restored by healing, the damage bonus decreases in real-time.\n\n" +
				"§eCost & Penalties:§r\n" +
				"1. §bArmor Collapse§r: As a penalty, the chestplate's base armor value is reduced by §c35% (Level I)§r or §c30% (Level II)§r (meaning the chestplate still retains 65% or 70% of its base armor value. For example, a Diamond Chestplate with 8 base armor will only lose 2 points under Level II, still providing 6 armor points, instead of being reduced to only 30%).\n" +
				"2. §bExclusion§r: Mutually exclusive with all protection enchantments including §bProtection§r, §bFire Protection§r, §bBlast Protection§r, §bProjectile Protection§r, and §bFeather Falling§r.\n\n" +
				"§7Acquisition: Max level II. Obtainable via Enchanting Table (rare), random loot, or Villager trades.§r";
			translationBuilder.add("jei.tuanzis_mod.blood_rage.description", bloodRageDescEn);
			translationBuilder.add("jei.tuanzis_mod.berserker.description", berserkerDescEn);

			translationBuilder.add("hud.tuanzis_mod.chain_mining.active", "Chain Mining §aActive");
			translationBuilder.add("jei.tuanzis_mod.scarecrow.description", "[Scarecrow]\nA decorative item, also serving as the core component to craft the advanced defensive item — §bDecoy Totem§r.\n\n§eAcquisition:§r\n1. §bShapeless Crafting§r: Crafted using 1 Hay Block, 1 Stick, and 1 Carved Pumpkin.");
			translationBuilder.add("jei.tuanzis_mod.decoy_totem.description", "[Decoy Totem]\nAn extremely powerful strategic defense tool. Perfect for drawing enemy attention, creating safe windows for combat or escape.\n\n§eEffects & Mechanics:§r\n1. §bPhantom Spawn§r: Generates a player phantom lasting §e15 seconds§r, drawing agro from all mobs within §a10 blocks§r.\n2. §bPerfect Disguise§r: The phantom looks exactly like the player (skin, armor, and held items).\n3. §bData Inheritance§r: Inherits player's health and armor value at the moment of spawn.\n4. §bLifelike Behavior§r: Randomly walks, runs, and jumps. Upon taking damage, it instantly panics and sprints away with big jumps, fleeing the damage source!\n5. §bShatter Revenge§r: Drops nothing on death but shatters with a clay pot sound. It enrages mobs within §a10 blocks§r, granting them §cSpeed II§r for §e10 seconds§r.\n\n§eAcquisition:§r\n1. §bShaped Crafting§r: Top [Empty, Sculk Vein, Empty], Middle [Phantom Membrane, Scarecrow, Phantom Membrane], Bottom [Glowstone, Redstone, Glowstone].\n\n§7Note: Spawning triggers a §c15-second§r item cooldown.");

			// Trial Dummy English Translations
			translationBuilder.add(me.tuanzi.init.ModItems.TRIAL_DUMMY, "Trial Dummy");
			translationBuilder.add("entity.tuanzis_mod.trial_dummy", "Trial Dummy");
			translationBuilder.add("jei.tuanzis_mod.trial_dummy.description", "[Trial Dummy]\nA highly specialized helper tool used for testing damage output and DPS.\n\n§eRules:§r\n1. §bPlacement§r: Right-click to place on a solid block. Max 1 per player within a 10-block radius (placing a 2nd instantly recovers the 1st without durability loss).\n2. §bRecovery§r: Shift + Right-click with empty hand to recover. Consumes 1 durability. Drop rate on zero durability: 0-2 sticks and 1 hay block. All worn armors safely drop on recovery!\n3. §bInvulnerability & Armor Inheritance§r: Immune to all environment damages (except Creative Player instant break). Right-click with any armor/weapon to swap and dress it. The dummy inherits all armor values, toughness, protection enchantments, and active effects to simulate a 100% real combat defense!\n4. §bWeapon & Projectile Protection§r: Attacking doesn't consume weapon durability or arrows.\n5. §bDPS Testing§r: Floating damage indicators, action bar real-time stats (Total Damage | DPS). Auto-archive on 4s idle. Right-click with empty hand to print ultra-detailed statistics in chat!");
			translationBuilder.add("hud.tuanzis_mod.trial_dummy.actionbar", "⚔ Total Damage: %s | DPS: %s");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.reset", "§c[Trial Dummy] Session ended. DPS stats archived and reset.");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.no_tests", "§6[Trial Dummy] No testing data found. Strike the dummy to start.");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.title_active", "============ §aTrial Dummy Stats (Active)§r ============");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.title_archive", "============ §6Trial Dummy Stats (Archived)§r ============");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.total_damage", "▶ Total Outgoing Damage: §e%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.max_damage", "▶ Maximum Single Strike: §e%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.hits", "▶ Valid Hits Count: §e%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.dps", "▶ Average DPS: §c%s§r");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.duration", "▶ Session Duration: §a%s§r seconds");
			translationBuilder.add("message.tuanzis_mod.trial_dummy.stats.footer", "================================================");

			// Villager Cage English Translations
			translationBuilder.add(ModItems.VILLAGER_CAGE, "Villager Cage");
			translationBuilder.add("item.tuanzis_mod.villager_cage.tooltip.empty", "§7Empty Cage");
			translationBuilder.add("item.tuanzis_mod.villager_cage.tooltip.filled", "§bContains: %s - %s (Level %d)");
			translationBuilder.add("message.tuanzis_mod.villager_cage.need_solid", "§cRequires a solid block surface to release the villager!");
			translationBuilder.add("message.tuanzis_mod.villager_cage.golem_aggro", "§cNearby protecting Iron Golems are now hostile!");
			translationBuilder.add("jei.tuanzis_mod.villager_cage.description",
				"[Villager Cage]\nA mystical imprisonment device that seals a villager inside for cross-dimensional transport.\n\n" +
				"§eCatching a Villager:§r\n" +
				"1. Hold the cage in your §bmain hand§r and §bright-click a villager§r to capture it. Plays a soul sand sound with Ender particles.\n" +
				"2. All villager data (profession/level/trades/name) is fully saved. The cage is §cdestroyed after use§r (one-time item).\n" +
				"3. §cWarning§r: If an Iron Golem is actively protecting the villager, capturing it will make all Golems within §c16 blocks§r hostile!\n\n" +
				"§eReleasing a Villager:§r\n" +
				"1. §bSneak + Right-click§r a solid block surface to release. The villager spawns above the block with all data intact.\n" +
				"2. The cage is consumed on release. If the target location has no solid ground or no space, release is denied with a message.\n" +
				"3. An empty cage does nothing when sneaking and right-clicking.\n\n" +
				"§eSpecial Notes:§r\n" +
				"1. §bCross-dimensional transport§r: Villager data is stored in the item; release in any dimension.\n" +
				"2. Trade lock state is preserved, but bed and job site bindings are cleared (must re-establish).\n\n" +
				"§eCrafting:§r\nIron Ingot, Emerald Block, Iron Ingot\nIron Ingot, Soul Sand, Iron Ingot\nIron Ingot, Iron Ingot, Iron Ingot");

			// Soul Merchant Station English Translations
			translationBuilder.add(me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION, "Soul Merchant Station");
			translationBuilder.add(me.tuanzi.init.ModBlocks.SOUL_MERCHANT_STATION.asItem(), "Soul Merchant Station");
			translationBuilder.add("item.tuanzis_mod.soul_merchant_station.tooltip", "Right-click to place. Import a villager for AI-free high-TPS trading.");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.imported", "§aVillager successfully imported!");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.no_villager", "§cNo villager currently residing in this station!");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.already_has_villager", "§cThis station already has a resident villager!");
			translationBuilder.add("message.tuanzis_mod.soul_merchant_station.extracted", "§aVillager successfully repackaged into the cage!");
			translationBuilder.add("jei.tuanzis_mod.soul_merchant_station.description",
				"[Soul Merchant Station]\nA revolutionary AI-free, non-entity villager trading device. Highly optimizes server FPS/TPS while fully retains vanilla trading.\n\n" +
				"§eFeatures:§r\n" +
				"1. §bVillager Import§r: Right-click with a filled §bVillager Cage§r to import the villager. Consumes the cage, displaying a 3D rotating profession icon inside the cage.\n" +
				"2. §bVillager Export§r: Sneak + Right-click with an §cempty Villager Cage§r to securely package the villager back, keeping the station intact.\n" +
				"3. §bAI-Free Trading§r: Right-clicking opens the standard trading GUI. Experience, leveling, and trade locks are fully functional. The villager is 100% safe from harm.\n" +
				"4. §bAuto-Restock§r: At dawn or waking up, if a matching, §bunoccupied job site block§r is adjacent (6 directions) to the station, locked trades will automatically restock.\n" +
				"5. §bNaming Support§r: Use a §bName Tag§r on the station to rename it; the name appears at the top of the trading GUI.\n" +
				"6. §bBlast Immunity§r: The block is completely immune to blast damages, offering permanent safety to the villager.");

			// Travelers Notebook English Translations
			translationBuilder.add(ModItems.TRAVELERS_NOTEBOOK, "Traveler's Notebook");
			translationBuilder.add(ModItems.TELEPORTATION_PAPER, "Teleportation Paper");
			translationBuilder.add(ModItems.SIGNPOST_RUNE, "Signpost Rune");
			
			translationBuilder.add("item.tuanzis_mod.travelers_notebook.tooltip.energy", "⏣ Energy: %d/%d");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.unbound", "§8[Unbound]§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.bind_hint", "§7Shift+Right-click to bind current location§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.bound", "§a[Bound]§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.dimension", "§7Dimension: %s§r");
			translationBuilder.add("item.tuanzis_mod.signpost_rune.tooltip.coords", "§7Coords: X: %s, Y: %s, Z: %s§r");
			
			translationBuilder.add("message.tuanzis_mod.signpost_rune.bound", "§aLocation successfully bound!§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.insufficient_energy", "§cTraveler's Notebook: Insufficient Energy§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.interrupted", "§cTeleportation Interrupted§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.starting", "§dChannelling teleportation... Stand still for 3 seconds§r");
			translationBuilder.add("message.tuanzis_mod.travelers_notebook.cooldown", "§cCooldown... %.1f seconds remaining§r");
			
			translationBuilder.add("container.tuanzis_mod.travelers_notebook", "Traveler's Notebook Storage");
			
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.teleport", "Teleport");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.energy_status", "⏣ Energy: %d/64");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.empty_slots", "Please put bound Signpost Runes inside the storage to mark destinations.");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.select_hint", "Select a bound Signpost Rune on the left to begin teleportation.");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.dim_label", "Dimension: %s");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.energy_cost", "Energy Cost: %d");
			translationBuilder.add("gui.tuanzis_mod.travelers_notebook.no_energy_warning", "Out of Energy");

			// Dimension translations
			translationBuilder.add("dimension.minecraft.overworld", "Overworld");
			translationBuilder.add("dimension.minecraft.the_nether", "The Nether");
			translationBuilder.add("dimension.minecraft.the_end", "The End");

			translationBuilder.add("jei.tuanzis_mod.travelers_notebook.description", "[Traveler's Notebook]\nA notebook woven from void energy that allows players to teleport across coordinates.\n\n§eUsage:§r\n1. §bRight-click§r: Open book-style teleport interface. Select a bound rune and press teleport.\n2. §bSneak + Right-click§r: Open internal storage (3x9, 27 slots).\n\n§eCharging & Storage:§r\n1. The very first slot (Slot 0) is reserved for §eFuel§r. Place §bTeleportation Paper§r inside; it automatically consumes them on GUI close to charge the notebook. 1 paper = 1 energy point, cap 64.\n2. The remaining 26 slots only accept bound §bSignpost Runes§r.\n3. The notebook cannot be enchanted or repaired.\n\n§eTeleport Rules:§r\n1. §bIntra-dimension§r teleport costs §a1§r energy. §bCross-dimension§r teleport costs §a2§r energy.\n2. Teleportation requires §e3s channelling§r. Taking damage or moving will cancel the teleport.\n3. Intradimension cooldown is 3s. Crossdimension cooldown is 5s.\n\n§cWarning:§r\nIf the notebook entity is destroyed in the world (e.g. lava, fire, explosion), all stored contents will drop!");
			translationBuilder.add("jei.tuanzis_mod.teleportation_paper.description", "[Teleportation Paper]\nA parchment infused with faint Ender energy.\n\n§eUsage:§r\nUsed solely as fuel. Place it in the fuel slot (Slot 0) of the §bTraveler's Notebook§r storage; it will be consumed to charge the notebook when the GUI is closed.");
			translationBuilder.add("jei.tuanzis_mod.signpost_rune.description", "[Signpost Rune]\nA runic slate used to lock and store dimensional coordinate data.\n\n§eUsage:§r\n1. §bBinding Locations§r: Right-click while holding it anywhere in the world to bind the current location and dimension. Plays a ringing chime sound.\n2. §bCustom Naming§r: You can rename bound runes in an §bAnvil§r. The custom name will be displayed in the §bTraveler's Notebook§r destination list.\n\n§7Note: Runes are non-stackable. Only bound runes can be placed in the notebook's destination slots.");

			// Potion Translations
			translationBuilder.add("item.minecraft.potion.effect.adrenaline", "Potion of Adrenaline");
			translationBuilder.add("item.minecraft.splash_potion.effect.adrenaline", "Splash Potion of Adrenaline");
			translationBuilder.add("item.minecraft.lingering_potion.effect.adrenaline", "Lingering Potion of Adrenaline");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.adrenaline", "Arrow of Adrenaline");
			
			translationBuilder.add("item.minecraft.potion.effect.adrenaline_ii", "Potion of Adrenaline II");
			translationBuilder.add("item.minecraft.splash_potion.effect.adrenaline_ii", "Splash Potion of Adrenaline II");
			translationBuilder.add("item.minecraft.lingering_potion.effect.adrenaline_ii", "Lingering Potion of Adrenaline II");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.adrenaline_ii", "Arrow of Adrenaline II");
			
			translationBuilder.add("item.minecraft.potion.effect.long_adrenaline", "Potion of Adrenaline (Extended)");
			translationBuilder.add("item.minecraft.splash_potion.effect.long_adrenaline", "Splash Potion of Adrenaline (Extended)");
			translationBuilder.add("item.minecraft.lingering_potion.effect.long_adrenaline", "Lingering Potion of Adrenaline (Extended)");
			translationBuilder.add("item.minecraft.tipped_arrow.effect.long_adrenaline", "Arrow of Adrenaline (Extended)");

			translationBuilder.add("jei.tuanzis_mod.adrenaline_potion.description", "[Potion of Adrenaline]\nA powerful potion that instantly unleashes potential, boosting attack speed and movement speed at the cost of a severe overdraw backlash.\n\n§eDrink Effects:§r\n1. §bExtreme Boost:§r Attack Speed is increased by §a15% * Level§r, and Movement Speed is increased by §a7.5% * Level§r.\n2. §bDurability:§r Normal version lasts 45 seconds, Strong (II) version lasts 30 seconds, and Extended version lasts 1 min 30 seconds.\n\n§eOverdraw & Penalties:§r\n1. §bMilk Immunity:§r The overdraw effect §ecannot be cleared by milk§r. If it remains when the Adrenaline buff ends, the overdraw side effects are triggered.\n2. §bSide Effects:§r Grants §c30 seconds of Weakness I + Mining Fatigue I§r per overdraw level.\n3. §bStacking Rule:§r Drinking while overdrawn §6stacks both the overdraw level and its duration§r. Each additional drink raises the overdraw level by 1 (max level IV), and the final side effect duration stacks linearly (30s for Level I, 60s for II, 90s for III, 120s for IV).");

			String wolfCommandDescEn = "[Wolf Command]\n" +
				"A charm that bestows infinite power and vitality to tamed wolves.\n\n" +
				"§eEquipped Effects (Must be in off-hand):§r\n" +
				"1. §bHealth Boost§r: All your tamed wolves gain an extra §a+4§r max health.\n" +
				"2. §bTearing Claws§r: Wolves' attacks have a §a15%§r chance to inflict the §cTearing§r status effect for 8 seconds. If the target is already torn, refreshes the duration and increases the effect level by 1 (up to level 4).\n" +
				"3. §bUndying Battle-will§r: Tamed wolves will not die upon taking fatal damage. Instead, they enter a §e5-second invulnerable rage mode§r (attack frequency doubled, movement and attack damage boosted), and then collapse while consuming 1 durability point of the charm.\n\n" +
				"§eDowned State:§r\n" +
				"The downed wolf cannot move, cannot attack, is immune to all environmental/normal damage, and cannot be leashed. Feed it §eRotten Flesh 10 times§r to fully revive it with full health!\n\n" +
				"§eStatus Effect - Tearing:§r\n" +
				"Ticks every 0.5s (10 ticks). If the entity moves during this tick, it takes §c0.5 * Level§r magic damage; otherwise, nothing happens.\n\n" +
				"§cNote: The charm cannot be repaired. Maximum durability is 3, maximum stack size is 1.§r";
			translationBuilder.add("jei.tuanzis_mod.wolf_command.description", wolfCommandDescEn);

			String seekingArrowDescEn = "[Seeking Arrow Enchantment]\n" +
				"A treasure-grade enchantment exclusively for Crossbows. Imbues fired arrows with unique projectile self-correction capabilities.\n\n" +
				"§eEffects:§r\n" +
				"1. §bTarget Highlighting§r: While holding a loaded Seeking Arrow crossbow, the targeted living entity glows brightly (glowing outline is visible only to the shooter in client-side).\n" +
				"2. §bTrajectory Correction§r: Once fired, after 0.5 seconds (10 ticks), the arrow performs a one-time trajectory self-correction (up to 30 degrees) towards either the highlighted entity or the target coordinates. Prioritizes entities!\n\n" +
				"§eConstraints \u0026 Restrictions:§r\n" +
				"1. §bOne-time Correction§r: Each arrow corrects its trajectory exactly once; it cannot perform large sweeping turns in mid-air.\n" +
				"2. §bMovement Prediction§r: Features movement prediction logic based on the target's current velocity (extrapolating its position 0.5 seconds later), but does not track acceleration or change in direction (if the target changes direction after correction, the arrow will still miss).\n" +
				"3. §bMutual Exclusion§r: Incompatible with §bMultishot§r.\n\n" +
				"§7Acquisition: A \"Treasure Enchantment\". Cannot be obtained via the Enchanting Table or Librarian trades. Found only in loot chests in dungeons or fortresses.";
			translationBuilder.add("jei.tuanzis_mod.seeking_arrow.description", seekingArrowDescEn);
			translationBuilder.add("enchantment.tuanzis_mod.seeking_arrow", "Seeking Arrow");

			// Shuriken and Nether Stew English translations
			translationBuilder.add(ModItems.SHURIKEN, "Shuriken");
			translationBuilder.add(ModStatusEffects.SHURIKEN_STUCK.value(), "Shuriken Stuck");
			
			String shurikenDescEn = "[Shuriken]\n" +
				"A lightweight and extremely fast ranged throwing weapon.\n\n" +
				"§eSpecial Throwing:§r\n" +
				"1. §bRapid Normal Throw§r: Deals 2 damage. Flying speed is 1.5 times that of an arrow without any gravity drop. Single right-click has a 0.2s throwing cooldown.\n" +
				"2. §bSneak Fan-Out Burst§r: Hold Sneak (Shift) + Right-click to consume 3 shurikens and launch 3 shurikens in a forward fan arc. Each shuriken calculates damage independently. Triggers a 0.6s overall cooldown after the burst.\n\n" +
				"§eShuriken Stuck Status Effect:§r\n" +
				"1. §bMovement Penalty§r: Hits have a 20% chance to apply the \"Shuriken Stuck\" debuff. Each stuck shuriken reduces movement speed by -10%, stacking up to 3 times (max -30% speed).\n" +
				"2. §bDuration Refresh§r: Hitting the target again refreshes the duration of all currently stuck shurikens back to 8 seconds.\n" +
				"3. §bItem Drop§r: If not refreshed within 8s, stuck shurikens automatically detach and drop at the target's feet as collectible items.\n" +
				"4. §bPVP Counterplay§r: When a player is stuck, taking damage has a 15% chance, and swinging a weapon has a 10% chance to shake off 1 shuriken prematurely.";
			translationBuilder.add("jei.tuanzis_mod.shuriken.description", shurikenDescEn);

			translationBuilder.add(ModItems.NETHER_STEW, "Nether Stew");
			translationBuilder.add(ModStatusEffects.HELL_FIRE.value(), "Hell Fire");
			translationBuilder.add(ModStatusEffects.BLAZING_HUNGER.value(), "Blazing Hunger");
			translationBuilder.add(ModStatusEffects.HYDROPHOBIA.value(), "Hydrophobia");
			translationBuilder.add("death.attack.tuanzis_mod.hydrophobia", "%s was vaporized by water");

			String netherStewDescEn = "[Nether Stew]\n" +
				"A demonic, spicy stew distilled from Hellfire crimson and warped funguses. Stackable up to 64, returns an empty bowl after eating. Consuming grants the player three double-edged status effects lasting 1 min 30 seconds:\n\n" +
				"§ePositive Effect - Hell Fire:§r\n" +
				"1. Grants complete immunity to fire and lava damage.\n" +
				"2. Melee attacks have a 60% chance to ignite the target for 2s.\n" +
				"3. Dealing melee damage to burning targets gains a flat +1 damage bonus.\n\n" +
				"§cNegative Effect 1 - Blazing Hunger:§r\n" +
				"Doubles the depletion speed of the player's hunger and saturation.\n\n" +
				"§cNegative Effect 2 - Hydrophobia:§r\n" +
				"1. Makes the player extremely afraid of water. Takes 2 damage per 1s while in water or rain.\n" +
				"2. This damage is true damage, ignoring all armor, and cannot be mitigated by any resistance or absorption effects!";
			translationBuilder.add("jei.tuanzis_mod.nether_stew.description", netherStewDescEn);
			
			// Execute and Chain Pain English translations
			translationBuilder.add("enchantment.tuanzis_mod.execute", "Execute");
			translationBuilder.add("death.attack.tuanzis_mod.execute_backlash", "%s died from the bloodlust backlash");
			String executeDescEn = "[Execute Enchantment]\n" +
				"A powerful combat enchantment applicable to Swords and Axes. Unleash chaotic bloodlust to execute low-health targets!\n\n" +
				"§eEffects:§r\n" +
				"Triggers when the target's current health percentage falls below a specified threshold, providing a massive multiplier to your melee damage!\n" +
				"1. §aLevel I§r: Triggers below §e15%§r health, deals §a+25%§r melee damage.\n" +
				"2. §aLevel II§r: Triggers below §e25%§r health, deals §a+50%§r melee damage.\n" +
				"3. §aLevel III§r: Triggers below §e35%§r health, deals §a+75%§r melee damage.\n\n" +
				"§cBacklash Penalty - Bloodlust Backlash:§r\n" +
				"1. Each time the execute damage boost is triggered, you will instantly take:\n" +
				"   §cLevel * 1 point of true damage (Level I: 1, Level II: 2, Level III: 3)§r.\n" +
				"2. This backlash is true damage, ignoring all armor, and cannot be mitigated by any resistance or absorption effects! Highly lethal!\n\n" +
				"§eExclusion \u0026 Limits:§r\n" +
				"Mutually exclusive with §bSharpness§r, §bSmite§r, and §bBane of Arthropods§r.";
			translationBuilder.add("jei.tuanzis_mod.execute.description", executeDescEn);

			translationBuilder.add("enchantment.tuanzis_mod.chain_pain", "Chain Pain");
			translationBuilder.add("death.attack.tuanzis_mod.chain_pain", "%s was crushed by the overflow pain shockwave");
			String chainPainDescEn = "[Chain Pain Enchantment]\n" +
				"A rare, treasure-grade combat enchantment exclusively for Axes. Directs the excess pain from a kill into a devastating shockwave chaining to nearby enemies!\n\n" +
				"§eEffects:§r\n" +
				"Killing any target with this Axe calculates the overflow damage (final blow damage minus target's health prior to the blow), dealing §c50%§r of this excess damage to all other enemies within §a5 blocks§r.\n" +
				"1. Chaining automatically targets up to §e5 entities§r, sorted and damaged from nearest to furthest.\n" +
				"2. Ideal for sweeping groups of monsters instantly after executing a single target.\n\n" +
				"§eConstraints \u0026 Restrictions:§r\n" +
				"1. §bNo Recursion§r: Chained splash damage will never trigger the chain pain effect recursively.\n" +
				"2. §bTreasure Hunt§r: Cannot be obtained via the Enchanting Table or Librarian trades. Found only in loot chests in dungeons or fortresses.\n" +
				"3. §bExclusion§r: Mutually exclusive with §bSilk Touch§r.";
			translationBuilder.add("jei.tuanzis_mod.chain_pain.description", chainPainDescEn);

			String buzzingRhythmDescEn = "[Buzzing Rhythm Enchantment]\n" +
				"A rare combat enchantment applicable to Swords. Drastically boosts attack damage through rhythmic consecutive hits!\n\n" +
				"§eEffects:§r\n" +
				"1. §bRhythm Boost§r: When attacking a target with \"Bee Poison\" or \"Bee Poison Cooldown\" consecutively with this enchanted weapon, gains one stack of \"Rhythm\" per hit, lasting for 3 seconds, up to 1 + level (max 5) stacks. Each stack grants a §a+3%§r bonus to attack damage (max +15% attack damage).\n" +
				"2. §bExpiration§r: If no hit is scored on the same target within 3 seconds, the stacks instantly clear.\n" +
				"3. §bReach Distance§r: Each level increases your entity interaction reach distance by §b0.0625 blocks§r (max 0.25 blocks).\n\n" +
				"§eExclusion \u0026 Limits:§r\n" +
				"Mutually exclusive with §bSweeping Edge§r, §bFire Aspect§r, and §bKnockback§r.";
			translationBuilder.add("jei.tuanzis_mod.buzzing_rhythm.description", buzzingRhythmDescEn);

			translationBuilder.add("enchantment.tuanzis_mod.abyssal_rhythm", "Abyssal Rhythm");
			String abyssalRhythmDescEn = "[Abyssal Rhythm Enchantment]\n" +
				"A general combat enchantment applicable to Swords. Resonates with tide energy to deal extra magic damage!\n\n" +
				"§eEffects:§r\n" +
				"1. §bAbyssal Strike§r: Each melee attack hit on a target with an enchanted sword triggers Abyssal Rhythm, dealing extra magic damage to the primary target. No cooldown.\n" +
				"2. §bDamage Calculations§r:\n" +
				"   Extra Damage = Base Damage + Tide Resonance Bonus\n" +
				"   §aBase Damage§r: Based on Level (0.1 for I, 0.2 for II, 0.25 for III, 0.3 for IV).\n" +
				"   §aTide Resonance Bonus§r: Active only when holding Tide Charge layers (from Tide Cleaver). Bonus = 0.05 × Level × current layers.\n" +
				"   At 5 layers, the resonance bonus for Level IV provides an extra 1.0 damage!\n\n" +
				"§eAcquisition:§r\n" +
				"Max Level IV. General sword enchantment, compatible with other damage enchantments.";
			translationBuilder.add("jei.tuanzis_mod.abyssal_rhythm.description", abyssalRhythmDescEn);

			// Tidal Weave Boots translations
			translationBuilder.add(ModItems.TIDAL_WEAVE_BOOTS, "Tidal Weave Boots");
			translationBuilder.add("item.tuanzis_mod.tidal_weave_boots.tooltip", "Woven from prismarine crystals and phantom membranes, flowing with the pulsing aura of the heart of the sea.");
			translationBuilder.add("jei.tuanzis_mod.tidal_weave_boots.description", "[Tidal Weave Boots]\nA pair of lightweight boots woven from prismarine crystals and phantom membranes. The surface flows with a pulsing aura of the heart of the sea.\n\n§eEffects & Mechanics:\n1. §bWater Walking: Walk, sprint, and jump freely on the surface of water as if it were a solid block when equipped in the feet slot.\n2. §bDiving Toggle: Press the sneak key (Shift) while walking on water to submerge into swimming. Release and swim back to the surface to automatically resume water walking.\n3. §bFall Damage Mitigation: Immune to all fall damage when falling onto the surface of water.");

			// Undying Potion translations
			translationBuilder.add("jei.tuanzis_mod.undying_potion.update_103", "[Undying Potion]\nPotions granting the \"Undying\" status effect, available in normal and extended variants.\n\n§e1.0.3 Update Notes:§r\nThe \"Undying\" status effect now only applies to §bPlayers§r and §bTamed Mobs§r (e.g., tamed wolves, cats). It no longer works on wild or hostile mobs, preventing accidental protection of enemies.");

			// Item Tag translations
			translationBuilder.add("tag.item.tuanzis_mod.repairs_tidal_weave_boots", "Tidal Weave Boots Repair Materials");
			translationBuilder.add(ModItems.WORLD_SCULPTORS_PEN, "World Sculptor's Pen");
			translationBuilder.add("tooltip.tuanzis_mod.world_sculptors_pen.mode", "Current Fill Mode: %s");
			translationBuilder.add("tuanzis_mod.mode.full_replace", "Full Replace");
			translationBuilder.add("tuanzis_mod.mode.air_only", "Air Only");
			translationBuilder.add("tuanzis_mod.mode.semi_replace", "Semi Replace");
			translationBuilder.add("tuanzis_mod.mode.replace_air_only", "Replace Air Only");
			translationBuilder.add("message.tuanzis_mod.world_sculptors_pen.mode_toggled", "§6World Sculptor's Pen toggled to: %s Mode");
			translationBuilder.add("jei.tuanzis_mod.world_sculptors_pen.description", "[World Sculptor's Pen]\nA luxury writing tool made of a netherite nib and an amethyst barrel, wrapped with glowing sculk-like fluorescent ink lines.\n\n§eMechanics:\n1. §bBatch Fill: Right-click a block to record start point. Right-click another block within 64 blocks distance and 2048 blocks volume to instantly fill the region.\n2. §bSelect Block Type: Sneak (Shift) + Right-click a block to select it. The selected block is rendered at the bottom-right of the pen's texture.\n3. §bSmart Consumption: Blocks are deducted from your inventory and shulker boxes inside. Cancelled if materials or durability are insufficient.\n4. §bAnti-Dupe Undo: Sneak (Shift) + Right-click within 60 seconds to undo. Only returns blocks and durability for blocks that are currently unchanged.\n5. §bDurability & Repair: Has 8192 durability. Cannot be enchanted. Repaired by 1000 durability per Void Ink Ingot in an anvil.");
			translationBuilder.add(ModItems.VOID_INK_INGOT, "Void Ink Ingot");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.desc_line1", "A deep purple ink ingot pressed from condensed dragon's breath and echo shards,");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.desc_line2", "with eddying patterns like the End void flowing on its surface, and dark gold glints of netherite scrap on the edges.");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.desc_line3", "Holding it close to the World Sculptor's Pen makes the pen tremble slightly, as if the ingot is calling out to its nib.");
			translationBuilder.add("tooltip.tuanzis_mod.void_ink_ingot.incompatible", "Ink Incompatible");
			translationBuilder.add("jei.tuanzis_mod.void_ink_ingot.description", "[Void Ink Ingot]\n" +
				"A deep purple ink ingot pressed from condensed dragon's breath and echo shards, with eddying patterns like the End void flowing on its surface, and dark gold glints of netherite scrap on the edges.\n\n" +
				"§eInk-Pen Affinity:§r\n" +
				"The Void Ink Ingot only matches the amethyst nib and echo ink sac system of the World Sculptor's Pen. It has no repair effect on other tools, weapons, or armor. Placing other items will display a dark red \"Ink Incompatible\" warning in the anvil output slot and prevent pick-up.");
		}
	}
}
