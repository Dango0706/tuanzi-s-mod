import os
import zipfile

cache_dir = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1")
found = False

for root, dirs, files in os.walk(cache_dir):
    for file in files:
        if "fabric-rendering-v1" in file and file.endswith(".jar") and "sources" not in file:
            jar_path = os.path.join(root, file)
            try:
                with zipfile.ZipFile(jar_path, 'r') as jar:
                    has_class = any("WorldRenderEvents" in name for name in jar.namelist())
                    print(f"Jar: {file} | Has WorldRenderEvents: {has_class}")
                    if has_class:
                        found = True
            except Exception as e:
                pass

if not found:
    print("Class WorldRenderEvents not found in any fabric-rendering-v1 jar.")
