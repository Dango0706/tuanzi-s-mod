import os
from PIL import Image

def process_image(input_path, output_path, target_size=16, bg_color="white", threshold=240):
    try:
        img = Image.open(input_path).convert("RGBA")
    except Exception as e:
        print(f"Failed to open {input_path}: {e}")
        return False

    data = img.getdata()
    new_data = []
    
    for item in data:
        r, g, b, a = item
        if bg_color == "white":
            # 滤除白色背景
            if r >= threshold and g >= threshold and b >= threshold:
                new_data.append((0, 0, 0, 0))
            else:
                new_data.append((r, g, b, a))
        else:
            new_data.append((r, g, b, a))
            
    img.putdata(new_data)
    
    # 裁剪边界
    bbox = img.getbbox()
    if bbox:
        cropped = img.crop(bbox)
        c_w, c_h = cropped.size
        max_side = max(c_w, c_h)
        square_img = Image.new("RGBA", (max_side, max_side), (0, 0, 0, 0))
        offset_x = (max_side - c_w) // 2
        offset_y = (max_side - c_h) // 2
        square_img.paste(cropped, (offset_x, offset_y))
    else:
        square_img = img

    # LANCZOS 高保真缩放到 16x16
    final_img = square_img.resize((target_size, target_size), resample=Image.Resampling.LANCZOS)
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    final_img.save(output_path, "PNG")
    print(f"Processed: {input_path} -> {output_path}")
    return True

# 路径定义
raw_under = r"C:\Users\12616\.gemini\antigravity\brain\8c35a454-20cd-4446-9d80-9a86944995e1\paint_bucket_under_raw_1783742454736.png"
raw_over = r"C:\Users\12616\.gemini\antigravity\brain\8c35a454-20cd-4446-9d80-9a86944995e1\paint_bucket_over_raw_1783742464754.png"

dest_under = r"f:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\item\paint_bucket_under.png"
dest_over = r"f:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\item\paint_bucket_over.png"
dest_block = r"f:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\block\color_block.png"

# 1. 处理油漆桶贴图
process_image(raw_under, dest_under, target_size=16)
process_image(raw_over, dest_over, target_size=16)

# 2. 生成纯白色方块贴图
pure_white_img = Image.new("RGBA", (16, 16), (255, 255, 255, 255))
os.makedirs(os.path.dirname(dest_block), exist_ok=True)
pure_white_img.save(dest_block, "PNG")
print(f"Generated pure white color_block: {dest_block}")
