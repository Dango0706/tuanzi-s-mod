# Suggested Commands (PowerShell on Windows)

Use PowerShell syntax exclusively (`./gradlew ...`, not bash `./gradlew` or sh).

## Build & Datagen
- Run Data Generation:
  ```powershell
  ./gradlew runDatagen
  ```
- Build Jar:
  ```powershell
  ./gradlew build
  ```
- Clean Build Cache:
  ```powershell
  ./gradlew clean
  ```

## Testing & Verification
- Run Tests (Fabric Loader JUnit):
  ```powershell
  ./gradlew test
  ```
- Run GameTests:
  ```powershell
  ./gradlew runGametest
  ```
- Run MCTest MCP:
  Use MCTest MCP tools (`mc_launch`, `mc_wait_ready`, `mc_world_create`, `mc_screenshot`, `mc_close_all`, etc.) for in-game dynamic E2E testing.

## Running the Game
- Launch Client:
  ```powershell
  ./gradlew runClient
  ```
- Launch Server:
  ```powershell
  ./gradlew runServer
  ```

## Decompiling & Sources
- Generate MC Sources:
  ```powershell
  ./gradlew genSources
  ```

## Texture Processing
- Python Texture Script:
  ```powershell
  # 32x32 white background removal
  python process_texture.py <input> <output> --size 32 --bg white
  # 16x16 black background removal
  python process_texture.py <input> <output> --size 16 --bg black
  ```
