import os
import zipfile

cache_dir = os.path.expanduser("~/.gradle/caches")
found = False

for root, dirs, files in os.walk(cache_dir):
    for file in files:
        if file.endswith(".jar"):
            jar_path = os.path.join(root, file)
            try:
                with zipfile.ZipFile(jar_path, 'r') as jar:
                    for name in jar.namelist():
                        if "MultiBufferSource.class" in name:
                            print(f"Found class: {name} in {jar_path}")
                            found = True
            except Exception as e:
                pass

if not found:
    print("MultiBufferSource not found in any Minecraft-related jar.")
