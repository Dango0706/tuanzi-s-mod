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
