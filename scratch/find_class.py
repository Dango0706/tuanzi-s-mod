import os
import zipfile

def search_class():
    gradle_cache = os.path.expanduser("~/.gradle/caches")
    print(f"Searching for BlockRenderLayerMap in {gradle_cache}...")
    for root, dirs, files in os.walk(gradle_cache):
        for file in files:
            if file.endswith(".jar"):
                jar_path = os.path.join(root, file)
                try:
                    with zipfile.ZipFile(jar_path, 'r') as z:
                        for name in z.namelist():
                            if "BlockRenderLayerMap" in name:
                                print(f"Found in jar: {jar_path}")
                                print(f"  Class path: {name}")
                except Exception:
                    pass

if __name__ == "__main__":
    search_class()
