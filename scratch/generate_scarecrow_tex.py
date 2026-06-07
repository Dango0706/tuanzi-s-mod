import os
from PIL import Image, ImageDraw

def generate_texture():
    # 创建 32x32 的 RGBA 图像
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    pixels = img.load()

    # 1. 默认底色填充 (用金黄色作为草帽和草手默认色)
    # 草黄色
    straw_color = (241, 196, 15, 255) # #f1c40f
    straw_dark = (212, 172, 13, 255)  # #d4ac0d
    straw_light = (244, 208, 63, 255) # #f4d03f

    for x in range(32):
        for y in range(32):
            # 基础草噪声
            if (x + y) % 3 == 0:
                pixels[x, y] = straw_dark
            elif (x - y) % 4 == 0:
                pixels[x, y] = straw_light
            else:
                pixels[x, y] = straw_color

    # 2. 南瓜头部区域 [0, 0] 到 [32, 16]
    pumpkin_orange = (230, 126, 34, 255) # #e67e22
    pumpkin_dark = (211, 84, 0, 255)     # #d35400
    pumpkin_light = (243, 156, 18, 255)   # #f39c12

    for x in range(32):
        for y in range(16):
            if (x + y) % 4 == 0:
                pixels[x, y] = pumpkin_dark
            elif (x - y) % 5 == 0:
                pixels[x, y] = pumpkin_light
            else:
                pixels[x, y] = pumpkin_orange

    # 绘制经典的南瓜脸 (Carved Pumpkin Face) 在 [8, 8] 到 [15, 15] 区域
    face_black = (17, 17, 17, 255)
    face_glow = (243, 156, 18, 255)

    # 三角左眼
    pixels[9, 10] = face_black
    pixels[10, 10] = face_black
    pixels[9, 11] = face_black
    
    # 三角右眼
    pixels[13, 10] = face_black
    pixels[14, 10] = face_black
    pixels[14, 11] = face_black

    # 鼻子
    pixels[11, 12] = face_black
    pixels[12, 12] = face_black

    # 锯齿嘴巴
    pixels[9, 14] = face_black
    pixels[10, 14] = face_black
    pixels[11, 14] = face_black
    pixels[12, 14] = face_black
    pixels[13, 14] = face_black
    pixels[14, 14] = face_black
    pixels[10, 13] = face_black
    pixels[13, 13] = face_black
    pixels[8, 13] = face_black
    pixels[15, 13] = face_black

    # 3. 南瓜柄 [0, 16] 到 [8, 20]
    stem_green = (39, 174, 96, 255) # #27ae60
    stem_dark = (30, 132, 73, 255)  # #1e8449
    for x in range(8):
        for y in range(16, 20):
            if (x + y) % 2 == 0:
                pixels[x, y] = stem_dark
            else:
                pixels[x, y] = stem_green

    # 4. 木纹区域 [0, 20] 到 [20, 24] 以及 [0, 20] 到 [16, 32] (木桩和手臂)
    wood_brown = (92, 58, 33, 255)  # #5c3a21
    wood_dark = (74, 39, 17, 255)   # #4a2711
    wood_light = (120, 66, 18, 255) # #784212

    for x in range(20):
        for y in range(20, 24):
            if (x + y) % 3 == 0:
                pixels[x, y] = wood_dark
            elif (x - y) % 4 == 0:
                pixels[x, y] = wood_light
            else:
                pixels[x, y] = wood_brown

    for x in range(8):
        for y in range(20, 32):
            if (x + y) % 3 == 0:
                pixels[x, y] = wood_dark
            elif (x - y) % 4 == 0:
                pixels[x, y] = wood_light
            else:
                pixels[x, y] = wood_brown

    # 5. 干草块区域 [8, 16] 到 [32, 32]
    # 重新画干草块细节，加上经典的红色绑线
    red_rope = (192, 57, 43, 255) # #c0392b
    for x in range(8, 32):
        for y in range(16, 32):
            # 草的纹理
            if y == 20 or y == 28:
                pixels[x, y] = red_rope
            elif (x + y) % 3 == 0:
                pixels[x, y] = straw_dark
            elif (x - y) % 5 == 0:
                pixels[x, y] = straw_light
            else:
                pixels[x, y] = straw_color

    # 保存文件
    os.makedirs(os.path.dirname(r'F:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\entity'), exist_ok=True)
    img.save(r'F:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\entity\scarecrow.png')
    print("Texture generated successfully!")

if __name__ == '__main__':
    generate_texture()
