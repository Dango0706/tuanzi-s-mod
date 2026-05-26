import sys
import argparse
from PIL import Image

def process_image(input_path, output_path, target_size=32, bg_color="white", threshold=240):
    try:
        # 1. 打开图片并转换为 RGBA
        img = Image.open(input_path).convert("RGBA")
    except Exception as e:
        print(f"错误: 无法打开输入图片 {input_path}。原因: {e}")
        return False

    # 2. 去除背景（透明化）
    data = img.getdata()
    new_data = []
    
    bg_color = bg_color.lower()
    for item in data:
        r, g, b, a = item
        if bg_color == "white":
            # 如果红绿蓝均大于等于阈值，视作白色/浅色背景，变为全透明
            if r >= threshold and g >= threshold and b >= threshold:
                new_data.append((0, 0, 0, 0))
            else:
                new_data.append((r, g, b, a))
        elif bg_color == "black":
            # 如果红绿蓝均小于等于阈值（此时阈值通常较小，如 15），视作黑色/深色背景，变为全透明
            if r <= threshold and g <= threshold and b <= threshold:
                new_data.append((0, 0, 0, 0))
            else:
                new_data.append((r, g, b, a))
        else:
            # 不处理背景
            new_data.append((r, g, b, a))
            
    img.putdata(new_data)
    
    # 3. 自动检测非透明内容的边界（Bounding Box），移除多余边缘留白
    bbox = img.getbbox()
    if not bbox:
        print("错误: 图片中未检测到任何有效的非透明实体内容！请调低阈值或检查背景颜色配置。")
        return False
    
    # 4. 裁剪出物体主体
    cropped = img.crop(bbox)
    c_w, c_h = cropped.size
    
    # 5. 居中填充为正方形画布，防止拉伸变形
    max_side = max(c_w, c_h)
    square_img = Image.new("RGBA", (max_side, max_side), (0, 0, 0, 0))
    offset_x = (max_side - c_w) // 2
    offset_y = (max_side - c_h) // 2
    square_img.paste(cropped, (offset_x, offset_y))
    
    # 6. 高保真缩放到指定尺寸
    try:
        resample_filter = Image.Resampling.LANCZOS
    except AttributeError:
        # 兼容旧版本 Pillow
        resample_filter = Image.ANTIALIAS
        
    final_img = square_img.resize((target_size, target_size), resample=resample_filter)
    
    # 7. 保存并输出
    try:
        final_img.save(output_path, "PNG")
        print(f"成功处理贴图: {input_path} -> {output_path} ({target_size}x{target_size}, 自动去 {bg_color} 背景并紧密裁剪)")
        return True
    except Exception as e:
        print(f"错误: 无法保存输出图片 {output_path}。原因: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(description="Minecraft 模组贴图高保真去背、去白边并等比例缩放工具 (Pillow)")
    parser.add_argument("input", help="输入图片的绝对路径或相对路径")
    parser.add_argument("output", help="处理后输出 PNG 贴图的路径")
    parser.add_argument("-s", "--size", type=int, default=32, help="目标缩小尺寸 (例如 16 或 32，默认 32)")
    parser.add_argument("-b", "--bg", choices=["white", "black", "none"], default="white", help="背景颜色模式: white(去白底，默认) / black(去黑底) / none(不去底)")
    parser.add_argument("-t", "--threshold", type=int, default=240, help="去除背景色判定阈值 (去白底默认 240，去黑底建议设为 15 左右)")
    
    args = parser.parse_args()
    
    # 自动微调去黑底的默认阈值
    threshold = args.threshold
    if args.bg == "black" and args.threshold == 240:
        threshold = 15
        
    process_image(args.input, args.output, args.size, args.bg, threshold)

if __name__ == "__main__":
    main()
