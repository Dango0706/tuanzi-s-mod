"""
拼豆转蓝图工具自动化测试套件：
1. Litematica (.litematic) 二进制 NBT 导出与回读一致性测试（X,Y 与 X,Z 方向 + ColorBlock 原色 NBT）。
2. 真实拼豆图纸识别测试（含非均匀防盗拉伸红线识别 + 精确原色还原）。
3. 模糊/低清 JPEG 退化图纸鲁棒性测试。
4. 蓝图桌风格 3D 预览渲染器（等轴/平铺/单层/高亮）离屏渲染与视觉核验。
"""

from __future__ import annotations

import math
import tempfile
import unittest
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

from bead_parser import ParseOptions, parse_bead_image, render_grid_overlay, render_pixel_preview
from litematic_io import COLOR_BLOCK_BE_ID, COLOR_BLOCK_ID, export_litematic, read_nbt_gzip
from preview_window import render_blueprint_3d_image

SAMPLE_IMAGE = Path(
    r"C:\Users\Tuanzi\.gemini\antigravity\brain\f022a9a1-95ac-43aa-8f3e-9fefdad54bfc\.user_uploaded\media_1795617980640_fbd2f19e.jpg"
)
SCRATCH_DIR = Path(
    r"C:\Users\Tuanzi\.gemini\antigravity\brain\f022a9a1-95ac-43aa-8f3e-9fefdad54bfc\scratch"
)


def _unpack_litematica_states(long_arr: np.ndarray, total_blocks: int, palette_size: int) -> np.ndarray:
    bits = max(2, int(math.ceil(math.log2(palette_size))))
    mask = (1 << bits) - 1
    u64 = long_arr.view(np.uint64)
    out = np.zeros(total_blocks, dtype=np.int32)
    for i in range(total_blocks):
        bit_off = i * bits
        li = bit_off // 64
        sh = bit_off % 64
        val = int(u64[li]) >> sh
        if sh + bits > 64 and li + 1 < len(u64):
            val |= int(u64[li + 1]) << (64 - sh)
        out[i] = val & mask
    return out


class BeadBlueprintToolTests(unittest.TestCase):
    def test_litematic_xy_and_xz_roundtrip(self) -> None:
        # 构造 4x3 拼豆网格，含自定义原色与空气格
        grid_rgb = np.zeros((3, 4, 3), dtype=np.uint8)
        mask = np.zeros((3, 4), dtype=bool)
        pixels = {
            (0, 0): (0xFF, 0x11, 0x22),  # 顶左
            (0, 3): (0x12, 0x34, 0x56),  # 顶右
            (2, 1): (0xAA, 0x71, 0xC5),  # 底中
            (1, 2): (0xF8, 0xD0, 0x82),  # 中右
        }
        for (r, c), rgb in pixels.items():
            grid_rgb[r, c] = rgb
            mask[r, c] = True

        with tempfile.TemporaryDirectory() as tmp:
            # 1) 测试 X,Y 竖放
            xy_path = Path(tmp) / "test_xy.litematic"
            info_xy = export_litematic(xy_path, grid_rgb, mask, orientation="xy", blueprint_name="test_xy")
            self.assertEqual((info_xy["width"], info_xy["height"], info_xy["length"]), (4, 3, 1))
            self.assertEqual(info_xy["total_blocks"], 4)

            nbt_xy = read_nbt_gzip(xy_path)
            self.assertEqual(nbt_xy["Version"], 5)
            region = nbt_xy["Regions"]["Region"]
            self.assertEqual(region["Size"], {"x": 4, "y": 3, "z": 1})
            self.assertEqual(region["BlockStatePalette"][0]["Name"], "minecraft:air")
            self.assertEqual(region["BlockStatePalette"][1]["Name"], COLOR_BLOCK_ID)

            unpacked = _unpack_litematica_states(region["BlockStates"], 12, 2)
            # (row=0, col=0) -> x=0, y=2, z=0 -> idx = 2 * 4 * 1 + 0 * 4 + 0 = 8
            self.assertEqual(unpacked[8], 1)
            # (row=2, col=1) -> x=1, y=0, z=0 -> idx = 1
            self.assertEqual(unpacked[1], 1)
            self.assertEqual(int(unpacked.sum()), 4)

            te_map = {(te["x"], te["y"], te["z"]): te for te in region["TileEntities"]}
            self.assertEqual(len(te_map), 4)
            self.assertEqual(te_map[(0, 2, 0)]["id"], COLOR_BLOCK_BE_ID)
            self.assertEqual(te_map[(0, 2, 0)]["Color"], 0xFF1122)
            self.assertEqual(te_map[(1, 0, 0)]["Color"], 0xAA71C5)

            # 2) 测试 X,Z 平铺
            xz_path = Path(tmp) / "test_xz.litematic"
            info_xz = export_litematic(xz_path, grid_rgb, mask, orientation="xz", blueprint_name="test_xz")
            self.assertEqual((info_xz["width"], info_xz["height"], info_xz["length"]), (4, 1, 3))
            nbt_xz = read_nbt_gzip(xz_path)
            region_xz = nbt_xz["Regions"]["Region"]
            te_xz = {(te["x"], te["y"], te["z"]): te for te in region_xz["TileEntities"]}
            # (row=0, col=0) -> (0, 0, 0); (row=2, col=1) -> (1, 0, 2)
            self.assertEqual(te_xz[(0, 0, 0)]["Color"], 0xFF1122)
            self.assertEqual(te_xz[(1, 0, 2)]["Color"], 0xAA71C5)

    def test_real_sample_image_and_blur_resilience(self) -> None:
        if not SAMPLE_IMAGE.is_file():
            self.skipTest(f"未找到示例图纸: {SAMPLE_IMAGE}")

        # 1) 真实原图解析
        res = parse_bead_image(SAMPLE_IMAGE, ParseOptions(cells_per_major=10, merge_delta_e=14.5))
        self.assertEqual(res.method, "red_grid")
        self.assertEqual((res.cols, res.rows), (137, 160))
        self.assertGreater(res.total_blocks, 13000)
        self.assertLess(res.total_blocks, 15000)
        self.assertTrue(7 <= len(res.color_stats) <= 12)

        # 保存核验图与导出 .litematic 文件供 Java 解析验证
        SCRATCH_DIR.mkdir(parents=True, exist_ok=True)
        overlay = render_grid_overlay(res)
        cv2.imwrite(str(SCRATCH_DIR / "verify_overlay.png"), cv2.cvtColor(overlay, cv2.COLOR_RGB2BGR))

        pix = render_pixel_preview(res)
        pix_big = cv2.resize(pix, (res.cols * 4, res.rows * 4), interpolation=cv2.INTER_NEAREST)
        cv2.imwrite(str(SCRATCH_DIR / "verify_pixels.png"), cv2.cvtColor(pix_big, cv2.COLOR_RGB2BGR))

        # 渲染 3D 预览核验图（X,Y 竖放等轴 & X,Z 平铺等轴）
        vp_xy, _, _, _ = render_blueprint_3d_image(
            res.grid_rgb, res.mask, "xy", 900, 680, yaw=35.0, pitch=20.0, zoom=1.0, pan_x=0.0, pan_y=0.0,
            current_layer=res.rows - 1, single_layer_mode=False
        )
        cv2.imwrite(str(SCRATCH_DIR / "verify_3d_xy.png"), cv2.cvtColor(vp_xy, cv2.COLOR_RGB2BGR))

        vp_xz, _, _, _ = render_blueprint_3d_image(
            res.grid_rgb, res.mask, "xz", 900, 680, yaw=45.0, pitch=38.0, zoom=1.0, pan_x=0.0, pan_y=0.0,
            current_layer=0, single_layer_mode=False
        )
        cv2.imwrite(str(SCRATCH_DIR / "verify_3d_xz.png"), cv2.cvtColor(vp_xz, cv2.COLOR_RGB2BGR))

        # 导出真实 .litematic 样本
        export_litematic(SCRATCH_DIR / "raiden_xy.litematic", res.grid_rgb, res.mask, orientation="xy", blueprint_name="raiden_xy")
        export_litematic(SCRATCH_DIR / "raiden_xz.litematic", res.grid_rgb, res.mask, orientation="xz", blueprint_name="raiden_xz")

        # 2) 模糊 + 低质量 JPEG 退化测试（模拟模糊玩家截图）
        blurred = cv2.GaussianBlur(res.source_rgb, (3, 3), 0.9)
        ok, enc = cv2.imencode(".jpg", cv2.cvtColor(blurred, cv2.COLOR_RGB2BGR), [int(cv2.IMWRITE_JPEG_QUALITY), 45])
        self.assertTrue(ok)
        degraded = cv2.cvtColor(cv2.imdecode(enc, cv2.IMREAD_COLOR), cv2.COLOR_BGR2RGB)
        res_blur = parse_bead_image(degraded, ParseOptions(cells_per_major=10, merge_delta_e=16.0))
        self.assertGreater(res_blur.cols, 60)
        self.assertGreater(res_blur.rows, 60)
        self.assertGreater(res_blur.total_blocks, 5000)

        pix_blur = render_pixel_preview(res_blur)
        pix_blur_big = cv2.resize(pix_blur, (res_blur.cols * 4, res_blur.rows * 4), interpolation=cv2.INTER_NEAREST)
        cv2.imwrite(str(SCRATCH_DIR / "verify_blurred_pixels.png"), cv2.cvtColor(pix_blur_big, cv2.COLOR_RGB2BGR))

    def test_no_red_line_fallback(self) -> None:
        # 纯像素画（无红线），验证降级算法不崩溃且能还原大致网格与颜色
        img = np.full((120, 120, 3), 255, dtype=np.uint8)
        img[20:100, 20:100] = (40, 120, 220)
        img[40:80, 40:80] = (230, 180, 50)
        res = parse_bead_image(img, ParseOptions(manual_cols=10, manual_rows=10))
        self.assertEqual((res.cols, res.rows), (10, 10))
        self.assertGreater(res.total_blocks, 50)
        self.assertEqual(len(res.color_stats), 2)

    def test_white_beads_with_black_text_preserved_and_empty_air_kept(self) -> None:
        """
        验证带黑色色号文字的白色/浅米白拼豆格（含外轮廓暴露白格与少量内部高光白格）
        不会被误判为空气，也不会在默认 merge_delta_e=14.5 下被浅肤色吞并；
        同时无字的外部空白背景与内部镂空洞仍正确保持为空气。
        """
        nrows, ncols = 30, 30
        pad = 20

        def build_chart(cell_px: int, jpg_quality: int = 100) -> tuple[np.ndarray, dict[str, list[tuple[int, int]]]]:
            gw, gh = ncols * cell_px, nrows * cell_px
            W, H = gw + pad * 2, gh + pad * 2
            img = Image.new("RGB", (W, H), (255, 255, 255))
            draw = ImageDraw.Draw(img)

            font_size = max(5, int(round(cell_px * 0.44)))
            font = None
            for fname in ("C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/consola.ttf"):
                try:
                    font = ImageFont.truetype(fname, font_size)
                    break
                except Exception:
                    pass
            if font is None:
                font = ImageFont.load_default()

            def paint_cell(r: int, c: int, bg: tuple[int, int, int], text: str, fg: tuple[int, int, int]) -> None:
                x0, y0 = pad + c * cell_px, pad + r * cell_px
                draw.rectangle([x0, y0, x0 + cell_px - 1, y0 + cell_px - 1], fill=bg)
                if text:
                    bbox = draw.textbbox((0, 0), text, font=font)
                    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
                    tx = x0 + (cell_px - tw) / 2.0 - bbox[0]
                    ty = y0 + (cell_px - th) / 2.0 - bbox[1]
                    draw.text((tx, ty), text, fill=fg, font=font)

            groups: dict[str, list[tuple[int, int]]] = {
                "outer_white": [],
                "eye_white": [],
                "off_white": [],
                "skin": [],
                "black": [],
                "inner_hole": [],
                "outer_air": [],
            }

            # 1) 角色外框（黑色 H7，白字）：r in [6..23], c in [6..23] 的边框
            for r in range(6, 24):
                for c in range(6, 24):
                    if r in (6, 23) or c in (6, 23):
                        paint_cell(r, c, (16, 16, 16), "H7", (235, 235, 235))
                        groups["black"].append((r, c))

            # 2) 内部大面积浅肤色 (#FAEBE3，黑字 E11，与纯白 Lab ΔE 仅 ~8.8 < 14.5)
            for r in range(7, 23):
                for c in range(7, 23):
                    # 留出内部无字镂空洞 r in [16..20], c in [15..20]（带黑色内框 r=15/21, c=14/21）
                    if 15 <= r <= 21 and 14 <= c <= 21:
                        if r in (15, 21) or c in (14, 21):
                            paint_cell(r, c, (16, 16, 16), "H7", (235, 235, 235))
                            groups["black"].append((r, c))
                        else:
                            groups["inner_hole"].append((r, c))
                        continue
                    # 留出 4 格内部纯白高光（眼睛高光，数量极少）
                    if (r, c) in ((9, 9), (9, 10), (10, 9), (10, 10)):
                        paint_cell(r, c, (255, 255, 255), "H2", (15, 15, 15))
                        groups["eye_white"].append((r, c))
                        continue
                    # 留出一片浅灰白/珍珠白色块 (#E6E8EC, H01，同样落入 is_white_like 区间)
                    if 12 <= r <= 14 and 8 <= c <= 12:
                        paint_cell(r, c, (230, 232, 236), "H01", (15, 15, 15))
                        groups["off_white"].append((r, c))
                        continue
                    paint_cell(r, c, (250, 235, 227), "E11", (20, 20, 20))
                    groups["skin"].append((r, c))

            # 3) 暴露在角色外侧边缘的纯白拼豆（如白耳朵/白袖边），直接紧邻无字空白背景
            for r in range(3, 6):
                for c in range(8, 13):
                    paint_cell(r, c, (255, 255, 255), "H2", (15, 15, 15))
                    groups["outer_white"].append((r, c))

            # 记录部分外围纯白空白格（包含贴近黑框与贴近外侧白拼豆的空格）
            for r in range(nrows):
                for c in range(ncols):
                    if (
                        (r, c) not in groups["black"]
                        and (r, c) not in groups["skin"]
                        and (r, c) not in groups["eye_white"]
                        and (r, c) not in groups["off_white"]
                        and (r, c) not in groups["outer_white"]
                        and (r, c) not in groups["inner_hole"]
                    ):
                        groups["outer_air"].append((r, c))

            # 绘制 1px 细网格线、5 格中灰线与 10 格红线
            for c in range(ncols + 1):
                x = pad + c * cell_px
                col = (225, 60, 60) if c % 10 == 0 else ((175, 175, 175) if c % 5 == 0 else (225, 225, 225))
                draw.line([(x, pad), (x, pad + gh)], fill=col, width=1)
            for r in range(nrows + 1):
                y = pad + r * cell_px
                col = (225, 60, 60) if r % 10 == 0 else ((175, 175, 175) if r % 5 == 0 else (225, 225, 225))
                draw.line([(pad, y), (pad + gw, y)], fill=col, width=1)

            arr = np.array(img, dtype=np.uint8)
            if jpg_quality < 100:
                ok, enc = cv2.imencode(
                    ".jpg", cv2.cvtColor(arr, cv2.COLOR_RGB2BGR), [int(cv2.IMWRITE_JPEG_QUALITY), jpg_quality]
                )
                self.assertTrue(ok)
                arr = cv2.cvtColor(cv2.imdecode(enc, cv2.IMREAD_COLOR), cv2.COLOR_BGR2RGB)
            return arr, groups

        for cell_px, jq in ((9, 100), (24, 100), (24, 70)):
            arr, groups = build_chart(cell_px, jpg_quality=jq)
            res = parse_bead_image(
                arr,
                ParseOptions(cells_per_major=10, merge_delta_e=14.5, white_bg_mode="all_white"),
            )
            self.assertEqual(res.method, "red_grid", f"cell_px={cell_px}, jq={jq}")
            self.assertEqual((res.cols, res.rows), (ncols, nrows), f"cell_px={cell_px}, jq={jq}")

            # 1) 外轮廓暴露的带字白格必须全部保留为白色方块，而非空气或肤色
            for r, c in groups["outer_white"]:
                self.assertTrue(res.mask[r, c], f"外侧带字白格 ({r},{c}) 不应变为空气 (px={cell_px}, jq={jq})")
                rgb = res.grid_rgb[r, c]
                self.assertGreaterEqual(int(rgb.min()), 245, f"外侧白格颜色偏离纯白: {rgb}")

            # 2) 仅 4 格的内部纯白高光必须全部保留为纯白，不得被大面积浅肤色吞并
            for r, c in groups["eye_white"]:
                self.assertTrue(res.mask[r, c], f"内部高光白格 ({r},{c}) 不应变为空气 (px={cell_px}, jq={jq})")
                rgb = res.grid_rgb[r, c]
                self.assertGreaterEqual(int(rgb.min()), 245, f"内部高光白格被误合并成其他颜色: {rgb}")

            # 3) 浅灰白格 H01 (#E6E8EC) 必须保留且与浅肤色 E11、纯白 H2 各自独立还原原色
            skin_sample = tuple(int(v) for v in res.grid_rgb[groups["skin"][0]])
            white_sample = tuple(int(v) for v in res.grid_rgb[groups["outer_white"][0]])
            for r, c in groups["off_white"]:
                self.assertTrue(res.mask[r, c], f"浅灰白格 ({r},{c}) 不应变为空气 (px={cell_px}, jq={jq})")
                rgb = res.grid_rgb[r, c]
                rgb_t = tuple(int(v) for v in rgb)
                self.assertNotEqual(rgb_t, skin_sample, f"浅灰白格不应被浅肤色吞并: {rgb}")
                self.assertNotEqual(rgb_t, white_sample, f"浅灰白格不应被纯白吞并: {rgb}")
                self.assertTrue(218 <= int(rgb.mean()) <= 242, f"浅灰白格亮度异常: {rgb}")

            # 4) 浅肤色 E11 保持原本浅肤色（不被漂白成纯白）
            for r, c in groups["skin"]:
                self.assertTrue(res.mask[r, c])
                rgb = res.grid_rgb[r, c]
                self.assertGreater(int(rgb[0]) - int(rgb[2]), 12, f"浅肤色被误漂白: {rgb}")

            # 5) 内部无字镂空洞与外部无字白底必须全部保持为空气
            for r, c in groups["inner_hole"]:
                self.assertFalse(res.mask[r, c], f"内部无字镂空洞 ({r},{c}) 应为空气 (px={cell_px}, jq={jq})")
            for r, c in groups["outer_air"]:
                self.assertFalse(res.mask[r, c], f"外部无字空白 ({r},{c}) 应为空气 (px={cell_px}, jq={jq})")

            # 6) 验证导出 .litematic 时白色方块携带正确 Color NBT
            if cell_px == 24 and jq == 100:
                with tempfile.TemporaryDirectory() as tmp:
                    out_litematic = Path(tmp) / "white_beads_xy.litematic"
                    export_litematic(out_litematic, res.grid_rgb, res.mask, orientation="xy", blueprint_name="white_beads")
                    nbt = read_nbt_gzip(out_litematic)
                    te_map = {(te["x"], te["y"], te["z"]): te["Color"] for te in nbt["Regions"]["Region"]["TileEntities"]}
                    # 外侧白格 (r=4, c=10) -> xy: x=10, y=nrows-1-4=25, z=0
                    self.assertIn((10, nrows - 1 - 4, 0), te_map)
                    self.assertEqual(te_map[(10, nrows - 1 - 4, 0)], 0xFFFFFF)
                    # 内部无字洞 (r=17, c=17) -> 不在 TileEntities 中（空气）
                    self.assertNotIn((17, nrows - 1 - 17, 0), te_map)


if __name__ == "__main__":
    unittest.main(verbosity=2)
