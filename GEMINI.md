# GEMINI.md

## 核心指令 (Core Mandates)
- **始终使用中文输出 (Always output in Chinese).**
- **编译环境:** 推荐使用 JBR 25 (JetBrains Runtime) 以支持增强的热重载 (HotSwap) 功能。
- **环境要求:** 你始终处于 PowerShell 环境中，必须使用 PowerShell 命令而非 bash。
- **Gradle 运行:** 运行 `gradlew` 时，可直接运行 `./gradlew <任务名>`。已在 `gradle.properties` 中全局配置了 `org.gradle.java.home=C:\\Program Files\\Zulu\\zulu-25`，Gradle 将自动使用 Java 25 运行。
- **优先使用 Serena MCP:** 当需要检索代码、分析符号或执行安全命令时，必须优先使用 Serena MCP 工具，而不是自行猜测或使用低效的文本搜索。尽量不要使用内置的 Search 工具（如 `grep_search`、`find_by_name` 等），多使用 Serena MCP 提供的工具进行代码搜索、符号查询与上下文检索,搜索不到时在使用内置的搜索工具,尽量不要使用命令行去进行搜索。
- **工具优先:** 除上述MCP使用外,能使用系统提供的工具（如 `replace`, `write_file` 等）时，绝不使用命令行指令。
- **数据生成:** 语言（Lang）、合成（Recipe）、标签（Tag）等资源文件一律使用 Data Generation (DataGen) 完成，禁止手动编辑相关 JSON。
- **源码参考:** Minecraft 版本为 26.3（2026年9月发布）。执行代码操作前，必须先阅读 `MinecraftSources` 下的源码。若无该目录，请运行 `./gradlew genSources` 并解压至此。
- **热重载配置:** 
    - 为了支持不重启修改 Mixin，请在 IDE 运行配置的 VM 参数中手动添加：`-Dmixin.hotSwap=true`
    - 若使用 JBR，可额外添加 `-XX:+AllowEnhancedClassRedefinition` 以开启增强类重定义。
- **创造模式集成:** 每次创作的物品、方块、附魔（包括其附魔书）等，均必须放进对应的创造模式物品栏中，确保玩家能在创造模式直接获取。
- **拾荒回廊创造模式物品栏:** 为“拾荒回廊”系统设立了专属创造模式物品栏（`scavenger_corridor_tab`）。凡是制作“拾荒回廊”系统的道具、方块或相关物品时，必须统一归类并注册到该专属创造模式物品栏中，禁止随意混入通用物品栏。
- **JEI 支持:** 每次新增的物品/附魔等，均需为其注册对应的 JEI 信息页，清晰展示该物品/附魔的用途与规则。
- **更新日志更新:** 每次新增或重大调整的内容，均必须及时更新到对应的模组更新日志（如 `Update_log/` 下的中英文 md 文件）中。
- **模型设计规范:** 每次需要制作模型（包括方块、生物、道具模型等）时,禁止使用无头模式,生成好后使用ask_question工具询问用户是否满意，**必须严格参考并遵守项目根目录下的 `blockbench.md` 文件中定义的开发规范**。严格禁止写实主义、平滑圆弧、高面数抛光，必须且仅使用低多边形阶梯状立方体构造，确保符合 Minecraft 的经典原版风格。
- **贴图生成与处理规范:**
    - **贴图生成**: 应使用图像生成工具生成贴图，且生成贴图时背景最好为纯色（如纯白、纯黑或红绿蓝等单色），便于后续抠图,生成的风格必须为像素风格,贴近Minecraft原版风格的贴图,禁止天马行空,确保符合 Minecraft 的经典原版风格。
    - **统一脚本处理**: 生成贴图后，**必须使用根目录下的 Python 脚本 `process_texture.py` 进行去背、非透明边缘剪裁与高保真等比缩放**，严禁填充任何杂色，确保材质图层完美贴合像素规范。
    - **尺寸缩小与用户询问**: 所有物品或材质贴图最终必须缩小至 `16*16` 或 `32*32` 的像素规格。在缩小图片前，**必须调用 `ask_question` 工具主动询问用户希望缩小的尺寸需求**（例如 16 * 16 或 32 * 32），获取选择后调用脚本处理。
    - **生成贴图规范**: 当你要生成工具/武器等贴图时,严格遵守以下生成贴图规范:
        - 摆放角度：物品整体呈斜向45度朝上摆放，类似Minecraft快捷栏中工具图标的朝向。
        - 定位规则：手柄/握持端始终位于画面的左下角，工作头部（如镐尖、斧刃、剑尖）指向右上角。
        - 视觉参考：想象一把钻石镐——木棍手柄从左下延伸至中心偏右，镐头在右上区域，整体形成稳定的对角线构图。
        - 关键约束：方向不可偏离，哪怕物品造型不同（如剑的护手、锹的平头），也必须通过旋转和构图使其符合“左下握柄，右上功能端”的斜45°法则。
    - **脚本使用示例**:
      ```powershell
      # 去除白底背景，紧贴边缘裁剪，高保真缩放到 32x32 (默认)：
      python process_texture.py <输入路径> <输出路径> --size 32 --bg white
      
      # 去除黑底背景，紧贴边缘裁剪，高保真缩放到 16x16：
      python process_texture.py <输入路径> <输出路径> --size 16 --bg black
      ```
- **日志版本号规范**: 编写或创建模组版本更新日志时，版本号命名必须严格根据 `gradle.properties` 中的 `mod_version` 常量版本值来走，禁止自创或臆测下一个版本号。
- **伤害计算规范**: 模组内所有自定义伤害或受到伤害的计算与加成调整，必须严格使用 `me.tuanzi.util.DamageCalculator` 类进行（支持加法累加模型），禁止在各处分散编写 ad-hoc 的伤害修改逻辑，以确保伤害计算的统一与可维护性。
- **禁止删除文件**: 禁止你使用任何方式（包括工具、所有命令、各种脚本等）删除任何文件。需要删除文件时，将其重命名为原来的名字后缀加上 `.bak`，如 `xxx.png` -> `xxx.png.bak`。
- **逻辑与伤害计算调试日志**: 将所有逻辑性的计算/获得效果/伤害计算/概率计算等全部使用 `me.tuanzi.util.ModLog` 的 `debug` 方法进行输出，便于调试和寻找问题。
- **分层自动化测试与交付规范 (Testing & Delivery Conventions)**: 代码编写完毕后，必须遵循**由浅入深、先代码后真机**的分层测试流程，直至全部测试通过后方可向用户交付，严禁带错交付：
  - **1. Fabric Loader JUnit 单元/逻辑测试**: 统一置于 `src/test/java` 下，优先针对纯算法、数学公式、数据结构、NBT 解析、伤害计算公式（如 `DamageCalculator`）等编写并运行单元测试（`./gradlew test`），极速验证底层核心计算与基础逻辑是否有误。
  - **2. Minecraft GameTest 机制/结构测试**: 针对方块状态转变、红石信号、掉落物机制、实体基础 AI、结构生成等原版交互机制，使用 GameTest 框架编写并运行批处理测试（`./gradlew runGametest`），快速排查基础游戏机制是否有误。
  - **3. MCTest MCP 最终端到端动态交互测试**: 在基础逻辑与机制测试通过后，最后全面采用 `mctest` MCP 工具链（如 `mc_launch`、`mc_world_create`/`mc_world_join`、`mc_teleport`、`mc_click_widget`、`mc_interact_entity`、`mc_screenshot` 等）在真实游戏环境中模拟玩家操作、GUI 交互、技能释放与视觉渲染核验。测试执行完毕或发生异常时，**必须调用 `mc_close_all` 及时清理释放游戏进程**。
  - **测试截图前置调整**: 在使用 MCTest MCP 截取测试结果图片前，**必须先调整好玩家位置（Position/Coordinates）、视角朝向与角度（Pitch/Yaw 或 Look At）以及飞行状态（Flying）**，确保被测目标（方块、实体、界面、粒子等）处于最佳可视观察范围，杜绝视线遮挡、距离过远或视角偏离。
  - **交付标准**: 底层测试（JUnit/GameTest）无失败断言，且最终在 MCTest MCP 真实游戏环境中端到端验证通过、功能完全符合预期、截图核对无误且客户端无未捕获异常或崩溃后，方可向用户交付。

## Project Overview
**tuanzi's_mod** is a Minecraft mod built on the **Fabric Loader** for Minecraft version 26.3 (as specified in `gradle.properties`). It utilizes the **Fabric API** and follows the standard Fabric mod structure.

- **Main Entry Point:** `me.tuanzi.Tuanzis_mod`
- **Data Generator:** `me.tuanzi.Tuanzis_modDataGenerator`
- **Mod ID:** `tuanzis_mod`
- **License:** CC0-1.0 (Template license)
- **Technologies:** Java 25, Gradle, Fabric Loom

## Building and Running
The project uses the Gradle wrapper (`./gradlew`) for all tasks.

- **Build Mod:**
  ```powershell
  ./gradlew build
  ```
- **Run Minecraft Client:**
  ```powershell
  ./gradlew runClient
  ```
- **Run Minecraft Server:**
  ```powershell
  ./gradlew runServer
  ```
- **Generate Data (Datagen):**
  ```powershell
  ./gradlew runDatagen
  ```
- **Clean Build Directory:**
  ```powershell
  ./gradlew clean
  ```
- **Run Tests (Fabric Loader JUnit):**
  ```powershell
  ./gradlew test
  ```
- **Run GameTests (GameTest Server):**
  ```powershell
  ./gradlew runGametest
  ```
- **Run MCTest MCP (Dynamic E2E Testing):**
  全面使用 MCTest MCP 工具链（如 `mc_launch`, `mc_wait_ready`, `mc_world_create`, `mc_teleport`, `mc_screenshot`, `mc_close_all` 等）进行游戏内端到端自动化与动态测试。

## Development Conventions
- **Naming:** Mod ID `tuanzis_mod` should be used for assets and registration.
- **Logging:** Use the static `LOGGER` in `Tuanzis_mod.java` for all logging.
- **Entry Points:** Main initialization logic belongs in `Tuanzis_mod#onInitialize`.
- **Mixins:** Mixins are configured in `src/main/resources/tuanzis_mod.mixins.json`.
- **Resources:** Assets are located in `src/main/resources/assets/tuanzis_mod/`.
- **Data Generation:** Use the `fabric-datagen` entry point for generating recipe, tag, and loot table files.

## Project Structure
- `src/main/java`: Source code.
- `src/main/resources`: Mod configuration, assets, and data.
- `src/test/java`: Test source code (Fabric Loader JUnit & GameTest).
- `build.gradle`: Project dependencies and build configuration.
- `gradle.properties`: Versioning and environment settings.
- `fabric.mod.json`: Mod metadata and entry points.
