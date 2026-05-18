# 团子的模组 (Tuanzi's Mod)

**团子的模组** 是一个基于 Fabric Loader 开发的高级功能模组，专为 Minecraft **26.1 (Tiny Takeover)** 版本设计。它引入了独特的“颜色海绵”系统和“灵魂绑定”附魔，同时通过底层逻辑修改彻底释放了铁砧的潜力。

---

## 🌟 核心功能 (Key Features)

### 1. 颜色海绵系统 (Color Sponge System)
颜色海绵是一张“魔法指令卡”。通过铁砧“编程”后，它可以将各种绚丽的样式应用到任何物品的名称上。

*   **编程阶段**：在铁砧中通过重命名海绵来输入指令代码（此时海绵会显示纯文本或颜色示范）。
*   **应用阶段**：将已命名的海绵与目标物品在铁砧中合并，实时解析指令并应用效果。

#### 指令语法 (Command Syntax):
| 效果类型 | 示例代码 | 功能说明 |
| :--- | :--- | :--- |
| **十六进制** | `#FF5555` | 应用精确的 16 进制颜色（如亮红色）。 |
| **原版样式** | `&l`, `&o`, `&n` | 支持加粗、倾斜、下划线等原版样式代码（使用 `&` 代替 `§`）。 |
| **分割系统** | `&c,&b,&l` | 使用逗号分隔不同样式，按字序循环应用到物品名。 |
| **智能渐变** | `&q3#FF0000` | 基于起始色为接下来的 N 个字生成随机平滑渐变。 |
| **双色渐变** | `&q5#FF0000-#0000FF` | 在起始色和结束色之间生成线性渐变。 |
| **重置属性** | `&r` | 清除物品上所有自定义的颜色和样式属性。 |

### 2. 灵魂绑定附魔 (Soulbound Enchantment)
*   **作用**：带有此附魔的物品在玩家死亡时不会掉落，而是保留在玩家背包中。
*   **获取方式**：目前设定为无法通过自然方式（附魔台、村民、宝箱）获取，仅限创造模式或指令。

### 3. 铁砧上限解除 (Anvil Limit Removal)
*   彻底移除了生存模式下铁砧 **40 级** 的经验限制。
*   解决了“过于昂贵！”的报错，无论合并操作需要多少经验，只要玩家等级足够即可取出物品。

---

## 💰 经验费用表 (Exp Cost Table)

| 操作类型 | 命名海绵 (Naming) | 合并物品 (Merging) |
| :--- | :--- | :--- |
| **十六进制颜色** | 8 级 | 32 级 |
| **原版样式项 (每项)** | +4 级 | +16 级 |
| **分割符 (每个逗号)** | +2 级 | +8 级 |
| **渐变字符 (每个字)** | 8 级 | 32 级 |
| **重置样式 (&r)** | 1 级 | 16 级 |

---

## 🛠 开发环境 (Development)

*   **Minecraft 版本**: 26.1 (Tiny Takeover) - *去混淆环境*
*   **Java 版本**: Java 25 (Zulu JDK)
*   **构建工具**: Gradle 9.4.1
*   **模组加载器**: Fabric Loader 0.19.2
*   **核心依赖**: 
    *   Fabric API (v0.145.1+26.1)
    *   Just Enough Items (JEI v29.2.0.21)
    *   ModMenu (v18.0.0-alpha.8)

---

# Tuanzi's Mod (README English)

**Tuanzi's Mod** is an advanced feature mod built on the Fabric Loader, specifically tailored for Minecraft **26.1 (Tiny Takeover)**. It introduces a unique "Color Sponge" system and "Soulbound" enchantment, along with a complete overhaul of anvil mechanics.

---

## 🌟 Features

### 1. Color Sponge System
The Color Sponge acts as a "Magic Command Card". Program it via an anvil, then use it to apply vivid styles to any item name.

*   **Programming**: Input command codes by renaming the sponge in an anvil.
*   **Application**: Merge the programmed sponge with a target item to apply the effects.

#### Syntax Guide:
| Effect Type | Example | Description |
| :--- | :--- | :--- |
| **Hex Color** | `#FF5555` | Applies a precise hex color (e.g., Bright Red). |
| **Vanilla Style** | `&l`, `&o`, `&n` | Supports Bold, Italic, Underline, etc. |
| **Splitting** | `&c,&b,&l` | Separates styles by comma to apply them char-by-char. |
| **Smart Gradient** | `&q3#FF0000` | Generates random smooth gradients for N chars. |
| **Linear Gradient**| `&q5#FF0000-#0000FF`| Fades linearly from Start color to End color. |
| **Reset** | `&r` | Clears all custom styles and returns to default. |

### 2. Soulbound Enchantment
*   **Effect**: Items with this enchantment remain in your inventory upon death.
*   **Obtaining**: Disabled for survival (Enchanting Table/Villagers/Loot). Creative only.

### 3. Anvil Limit Break
*   Removes the **Level 40** limit in Survival mode.
*   Fixes "Too Expensive!" error. Items can be retrieved at any level cost as long as the player has sufficient XP.

---

## 💰 Level Cost Rules

| Action | Naming Sponge | Merging Item |
| :--- | :--- | :--- |
| **Hex Color** | 8 levels | 32 levels |
| **Vanilla Style (per code)** | +4 levels | +16 levels |
| **Split Char (per comma)** | +2 levels | +8 levels |
| **Gradient Char (per char)**| 8 levels | 32 levels |
| **Reset Style (&r)** | 1 level | 16 levels |

---

## 🛠 Build Setup

*   **Minecraft**: 26.1 (Tiny Takeover) - *Unobfuscated*
*   **Java**: Java 25 (Zulu JDK)
*   **Build Tool**: Gradle 9.4.1
*   **Loader**: Fabric Loader 0.19.2
*   **Dependencies**: 
    *   Fabric API (v0.145.1+26.1)
    *   JEI (v29.2.0.21)
    *   ModMenu (v18.0.0-alpha.8)

---

### 如何运行 (How to Run)
```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-25'
./gradlew runClient
```

---
*Created by Gemini CLI Agent*
