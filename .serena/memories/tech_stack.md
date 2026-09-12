# Tech Stack & Environment

## Core Technologies
- Platform: Fabric Loader (>= 0.19.3) for Minecraft 26.2
- Target Minecraft: 26.2
- Language: Java 25
- Runtime JDK: Zulu 25 / JBR 25 (JetBrains Runtime recommended for hotswap)
  - Configured in `gradle.properties`: `org.gradle.java.home=C:\\Program Files\\Zulu\\zulu-25`
- Build Tool: Gradle Wrapper (`./gradlew`) with Fabric Loom (1.17-SNAPSHOT)
- OS: Windows (PowerShell environment)

## Key Dependencies & Integrations
- Fabric API: `0.152.2+26.2`
- Just Enough Items (JEI): `30.0.0.4` (`jei_mod_plugin`)
- ModMenu: `20.0.0-beta.3`
- Decompiled MC Sources: `./MinecraftSources` (generated via `./gradlew genSources`)
