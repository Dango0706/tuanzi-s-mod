import os
import random
from PIL import Image

def load_image(path):
    return Image.open(path).convert("RGBA")

def save_image(img, path):
    img.save(path)
    print(f"Generated: {path}")

def generate_variants():
    base_dir = r"f:\Development\Java\tuanzis_mods\src\main\resources\assets\tuanzis_mod\textures\item"
    base_path = os.path.join(base_dir, "tide_cleaver_1.png")
    
    if not os.path.exists(base_path):
        print(f"Error: Base image {base_path} not found.")
        return

    # Load Stage 1
    img1 = load_image(base_path)
    width, height = img1.size
    
    # 1. Generate tide_cleaver_energy.png
    # Find the glowing Heart of the Sea core. Typically it's in the guard area (around 8,14 to 18,24)
    # and has high blue/teal component (B > 120, R < 160).
    energy_img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    energy_pixels = energy_img.load()
    base_pixels = img1.load()
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = base_pixels[x, y]
            if a > 0:
                # Target guard center: search for bright cyan/blue pixels
                # Typically Heart of the Sea is cyan/blue (g and b are high, r is moderate/low)
                if 6 <= x <= 18 and 12 <= y <= 24:
                    if b > 100 and g > 100 and r < 180:
                        energy_pixels[x, y] = (r, g, b, a)
                        
    save_image(energy_img, os.path.join(base_dir, "tide_cleaver_energy.png"))
    
    # Random seed for repeatable generation
    random.seed(42)
    
    # 2. Stage 2 (Slight Wear)
    img2 = img1.copy()
    pixels2 = img2.load()
    non_transparent = []
    for y in range(height):
        for x in range(width):
            if base_pixels[x, y][3] > 0:
                non_transparent.append((x, y))
                
    # Slightly dim colors (decrease saturation/brightness slightly)
    for x, y in non_transparent:
        r, g, b, a = pixels2[x, y]
        # Mix 90% original, 10% greyish blue
        pixels2[x, y] = (int(r * 0.9 + 20), int(g * 0.9 + 25), int(b * 0.9 + 30), a)
        
    # Remove 3 random outer edge pixels
    edge_pixels = get_edges(img2)
    to_remove = random.sample(edge_pixels, min(len(edge_pixels), 3))
    for x, y in to_remove:
        pixels2[x, y] = (0, 0, 0, 0)
    save_image(img2, os.path.join(base_dir, "tide_cleaver_2.png"))
    
    # 3. Stage 3 (Moderate Wear)
    img3 = img1.copy()
    pixels3 = img3.load()
    # Dim more towards grey-blue
    for x, y in non_transparent:
        r, g, b, a = pixels3[x, y]
        pixels3[x, y] = (int(r * 0.8 + 40), int(g * 0.8 + 45), int(b * 0.8 + 50), a)
        
    # Add minor internal scratches (grey lines)
    # We choose 3 random internal pixels and color them grey
    internal_pixels = [p for p in non_transparent if p not in get_edges(img3)]
    scratches = random.sample(internal_pixels, min(len(internal_pixels), 4))
    for x, y in scratches:
        pixels3[x, y] = (100, 110, 120, 255)
        
    # Remove 6 edge pixels
    edge_pixels = get_edges(img3)
    to_remove = random.sample(edge_pixels, min(len(edge_pixels), 6))
    for x, y in to_remove:
        pixels3[x, y] = (0, 0, 0, 0)
    save_image(img3, os.path.join(base_dir, "tide_cleaver_3.png"))
    
    # 4. Stage 4 (Heavy Wear)
    img4 = img1.copy()
    pixels4 = img4.load()
    # Color 30% blade pixels to dark iron color
    for x, y in non_transparent:
        r, g, b, a = pixels4[x, y]
        # Blade is typically pointing towards top-right (x > 15 or y < 15)
        if (x + y > 24) and random.random() < 0.4:
            # Dark steel color
            pixels4[x, y] = (70, 75, 80, a)
        else:
            # Dull color
            pixels4[x, y] = (int(r * 0.7 + 50), int(g * 0.7 + 55), int(b * 0.7 + 60), a)
            
    # Remove 12 edge pixels (chipped blade)
    edge_pixels = get_edges(img4)
    to_remove = random.sample(edge_pixels, min(len(edge_pixels), 12))
    for x, y in to_remove:
        pixels4[x, y] = (0, 0, 0, 0)
    save_image(img4, os.path.join(base_dir, "tide_cleaver_4.png"))
    
    # 5. Stage 5 (Broken Stage)
    img5 = img1.copy()
    pixels5 = img5.load()
    # Severe color decay
    for x, y in non_transparent:
        r, g, b, a = pixels5[x, y]
        # Make it heavily grey and dark
        pixels5[x, y] = (int(r * 0.5 + 40), int(g * 0.5 + 40), int(b * 0.5 + 45), a)
        
    # Structural fracture: cut a diagonal line near the middle of the blade
    # Middle of the blade is roughly x + y = 30 to 32
    for x, y in non_transparent:
        if 28 <= x + y <= 30:
            if random.random() < 0.8:
                # Delete pixel to create a fracture gap
                pixels5[x, y] = (0, 0, 0, 0)
            else:
                # Weak glowing teal dust connection
                pixels5[x, y] = (80, 180, 220, 150)
                
    # Remove more edge pixels
    edge_pixels = get_edges(img5)
    to_remove = random.sample(edge_pixels, min(len(edge_pixels), 18))
    for x, y in to_remove:
        pixels5[x, y] = (0, 0, 0, 0)
    save_image(img5, os.path.join(base_dir, "tide_cleaver_5.png"))

def get_edges(img):
    width, height = img.size
    pixels = img.load()
    edges = []
    for y in range(height):
        for x in range(width):
            if pixels[x, y][3] > 0:
                # Check neighbors
                is_edge = False
                for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                    nx, ny = x + dx, y + dy
                    if nx < 0 or nx >= width or ny < 0 or ny >= height or pixels[nx, ny][3] == 0:
                        is_edge = True
                        break
                if is_edge:
                    edges.append((x, y))
    return edges

if __name__ == "__main__":
    generate_variants()
