import os
import zipfile
import json
import glob

def find_minecraft_jars():
    home = os.path.expanduser("~")
    gradle_caches = os.path.join(home, ".gradle", "caches")
    pattern = os.path.join(gradle_caches, "**", "*.jar")
    print(f"Searching for jars in: {gradle_caches}")
    jar_files = glob.glob(pattern, recursive=True)
    return jar_files

def extract_models():
    jars = find_minecraft_jars()
    target_files = {
        "assets/minecraft/models/block/cube_all.json": "cube_all.json",
        "assets/minecraft/models/block/slab.json": "slab.json",
        "assets/minecraft/models/block/slab_top.json": "slab_top.json",
        "assets/minecraft/models/block/stairs.json": "stairs.json",
        "assets/minecraft/models/block/inner_stairs.json": "inner_stairs.json",
        "assets/minecraft/models/block/outer_stairs.json": "outer_stairs.json"
    }
    
    extracted = {}
    for jar in jars:
        if len(extracted) == len(target_files):
            break
        # 只看可能是 minecraft 相关的 jar，加速搜索
        jar_name = os.path.basename(jar).lower()
        if "minecraft" not in jar_name and "mapped" not in jar_name and "client" not in jar_name and "intermediary" not in jar_name:
            continue
        try:
            with zipfile.ZipFile(jar, 'r') as zf:
                nl = zf.namelist()
                for path_in_jar, out_name in target_files.items():
                    if path_in_jar in nl and out_name not in extracted:
                        print(f"Extracting {path_in_jar} from {jar}...")
                        content = zf.read(path_in_jar).decode("utf-8")
                        extracted[out_name] = content
        except Exception as e:
            pass
            
    if not extracted:
        # 如果从 gradle 缓存没找到，可能也可以在 gradle project classpath 找，但这里直接搜索所有 jar 已经很全了
        print("No files extracted. Trying a broader jar search...")
        # 尝试搜索包含 assets/minecraft/models/block/stairs.json 的任意 jar
        for jar in jars:
            if len(extracted) == len(target_files):
                break
            try:
                with zipfile.ZipFile(jar, 'r') as zf:
                    nl = zf.namelist()
                    for path_in_jar, out_name in target_files.items():
                        if path_in_jar in nl and out_name not in extracted:
                            print(f"Extracting {path_in_jar} from {jar}...")
                            content = zf.read(path_in_jar).decode("utf-8")
                            extracted[out_name] = content
            except Exception as e:
                pass
                
    # 写入提取的文件并生成对应的 tinted 模型
    out_dir = os.path.join("src", "main", "resources", "assets", "tuanzis_mod", "models", "block")
    os.makedirs(out_dir, exist_ok=True)
    
    for name, content in extracted.items():
        data = json.loads(content)
        # 为所有的 elements 的 faces 添加 tintindex
        if "elements" in data:
            for element in data["elements"]:
                if "faces" in element:
                    for face_name, face_data in element["faces"].items():
                        # slab_top.json 的 tintindex 应该为 1，其他为 0
                        tint_index = 1 if name == "slab_top.json" else 0
                        face_data["tintindex"] = tint_index
        else:
            print(f"Warning: {name} has no elements!")
            
        tinted_name = "tinted_" + name
        out_path = os.path.join(out_dir, tinted_name)
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
        print(f"Generated: {out_path}")

if __name__ == "__main__":
    extract_models()
