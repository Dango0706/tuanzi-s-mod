# Tech Stack & Environment

## Core Technologies
- Platform: Fabric Loader (>= 0.19.5) for Minecraft 26.3
- Target Minecraft: 26.3
- Language: Java 25
- Runtime JDK: Zulu 25 / JBR 25 (JetBrains Runtime recommended for hotswap)
  - Configured in `gradle.properties`: `org.gradle.java.home=C:\\Program Files\\Zulu\\zulu-25`
- Build Tool: Gradle Wrapper 9.7.1 (`./gradlew`) with Fabric Loom (1.18-SNAPSHOT)
- OS: Windows (PowerShell environment)

## Key Dependencies & Integrations
- Fabric API: `0.161.0+26.3`
- Just Enough Items (JEI): `31.7.0.44` (`jei-26.3-fabric`)
- ModMenu: `21.0.0`
- Decompiled MC Sources: `./MinecraftSources` (generated via `./gradlew genSources`)
