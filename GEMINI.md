# GEMINI.md

## 核心指令 (Core Mandates)
- **始终使用中文输出 (Always output in Chinese).**
- **编译环境:** 推荐使用 JBR 25 (JetBrains Runtime) 以支持增强的热重载 (HotSwap) 功能。
- **环境要求:** 你始终处于 PowerShell 环境中，必须使用 PowerShell 命令而非 bash。
- **Gradle 运行:** 运行 `gradlew` 时，始终使用 `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-25';` 前缀。
- **工具优先:** 能使用系统提供的工具（如 `replace`, `write_file` 等）时，绝不使用命令行指令。
- **数据生成:** 语言（Lang）、合成（Recipe）、标签（Tag）等资源文件一律使用 Data Generation (DataGen) 完成，禁止手动编辑相关 JSON。
- **源码参考:** Minecraft 版本为 26.1（2026年3月24日发布）。执行代码操作前，必须先阅读 `MinecraftSources` 下的源码。若无该目录，请运行 `$env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'; ./gradlew genSources` 并解压至此。
- **热重载配置:** 
    - 为了支持不重启修改 Mixin，请在 IDE 运行配置的 VM 参数中手动添加：`-Dmixin.hotSwap=true`
    - 若使用 JBR，可额外添加 `-XX:+AllowEnhancedClassRedefinition` 以开启增强类重定义。
- **材质透明度:** 所有生成或修改的物品与方块材质文件（PNG）必须具有真正的 Alpha 透明通道（背景完全透明），禁止在背景中填充类似透明的灰白网格、纯白或纯黑图层。

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
  $env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'; ./gradlew build
  ```
- **Run Minecraft Client:**
  ```powershell
  $env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'; ./gradlew runClient
  ```
- **Run Minecraft Server:**
  ```powershell
  $env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'; ./gradlew runServer
  ```
- **Generate Data (Datagen):**
  ```powershell
  $env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'; ./gradlew runDatagen
  ```
- **Clean Build Directory:**
  ```powershell
  $env:JAVA_HOME='C:\Program Files\Zulu\zulu-25'; ./gradlew clean
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
