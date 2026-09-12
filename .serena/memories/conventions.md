# Codebase Conventions & Standards

## Mod Invariants & Data Generation
- Data Generation First: Recipes, tags, languages (`zh_cn`, `en_us`), loot tables, and blockstates MUST be generated via `fabric-datagen` (`me.tuanzi.datagen`). Never hand-edit resource JSONs.
- Creative Tab Integration: Every new item, block, weapon, or enchantment must be registered to the corresponding creative inventory tab.
- JEI Info: Every new item, block, and enchantment must have a corresponding JEI recipe/info page registered via `me.tuanzi.jei.TuanzisJeiPlugin`.
- Update Logs: Major features and updates must be recorded in `Update_log/` in both Chinese and English, strictly matching the `mod_version` in `gradle.properties`.

## Damage Calculation & Logging
- Damage Calculation Rule: All custom damage modifications, attack boosts, or damage reduction MUST go through `me.tuanzi.util.DamageCalculator` (additive accumulator model). No ad-hoc scattered damage arithmetic.
- Debug Logging: All calculation results, buffs, probabilities, and damage logic MUST use `me.tuanzi.util.ModLog.debug(...)` for traceability.

## Model & Texture Guidelines
- Model Specifications: Must strictly comply with `blockbench.md`. Low-poly, stepped cube structures only. No realistic curves, high-poly meshes, or smooth primitives. Ask user for approval after generating models.
- Texture Specifications: Pixel art style conforming to vanilla Minecraft.
  - Direction: 45° angle, handle at bottom-left, functional head at top-right.
  - Dimensions: Ask user via `ask_question` whether to scale to `16x16` or `32x32`.
  - Process via `process_texture.py` for background removal and nearest-neighbor scaling.

## Safety & File Maintenance
- NEVER delete files. Always rename with a `.bak` extension (e.g. `file.png.bak`).
