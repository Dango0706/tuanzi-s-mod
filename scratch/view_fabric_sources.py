import os
import zipfile

cache_dir = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1")
target_jar = "fabric-rendering-v1-23.0.4+c7e428fd47-sources.jar"

for root, dirs, files in os.walk(cache_dir):
    for file in files:
        if file == target_jar:
            jar_path = os.path.join(root, file)
            print(f"Reading LevelRenderEvents.java from {jar_path}:")
            with zipfile.ZipFile(jar_path, 'r') as jar:
                for name in jar.namelist():
                    if "LevelRenderContextBackwardsCompatHack.java" in name:
                        with jar.open(name) as f:
                            print(f.read().decode('utf-8'))
            break
