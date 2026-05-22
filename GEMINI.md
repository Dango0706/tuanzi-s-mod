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
- **材质透明度:** 所有生成或修改的物品与方块材质文件（PNG）必须具有真正的 Alpha 透明通道（背景完全透明），禁止在背景中填充类似透明的灰白网格、纯白或纯黑图层。
- **创造模式集成:** 每次创作的物品、方块、附魔（包括其附魔书）等，均必须放进对应的创造模式物品栏中，确保玩家能在创造模式直接获取。
- **JEI 支持:** 每次新增的物品/附魔等，均需为其注册对应的 JEI 信息页，清晰展示该物品/附魔的用途与规则。
- **更新日志更新:** 每次新增或重大调整的内容，均必须及时更新到对应的模组更新日志（如 `Update_log/` 下的中英文 md 文件）中。

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
