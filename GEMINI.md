# GEMINI.md

## 核心指令 (Core Mandates)
- **始终使用中文输出 (Always output in Chinese).**
- **编译环境:** 使用 Java 25 进行编译，路径为 `C:/Program Files/Zulu/zulu-25`。
- **开发参考:** 请参考 Minecraft 反编译后的本地源码来进行开发（源码 Jar 包通常由 Fabric Loom 自动下载并配置在 Gradle 依赖中，或参考项目根目录相关说明）。

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
