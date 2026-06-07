import os
from PIL import Image

def generate_texture():
    # 创建 32x32 的 RGBA 图像
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    pixels = img.load()

    # 1. 默认底色填充 (用金黄色/浅木色作为木纹的基底)
    wood_brown = (93, 64, 55, 255)   # #5d4037
    wood_dark = (78, 52, 46, 255)    # #4e342e
    wood_light = (109, 76, 65, 255)  # #6d4c41

    # 2. 绘制木纹材质区域 [0, 12] 到 [32, 16]
    for x in range(32):
        for y in range(12, 16):
            if (x + y) % 3 == 0:
                pixels[x, y] = wood_dark
            elif (x - y) % 4 == 0:
                pixels[x, y] = wood_light
            else:
                pixels[x, y] = wood_brown

    # 3. 绘制 head_bag (麻布袋头，大小 6x6x6，在 [0, 0] 到 [24, 12])
    bag_base = (215, 204, 200, 255)  # #d7ccc8
    bag_dark = (188, 170, 164, 255)  # #bcaaa4
    bag_light = (245, 245, 245, 255) # #f5f5f5
    stitch_grey = (141, 110, 99, 255) # #8d6e63

    for x in range(24):
        for y in range(12):
            if (x + y) % 5 == 0:
                pixels[x, y] = bag_dark
            elif (x - y) % 7 == 0:
                pixels[x, y] = bag_light
            else:
                pixels[x, y] = bag_base

    # 在 head_bag 的正面 (north面: 水平 [6, 12], 垂直 [6, 12] 区域) 绘制叉叉缝合眼和斜嘴
    # 叉叉左眼 (在 7,7 和 8,8)
    pixels[7, 7] = stitch_grey
    pixels[8, 8] = stitch_grey
    pixels[8, 7] = stitch_grey
    pixels[7, 8] = stitch_grey
    
    # 叉叉右眼 (在 10,7 和 11,8)
    pixels[10, 7] = stitch_grey
    pixels[11, 8] = stitch_grey
    pixels[11, 7] = stitch_grey
    pixels[10, 8] = stitch_grey

    # 缝合斜嘴 (在 8,10 到 11,10)
    pixels[8, 10] = stitch_grey
    pixels[9, 10] = stitch_grey
    pixels[10, 9] = stitch_grey
    pixels[11, 9] = stitch_grey

    # 4. 绘制 torso_padding (皮革躯干，大小 8x10x6，位于 [0, 16] 到 [28, 32])
    leather_base = (141, 110, 99, 255)  # #8d6e63
    leather_dark = (109, 76, 65, 255)   # #6d4c41
    leather_light = (161, 136, 127, 255) # #a1887f

    for x in range(28):
        for y in range(16, 32):
            if (x + y) % 4 == 0:
                pixels[x, y] = leather_dark
            elif (x - y) % 6 == 0:
                pixels[x, y] = leather_light
            else:
                pixels[x, y] = leather_base

    # 5. 绘制身体正面的打靶红心同心圆 (north面: 水平 [14, 22], 垂直 [22, 32])
    red_color = (211, 47, 47, 255)   # #d32f2f
    white_color = (255, 255, 255, 255) # #ffffff

    # 同心圆靶外环 (红色)
    for x in range(15, 21):
        for y in range(23, 31):
            # 形成一个圆环
            dx = abs(x - 17.5)
            dy = abs(y - 26.5)
            dist = dx*dx + dy*dy
            if dist <= 9.0:
                pixels[x, y] = red_color

    # 同心圆靶内环 (白色)
    for x in range(16, 20):
        for y in range(24, 30):
            dx = abs(x - 17.5)
            dy = abs(y - 26.5)
            dist = dx*dx + dy*dy
            if dist <= 4.5:
                pixels[x, y] = white_color

    # 靶心红点 (红色)
    pixels[17, 26] = red_color
    pixels[18, 26] = red_color
    pixels[17, 27] = red_color
    pixels[18, 27] = red_color

    # 6. 为假人的底座、脖绳绳结等加一点边缘缝线阴影，提高立体感
    for x in range(28):
        pixels[x, 16] = wood_dark  # 身体与木桩的接缝
        pixels[x, 31] = wood_dark  # 身体底部的接缝

    # 保存临时大材质图
    temp_dir = r'F:\Development\Java\tuanzis_mods\scratch'
    os.makedirs(temp_dir, exist_ok=True)
    temp_path = os.path.join(temp_dir, 'trial_dummy_temp.png')
    img.save(temp_path)
    print(f"Temporary 32x32 dummy texture generated at {temp_path}")

if __name__ == '__main__':
    generate_texture()
