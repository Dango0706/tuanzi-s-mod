import os
from PIL import Image

def process_armor_texture():
    src_path = r"f:\Development\Java\tuanzis_mods\scratch\assets\minecraft\textures\entity\equipment\humanoid\diamond.png"
    dest_dir = r"f:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\entity\equipment\humanoid"
    dest_path = os.path.join(dest_dir, "tidal_weave.png")

    if not os.path.exists(src_path):
        print(f"Error: diamond.png not found at {src_path}")
        return

    # 打开图像
    img = Image.open(src_path).convert("RGBA")
    width, height = img.size
    print(f"Original image size: {width}x{height}")

    # 我们要处理靴子部分。对于标准 64x32 图像，靴子在 X < 16, Y >= 16 区域。
    # 如果图像被放大了（例如 128x64 或更高分辨率），我们需要按比例缩放坐标阀值。
    scale_x = width / 64
    scale_y = height / 32

    x_threshold = int(16 * scale_x)
    y_threshold = int(16 * scale_y)

    pixels = img.load()

    # 将除靴子之外的区域（头盔、胸甲等）设为全透明
    for x in range(width):
        for y in range(height):
            if x >= x_threshold or y < y_threshold:
                pixels[x, y] = (0, 0, 0, 0)
            else:
                # 处理靴子区域的像素颜色
                r, g, b, a = pixels[x, y]
                if a > 0:
                    # 钻石原来是明亮的天蓝色（R 较小，G, B 较大）
                    # 潮汐织靴外观：海晶砂与幻翼膜编织，流淌着海洋之心的脉动光晕（青绿色、海蓝色，带有一些紫色和淡黄/粉红的微光）
                    # 我们可以通过以下公式转换颜色：
                    # 幻翼膜微黄偏灰，海晶砂是青绿/海蓝，海洋之心是明亮的蓝色与紫色脉动。
                    # 如果原像素偏暗偏灰色（如皮带/扣子部分），我们保留暗色调，但带有一点深蓝色。
                    # 如果原像素是明亮的蓝色（钻石主体），我们将其转为充满海洋之心和海晶石青绿色泽的颜色。
                    
                    # 检查是否为较暗的皮带/阴影（灰度值较低）
                    brightness = (r + g + b) / 3
                    if brightness < 80:
                        # 转换成深海蓝色
                        pixels[x, y] = (int(r * 0.4), int(g * 0.6), int(b * 0.9), a)
                    else:
                        # 转换成青绿与淡紫光晕
                        # 我们根据 x, y 坐标略微加入彩虹般的色偏，模拟海洋之心脉动光晕
                        wave = (x + y) % 6
                        if wave < 3:
                            # 偏青绿色（海晶石）
                            new_r = int(r * 0.1)
                            new_g = int(g * 0.8)
                            new_b = int(b * 0.7)
                        elif wave < 5:
                            # 偏海蓝色（海洋之心）
                            new_r = int(r * 0.2)
                            new_g = int(g * 0.6)
                            new_b = int(b * 0.95)
                        else:
                            # 偏流动的粉紫色光晕
                            new_r = int(r * 0.75)
                            new_g = int(g * 0.45)
                            new_b = int(b * 0.95)
                            
                        # 稍微加亮一些
                        new_r = min(255, int(new_r * 1.1))
                        new_g = min(255, int(new_g * 1.1))
                        new_b = min(255, int(new_b * 1.1))
                        
                        pixels[x, y] = (new_r, new_g, new_b, a)

    # 确保目标文件夹存在
    os.makedirs(dest_dir, exist_ok=True)
    img.save(dest_path)
    print(f"Processed texture saved successfully to {dest_path}")

if __name__ == "__main__":
    process_armor_texture()
