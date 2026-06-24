import os

def search_text(directory, text):
    text_lower = text.lower()
    for root, dirs, files in os.walk(directory):
        for file in files:
            if 'Strider' in file:
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        for line_num, line in enumerate(f, 1):
                            if text_lower in line.lower():
                                print(f"{path}:{line_num}: {line.strip()}")
                except Exception as e:
                    pass

search_text(r"f:\Development\Java\tuanzis_mods\MinecraftSources", "collision")
