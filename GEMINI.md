# GEMINI.md

## 核心指令 (Core Mandates)
- **始终使用中文输出 (Always output in Chinese).**
- **编译环境:** 推荐使用 JBR 25 (JetBrains Runtime) 以支持增强的热重载 (HotSwap) 功能。
- **环境要求:** 你始终处于 PowerShell 环境中，必须使用 PowerShell 命令而非 bash。
- **Gradle 运行:** 运行 `gradlew` 时，可直接运行 `./gradlew <任务名>`。已在 `gradle.properties` 中全局配置了 `org.gradle.java.home=C:\\Program Files\\Zulu\\zulu-25`，Gradle 将自动使用 Java 25 运行。
- **工具优先:** 能使用系统提供的工具（如 `replace`, `write_file` 等）时，绝不使用命令行指令。
- **数据生成:** 语言（Lang）、合成（Recipe）、标签（Tag）等资源文件一律使用 Data Generation (DataGen) 完成，禁止手动编辑相关 JSON。
- **源码参考:** Minecraft 版本为 26.1（2026年3月24日发布）。执行代码操作前，必须先阅读 `MinecraftSources` 下的源码。若无该目录，请运行 `./gradlew genSources` 并解压至此。
- **热重载配置:** 
    - 为了支持不重启修改 Mixin，请在 IDE 运行配置的 VM 参数中手动添加：`-Dmixin.hotSwap=true`
    - 若使用 JBR，可额外添加 `-XX:+AllowEnhancedClassRedefinition` 以开启增强类重定义。
- **创造模式集成:** 每次创作的物品、方块、附魔（包括其附魔书）等，均必须放进对应的创造模式物品栏中，确保玩家能在创造模式直接获取。
- **JEI 支持:** 每次新增的物品/附魔等，均需为其注册对应的 JEI 信息页，清晰展示该物品/附魔的用途与规则。
- **更新日志更新:** 每次新增或重大调整的内容，均必须及时更新到对应的模组更新日志（如 `Update_log/` 下的中英文 md 文件）中。
- **贴图生成与处理规范:**
    - **贴图生成**: 创建物品、药水效果或方块等需要贴图的内容时，应使用图像生成工具生成高品质贴图，且生成贴图时背景最好为纯色（如纯白、纯黑或红绿蓝等单色），便于后续抠图。
    - **去背透明化**: 生成贴图后必须手动或通过脚本去除背景，确保材质图层具有真正的 Alpha 透明通道（背景完全透明），严禁填充任何杂色或灰白网格背景。
    - **尺寸缩小与用户询问**: 所有物品或材质贴图最终必须缩小至 `16*16` 或 `32*32` 或不压缩的像素规格。在缩小图片前，**必须调用 `ask_question` 工具主动询问用户希望缩小的尺寸需求**（例如 16 * 16 或 32 * 32 或不压缩），获取选择后方可执行缩小处理。
- **日志版本号规范**: 编写或创建模组版本更新日志时，版本号命名必须严格根据 `gradle.properties` 中的 `mod_version` 常量版本值来走，禁止自创或臆测下一个版本号。
- **伤害计算规范**: 模组内所有自定义伤害或受到伤害的计算与加成调整，必须严格使用 `me.tuanzi.util.DamageCalculator` 类进行（支持加法累加模型），禁止在各处分散编写 ad-hoc 的伤害修改逻辑，以确保伤害计算的统一与可维护性。

## Project Overview
**tuanzi's_mod** is a Minecraft mod built on the **Fabric Loader** for Minecraft version 26.1 (as specified in `gradle.properties`). It utilizes the **Fabric API** and follows the standard Fabric mod structure.

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
- `build.gradle`: Project dependencies and build configuration.
- `gradle.properties`: Versioning and environment settings.
- `fabric.mod.json`: Mod metadata and entry points.
