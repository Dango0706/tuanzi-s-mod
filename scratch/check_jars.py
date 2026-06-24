import os

cache_dir = os.path.expanduser("~/.gradle/caches")
jars_found = []

for root, dirs, files in os.walk(cache_dir):
    for file in files:
        if "fabric-rendering-v1" in file and file.endswith(".jar"):
            jars_found.append(os.path.join(root, file))

print(f"Total fabric-rendering-v1 jars found: {len(jars_found)}")
for jar in sorted(jars_found):
    print(jar)
