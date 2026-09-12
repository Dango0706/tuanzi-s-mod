# Task Completion Workflow

When a coding task is completed, verify and finalize following these steps:

## Checklist & Execution Steps
1. **DataGen**: If recipes, models, lang, loot tables, or tags were added/changed:
   ```powershell
   ./gradlew runDatagen
   ```
2. **Compile & Verification**: Ensure code compiles without errors:
   ```powershell
   ./gradlew build -x test
   ```
3. **Creative Tab & JEI Verification**:
   - Verify all new items/blocks are present in creative tabs.
   - Verify JEI registration in `TuanzisJeiPlugin`.
4. **Update Logs**:
   - Check version in `gradle.properties` (`mod_version`).
   - Append changes to `Update_log/` (Chinese & English).
5. **Serena Maintenance Check**:
   - Run `serena memories check` or verify references if memories were updated.
