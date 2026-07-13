import os
import json

gradle_cache = r"C:\Users\12616\.gradle"
found = False

for root, dirs, files in os.walk(gradle_cache):
    for file in files:
        if file == "grass_block.json":
            path = os.path.join(root, file)
            print(f"Found grass_block.json at: {path}")
            with open(path, "r", encoding="utf-8") as f:
                try:
                    data = json.load(f)
                    print(json.dumps(data, indent=2))
                    found = True
                    break
                except Exception:
                    pass
    if found:
        break
