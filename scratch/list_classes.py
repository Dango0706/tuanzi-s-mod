import os
import zipfile

cache_dir = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1")
target_jar = "fabric-rendering-v1-23.0.4+c7e428fd47.jar"

for root, dirs, files in os.walk(cache_dir):
    for file in files:
        if file == target_jar:
            jar_path = os.path.join(root, file)
            print(f"Listing contents of {jar_path}:")
            with zipfile.ZipFile(jar_path, 'r') as jar:
                classes = [name for name in jar.namelist() if name.endswith(".class")]
                for c in sorted(classes):
                    print(c)
            break
