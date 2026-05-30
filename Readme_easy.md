# 团子的模组 - 精简指南 (Tuanzi's Mod - Quick Guide)

**团子的模组 (Tuanzi's Mod)** 是一个基于 Minecraft **26.1 (Tiny Takeover)** 版本的 **Fabric** 高级玩法拓展模组。本指南为您快速展示其最核心、最有趣的几大特色！

---

## 🌟 核心特色 (Core Features)

### 🌈 1. 彩虹海绵染色 (Rainbow Sponge Formatting)
*   **解除限制**：彻底打破了铁砧重命名 50 字符的长度限制。
*   **铁砧编程**：在铁砧中命名彩虹海绵（输入颜色指令，如 `#FF5555`，`&l`，`&q5#FF0000-#0000FF`），再与装备合并，即可将炫酷的十六进制、原版样式或平滑渐变渲染至物品名称上。

### 💼 2. 灵笼与无 AI 交易 (Villager Caging & AI-less Trading)
*   **缚灵笼**：右键一键捕捉村民进背包（保留职业、等级、交易及改名）。在 Iron Golem 巡逻范围内捕捉会瞬间吸引仇恨。
*   **灵笼贸易站**：塞入村民后，右键可进行完全一致的无 AI 交易，极大地优化游戏性能。将对应的未占用工作站贴在其相邻的 6 面，每日黎明即可自动解锁锁定。

### 🛡 3. 专属装备与测试假人 (Special Equipment & Trial Dummy)
*   **尤里的复仇**：只拥有 5 点耐久度的神镐，**只能挖掘基岩**。
*   **回响破障者**：主手持有减免 Warden 音波伤害 30%；副手空手右键可防御 0.5s（100% 免伤），格挡瞬间反弹 15 点真实伤害并击退 Warden。
*   **试炼假人**：无敌测试实体（可为其穿戴全套盔甲），攻击不损武器耐久，箭矢原样回收。受击产生橙色 3D 浮空伤害跳字，ActionBar 实时显示 DPS，右键可打印极详尽的伤害报告。
*   **稻草人与假目标**：稻草人拥有 3D 立柱底座与受击偏摆晃动物理，夜晚变南瓜灯发光；假目标可召唤 15s 完美复制玩家装备的幻影来吸引仇恨，受击会逃跑，消失时产生粒子 Poof 特效。
*   **坐在楼梯上**：空手右键正向楼梯方块即可坐下，下车精准落至顶部，实体自动销毁，防卡与防重叠。

### 🔮 4. 战术护符与专属附魔 (Tactical Charms & Custom Enchantments)
*   **狂暴护符**：副手携带，HP 低于 30% 消耗一个瞬间提供 15s 力量II + 速度II 爆发（内置 15s 冷却防秒吃）。
*   **战狼护符**：狼生命 +4；攻击 15% 附加“撕裂”（移动受法伤，可叠四级）；濒死消耗耐久免死并无敌狂怒 5s，力竭倒地后喂 10 个腐肉满血复活。
*   **专属附魔**：
    *   **灵魂绑定**：死亡不掉落。
    *   **阅历**：击杀生物经验每级额外 +25%（最高 IV）。
    *   **熔炼**：自动烧炼掉落，兼容时运，冲突精准，多扣 1 耐久。
    *   **血怒**：击杀获 7s Buff（伤害+40% / 易伤+30%），超时未续杯受 2 点真伤反噬。
    *   **狂战士**：胸甲附魔，血越低近战伤害越高，但减少胸甲基础护甲。
    *   **处决**：对斩杀线以下目标造成大额增伤，但伴随真实反噬伤害。
    *   **连锁苦痛**：斧头附魔，击杀溢出伤害 50% 连锁传导给周围 5 个敌人。
    *   **追踪箭**：弩瞄准高亮目标，箭射出 0.5s 后向原目标进行最大 30 度弹道微修正。

### 🧪 5. 奇妙炼金与食品 (Alchemy & Foods)
*   **飞行药水**：生存模式飞行，到期前 ActionBar 进行倒计时和音效急促提示。
*   **不死药水**：赋予不死效果（仅限玩家及驯服生物生效）。
*   **肾上腺素**：每级提供攻速与移速加成，但产生牛奶无法清除的透支 Buff，结束后根据透支等级叠加虚弱与疲劳。
*   **地狱炖菜**：64 堆叠食完返空碗。提供地狱之火（火免、60%燃敌、燃敌增伤），但产生烈焰饥渴（饥饿消耗翻倍）与惧水（水中每秒受 2 点真伤）。

---

## 🛠 开发环境 (Development)
*   **版本适配**：Minecraft 26.1 / Fabric Loader 0.18.4 / Java 25 (JBR 25 最佳) / Gradle 9.4.1
*   **运行客户端**：`./gradlew runClient`
*   **构建模组**：`./gradlew build`

---

# Tuanzi's Mod - Easy Guide (English)

**Tuanzi's Mod** is an advanced game expansion mod built on **Fabric MC** for Minecraft **26.1 (Tiny Takeover)**. This quick guide outlines the most essential and exciting features!

---

## 🌟 Key Features

### 🌈 1. Rainbow Sponge Formatting
*   **Anvil Coding**: Rename a Rainbow Sponge in an anvil to write format commands (e.g. `#FF5555`, `&l`, `&q5#FF0000-#0000FF`).
*   **Format Merging**: Combine the sponge with target items to paint beautiful hex colors, gradients, and custom styles onto item names, bypassing the vanilla 50-character limit.

### 💼 2. Villager Caging & AI-less Trading
*   **Villager Cage**: Capture any villager to store all NBT trade and level data into the item. Capturing in Golem patrol area triggers aggro.
*   **Soul Merchant Station**: Place a caged villager in the station for zero-AI trading to boost performance. Place their workplace next to it to refresh trades at dawn automatically.

### 🛡 3. Special Weapons & Custom Entities
*   **Yuri's Revenge**: A mythical 5-durability pickaxe **only capable of mining Bedrock**.
*   **Echo Breaker**: Held in main hand reduces Warden sonic blast by 30%. Empty offhand right-click parries for 0.5s, parrying reflects 15 True Damage back and knocks Warden back.
*   **Trial Dummy**: Invulnerable simulator. Supports full armor equipping. Attacks consume 0 durability, arrow projectiles are fully recovered. Triggers orange floating 3D numbers, real-time DPS ActionBar, and a full analysis report.
*   **Scarecrow & Decoy**: Scarecrow features beautiful 3D fence posts, hit wobbles, and lights up at night. Decoy summons a 15s player clone that inherits gear, draws mob aggro, flees when hit, and explodes upon expiration.
*   **Stair Seating**: Click bottom-half stairs with empty hands to sit. Features collision checks and auto-destroy seat entity to avoid world trash.

### 🔮 4. Tactical Charms & Enchantments
*   **Berserk Charm**: Offhand charm. Auto-consumes at <30% HP to grant Strength II + Speed II for 15s (15s internal cooldown).
*   **Wolf Command**: Tamed wolves get +4 HP, 15% chance to apply "Tearing" (moves inflict magic damage), and death-defiance invulnerable enrage. Feed 10 Rotten Flesh to revive them.
*   **Enchantments**:
    *   **Soulbound**: Preserves items upon death.
    *   **Experience**: Boosts experience drops by +25% per level.
    *   **Smelting**: Auto-smelt drops. Fortune compatible.
    *   **Blood Rage**: Kills grant 7s rage (+40% damage dealt / +30% taken); expiring deals 2 True Damage backlash.
    *   **Berserker**: Less HP yields higher melee damage, but decreases armor rating.
    *   **Execute**: Heavy execute multiplier below HP threshold, but deals self true-damage feedback.
    *   **Chain Pain**: Propagation of 50% overflow kill damage to up to 5 nearby targets.
    *   **Seeking Arrow**: Crossbow aiming highlights targets; fired arrows home up to 30° after 0.5s.

### 🧪 5. Alchemy & Foods
*   **Flight Potion**: Enables survival flight with intensive ActionBar warnings before expiration.
*   **Undying Potion**: grants undying status (strictly protects only players and tamed pets).
*   **Adrenaline Potion**: Massive attack/move speed buff, but applies fatigue crash side effects (Weakness + Fatigue) upon expiration, multiplying if stacked.
*   **Nether Stew**: Stacks up to 64. Returns empty bowl. Grants fire immunity and burning attack buffs, but doubles hunger rate and inflicts 2 True Damage per second under water.

---

## 🛠 Development Setup
*   **Specs**: Minecraft 26.1 / Fabric Loader 0.18.4 / Java 25 / Gradle 9.4.1
*   **Run**: `./gradlew runClient`
*   **Build**: `./gradlew build`

---
*Created by Gemini CLI Agent*
