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
			translationBuilder.add(ModItems.WARDEN_HEART, "坚守者的心脏");
			translationBuilder.add(ModItems.ECHO_BREAKER, "回响破障者");
			translationBuilder.add(ModStatusEffects.FLIGHT.value(), "飞行");
			translationBuilder.add(ModStatusEffects.UNDYING.value(), "不死");
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
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "【附魔兼容性增强】\n现在 §b无限 (Infinity)§r 与 §b经验修补 (Mending)§r 互相兼容，可以同时附魔在同一把弓上。");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "【监守者】\n一种极其危险的生物，通常潜伏在深暗之域的远古城市中。\n\n§e奖励增强：§r\n1. §b经验提升§r：现在被玩家或被驯服的狼杀死时，会掉落 §a275§r 点经验值（原为 5 点）。\n2. §b稀有掉落§r：若玩家对单个监守者累计造成超过 §c50%§r 最大生命值的伤害，该监守者死亡时有 §a10%§r 的概率掉落§6《灵魂绑定》§r附魔书（受抢夺影响，每级增加 2.5%）。\n\n§7提示：虽然经验变多了，但它依然非常危险，建议做好充分准备再进行挑战。§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "【阅历附魔】\n增加击杀生物时获得的经验收益。\n\n§e效果：§r\n1. §b收益提升§r：每一级增加 §a25%§r 的经验收益（向上取整）。\n2. §b生效条件§r：仅在主手持有该附魔武器击杀生物时生效。\n3. §b范围§r：对除击杀玩家外的所有生物有效。\n\n§7说明：最高等级为 4 级。30 级附魔台最高只能刷出 3 级，4 级需通过铁砧合并或特殊战利品获得。§r");
			translationBuilder.add("jei.tuanzis_mod.warden_heart.description", "【坚守者的心脏】\n击杀监守者 (Warden) 获得的核心材料，蕴含着其顽强生命力与声波能量。\n\n§e获取方式：§r\n1. §b稀有掉落§r：击杀监守者 (Warden) 时有 §a25%§r 的概率掉落（受抢夺附魔影响，每级抢夺增加 10% 掉落概率）。\n\n§e用途：§r\n1. §b合成原料§r：与 3 个 §b回响碎片 (Echo Shard)§r 和 1 根 §b木棍§r 合成强力防御武器——§6回响破障者§r。\n2. §b装备修复§r：可用于在铁砧中修复§6回响破障者§r武器。");
			translationBuilder.add("jei.tuanzis_mod.echo_breaker.description", "【回响破障者】\n使用坚守者的心脏与回响碎片锻造的终极防御型防具/武器。蕴藏着能抵御甚至反弹声波伤害的神奇力量。\n\n§e被动效果：§r\n1. §b声波吸收§r：仅在主手持有生效，副手及快捷栏不生效。在受到监守者音波爆破伤害时，§a自动减免 30%§r 伤害。\n\n§e主动效果 (右键使用)：§r\n1. §b使用限制§r：仅在§c副手无物品§r时才可以使用右键格挡。\n2. §b完美格挡§r：右键格挡持续 §e0.5 秒§r。格挡期间§a免疫 100% 音波爆破伤害§r。\n3. §b声波反弹§r：在完美格挡音波爆破时，将声波完美反弹给发射源，对目标造成 §c15 点§r 真实伤害并附加 §9强烈击退§r 效果。\n4. §b格挡代价§r：每次格挡成功将额外扣除 §c3 点§r 耐久值。\n5. §b冷却机制§r：格挡§c失败§r则触发 §e5.0 秒§r 冷却，若格挡§a成功§r则触发 §68.0 秒§r 冷却。\n\n§e修复与维护：§r\n可以使用§b坚守者的心脏§r在铁砧中修复其耐久度。");
			translationBuilder.add("enchantment.tuanzis_mod.chain_mining", "连锁采掘");
			translationBuilder.add("enchantment.tuanzis_mod.blood_rage", "血怒");
			translationBuilder.add("enchantment.tuanzis_mod.berserker", "狂战士");
			translationBuilder.add("effect.tuanzis_mod.blood_rage", "血怒");
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
				"1. §b破甲减免§r：作为代价，胸甲本身提供的基础护甲值减少 §c35%（等级 I）§r 或 §c30%（等级 II）§r（即实际保留胸甲基础护甲值的 65% 或 70%，例如 8 点护甲的钻石胸甲在 II 级下仅扣除 2 点，仍可提供 6 点护甲，绝非削减到仅剩 30%）。\n" +
				"2. §b冲突互斥§r：与 §b保护§r、§b火焰保护§r、§b爆炸保护§r、§b弹射物保护§r、§b摔落保护§r 等所有保护类附魔互斥。\n\n" +
				"§7获取方式：最高等级 II 级。可通过附魔台直接附魔（稀有级）、战利品箱搜刮或村民交易获得。§r";
			translationBuilder.add("jei.tuanzis_mod.berserker.description", berserkerDesc);

			translationBuilder.add("hud.tuanzis_mod.chain_mining.active", "连锁挖掘§a已激活");
			translationBuilder.add("jei.tuanzis_mod.scarecrow.description", "【稻草人】\n一个普通的装饰性物品，也是制作高级防御道具——§b假目标 (Decoy Totem)§r 的核心基础组件。\n\n§e获取方式：§r\n1. §b无序合成§r：由 1 个干草块、1 根木棍和 1 个雕刻南瓜合成。");
			translationBuilder.add("jei.tuanzis_mod.decoy_totem.description", "【假目标】\n极其强大的战略防御道具。使用后可以完美吸引周围敌对生物的注意力，为自己创造安全的输出或逃生环境。\n\n§e效果与机制：§r\n1. §b幻影生成§r：放置后产生一个持续 §e15 秒§r 的玩家幻影，吸引半径 §a10 格§r 内所有怪物的仇恨。\n2. §b完美伪装§r：幻影与释放时的玩家一模一样（包括皮肤、身上所穿盔甲与手持物品）。\n3. §b属性继承§r：幻影的血量与护甲值完美继承释放瞬间玩家的数据。\n4. §b拟真行为§r：幻影会随机进行走动、跑动、跳跃等模拟真实玩家的动作。在受到伤害时，会立即切换为奔跑动作大跳着逃跑，远离伤害来源！\n5. §b碎裂余威§r：幻影死亡时不掉落任何物品，但发出玻璃般的碎裂声。激怒半径 §a10 格§r 内所有怪物，使其获得 §c速度 II§r 效果持续 §e10 秒§r。\n\n§e获取方式：§r\n1. §b有序合成§r：上排为[空、幽匿脉络、空]，中排为[幻翼膜、稻草人、幻翼膜]，下排为[萤石、红石、萤石]。\n\n§7注：每次使用会触发 §c15 秒§r 的物品冷却时间。");

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
			translationBuilder.add("jei.tuanzis_mod.compatibility.infinity_mending", "[Enchantment Compatibility]\n§bInfinity§r and §bMending§r are now compatible and can be applied to the same bow simultaneously.");
			translationBuilder.add("item.minecraft.warden_spawn_egg.jei.description", "[Warden]\nAn extremely dangerous creature that typically lurks within the Ancient Cities of the Deep Dark.\n\n§eReward Boost:§r\n1. §bXP Increase§r: Now drops §a275§r experience points when killed by a player or a tamed wolf (was 5).\n2. §bRare Drop§r: If a player deals more than §c50%§r of its max health as damage, it has a §a10%§r chance to drop a §6Soulbound§r enchanted book (affected by Looting, +2.5% per level).\n\n§7Tip: Even with the increased reward, it remains highly dangerous. Prepare thoroughly before engaging.§r");

			translationBuilder.add("jei.tuanzis_mod.experience.description", "[Experience Enchantment]\nIncreases the experience points gained from killing mobs.\n\n§eEffects:§r\n1. §bXP Boost§r: Each level increases experience gain by §a25%§r (rounded up).\n2. §bCondition§r: Only effective when killing mobs with the enchanted weapon in the main hand.\n3. §bScope§r: Effective against all mobs except players.\n\n§7Note: Max level is 4. Level 30 enchantment table can yield up to level 3. Level 4 requires anvil merging or special loot.§r");
			translationBuilder.add("jei.tuanzis_mod.warden_heart.description", "[Warden Heart]\nA core material obtained from defeating the Warden, containing its resilient life force and sonic energy.\n\n§eAcquisition:§r\n1. §bRare Drop:§r Defeating a Warden has a §a25%§r chance to drop it (affected by Looting, +10% probability per level).\n\n§eUsage:§r\n1. §bCrafting Ingredient:§r Combine with 3 §bEcho Shards§r and 1 §bStick§r to craft the powerful defensive weapon — §6Echo Breaker§r.\n2. §bEquipment Repair:§r Can be used to repair the §6Echo Breaker§r in an anvil.");
			translationBuilder.add("jei.tuanzis_mod.echo_breaker.description", "[Echo Breaker]\nAn ultimate defensive weapon forged using a Warden Heart and Echo Shards. It holds the mysterious power to resist and even deflect sonic damage.\n\n§ePassive Effects:§r\n1. §bSonic Absorption:§r Effective only when held in the main hand. Automatically §areduces Sonic Boom damage by 30%§r.\n\n§eActive Effects (Right-click):§r\n1. §bRestrictions:§r Right-click block can only be used when the §coffhand is empty§r.\n2. §bPerfect Block:§r Right-click to block for §e0.5 seconds§r. During blocking, grants §a100% immunity to Sonic Boom damage§r.\n3. §bSonic Deflection:§r Blocking a Sonic Boom perfectly deflects the sound wave back to its source, dealing §c15 true damage§r and applying §9strong knockback§r.\n4. §bDurability Cost:§r Each successful block consumes an extra §c3 durability points§r.\n5. §bCooldown Mechanics:§r Triggers a §c5.0-second§r cooldown on §cfailure§r (no damage blocked); triggers an §a8.0-second§r cooldown on §asuccess§r (perfectly deflected).\n\n§eRepair & Maintenance:§r\nCan be repaired in an anvil using a §bWarden Heart§r.");
			translationBuilder.add("enchantment.tuanzis_mod.chain_mining", "Chain Mining");
			translationBuilder.add("enchantment.tuanzis_mod.blood_rage", "Blood Rage");
			translationBuilder.add("enchantment.tuanzis_mod.berserker", "Berserker");
			translationBuilder.add("effect.tuanzis_mod.blood_rage", "Blood Rage");
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
		}
	}
}
