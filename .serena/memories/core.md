# Core Architecture & Source Map

## System Map
- Main Mod Initializer: `me.tuanzi.Tuanzis_mod` (entry point for common setup)
- Client Initializer: `me.tuanzi.client.TuanzisModClient` (client rendering, HUD, keybinds)
- Data Generator: `me.tuanzi.Tuanzis_modDataGenerator` (`fabric-datagen`)
- JEI Plugin: `me.tuanzi.jei.TuanzisJeiPlugin`
- ModMenu Integration: `me.tuanzi.integration.ModMenuIntegration`
- Mixin Configs: `tuanzis_mod.mixins.json`, `tuanzis_mod.client.mixins.json`

## Subpackage Layout
- `me.tuanzi.init`: Registry holders for items, blocks, entities, effects, sounds, creative tabs, etc.
- `me.tuanzi.item`: Custom item logic, weapons, armor, gacha items.
- `me.tuanzi.block`: Custom blocks and block entities.
- `me.tuanzi.effect`: Status effects.
- `me.tuanzi.entity`: Custom entities, projectiles, bosses.
- `me.tuanzi.gacha`: Gacha mechanics, pools, roll state.
- `me.tuanzi.command`: In-game commands.
- `me.tuanzi.network`: Networking packets and sync.
- `me.tuanzi.util`: Utility classes (`DamageCalculator`, `ModLog`, math/helper utils).
- `me.tuanzi.datagen`: Data generators for recipes, tags, loot tables, lang, models.

## Project Invariants
- Mod ID: `tuanzis_mod`. All registry keys, resource paths, and identifiers must use this ID.
- Never delete files directly; rename deprecated files with `.bak` suffix.
- All communications and documentation must be in Chinese.

## Linked Memories
- Technical runtime, dependencies, and environment: `mem:tech_stack`
- Build, test, and execution commands (PowerShell/Windows): `mem:suggested_commands`
- Coding, damage calculation, texture, and modeling rules: `mem:conventions`
- Quality checks and task finishing workflow: `mem:task_completion`
