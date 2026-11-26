"""
蓝图桌风格 3D 预览窗口 (对齐 tuanzis_mod 的 BlueprintInspectorScreen)：
- 深色 Studio 主题 (#12131A 画布, #161822 顶/底栏, #181A26 可折叠右侧材料清单栏)
- 正交 3D 视口：左键拖拽旋转，右键/中键平移，滚轮缩放
- 3D 外框线 + X(红)/Y(绿)/Z(蓝) 标尺与标签 + 切层黄色高亮框
- 左下角 3D 方位罗盘 (东/南/上)，点击切换外框与 XYZ 显示
- 底栏切层控制器 (-/+ 与 累计/单层) + 5 预设视角 (0:俯视 1:正前 2:右侧 3:等轴 4:仰视)
- 右侧材料清单：支持搜索、排序(数量↓/色值↑)、单击高亮过滤、双击修改该原色 RGB
- 顶部支持直接切换 X,Y(竖放) / X,Z(平铺) 并一键导出 .litematic 蓝图文件
"""

from __future__ import annotations

import math
import tkinter as tk
from tkinter import colorchooser, filedialog, messagebox
from typing import Callable, Literal

import cv2
import numpy as np
from PIL import Image, ImageTk

from bead_parser import compute_color_stats
from litematic_io import export_litematic

# 与 BlueprintInspectorScreen 一致的配色常量
BG_CANVAS = "#12131A"
BG_BAR = "#161822"
BG_SIDEBAR = "#181A26"
BG_CARD = "#202334"
BG_CARD_HOVER = "#2A2F46"
BG_CARD_SEL = "#35301C"
BORDER_COLOR = "#2E3244"
ACCENT_CYAN = "#88EEFF"
ACCENT_YELLOW = "#FFEE55"
TEXT_MAIN = "#E8ECF4"
TEXT_MUTED = "#99A0B0"

TOP_BAR_H = 34
BOTTOM_BAR_H = 34
SIDEBAR_W = 268
COMPASS_CENTER_X = 44
COMPASS_HALF = 28


def project_3d_point(
    wx: float,
    wy: float,
    wz: float,
    cos_y: float,
    sin_y: float,
    cos_p: float,
    sin_p: float,
    scale: float,
    cx: float,
    cy: float,
) -> tuple[float, float, float]:
    """与 BlueprintPreviewMath#projectToScreen 完全一致的右手正交投影。"""
    rx = wx * cos_y - wz * sin_y
    rz = wx * sin_y + wz * cos_y
    ry = wy * cos_p - rz * sin_p
    depth = wy * sin_p + rz * cos_p
    return cx + rx * scale, cy - ry * scale, depth


def render_blueprint_3d_image(
    grid_rgb: np.ndarray,
    mask: np.ndarray,
    orientation: Literal["xy", "xz"],
    vp_w: int,
    vp_h: int,
    yaw: float,
    pitch: float,
    zoom: float,
    pan_x: float,
    pan_y: float,
    current_layer: int,
    single_layer_mode: bool,
    selected_color: int | None = None,
) -> tuple[np.ndarray, float, float, float]:
    """
    高速精确 3D 体素正交离屏渲染器（专为 1 格厚拼豆立面/平铺蓝图优化，15,000 方块仅需 ~5-15ms）。
    返回: (rgb_image, base_scale, center_x, center_y)
    """
    vp_w = max(64, int(vp_w))
    vp_h = max(64, int(vp_h))
    canvas = np.full((vp_h, vp_w, 3), (18, 19, 26), dtype=np.uint8)  # #12131A

    rows, cols = mask.shape
    if orientation == "xy":
        struct_w, struct_h, struct_l = cols, rows, 1
    else:
        struct_w, struct_h, struct_l = cols, 1, rows

    max_dim = max(1.0, float(max(struct_w, struct_h, struct_l)))
    base_scale = (min(vp_w, vp_h) * 0.68 / max_dim) * zoom
    cx = vp_w * 0.5 + pan_x
    cy = vp_h * 0.5 + pan_y

    rad_y = math.radians(yaw)
    rad_p = math.radians(pitch)
    cos_y, sin_y = math.cos(rad_y), math.sin(rad_y)
    cos_p, sin_p = math.cos(rad_p), math.sin(rad_p)

    # 相机观察方向（指向观察者）
    vz_x = sin_y * cos_p
    vz_y = sin_p
    vz_z = cos_y * cos_p

    # 应用切层过滤（沿 Y 轴）
    active_mask = mask.copy()
    if orientation == "xy":
        # row r 对应 y = (rows - 1) - r
        ys_row = (rows - 1) - np.arange(rows)
        if single_layer_mode:
            layer_ok = ys_row == current_layer
        else:
            layer_ok = ys_row <= current_layer
        active_mask &= layer_ok[:, None]
    else:
        # xz 平铺只有 y=0 一层
        if current_layer < 0 or (single_layer_mode and current_layer != 0):
            active_mask[:] = False

    if not active_mask.any():
        return canvas, base_scale, cx, cy

    # 颜色高亮过滤：未选中色块淡化为暗灰半透明效果，选中色块高亮全彩
    rgb_ints = (
        (grid_rgb[:, :, 0].astype(np.int32) << 16)
        | (grid_rgb[:, :, 1].astype(np.int32) << 8)
        | grid_rgb[:, :, 2].astype(np.int32)
    )

    def draw_pass(pass_mask: np.ndarray, dim_factor: float) -> None:
        if not pass_mask.any():
            return
        hw = struct_w * 0.5
        hh = struct_h * 0.5
        hl = struct_l * 0.5

        def proj(wx: float, wy: float, wz: float) -> tuple[float, float]:
            sx, sy, _ = project_3d_point(
                wx - hw, wy - hh, wz - hl, cos_y, sin_y, cos_p, sin_p, base_scale, cx, cy
            )
            return sx, sy

        # 计算单位基向量在屏幕空间的投影偏移
        o_x, o_y = proj(0.0, 0.0, 0.0)
        ex_x, ex_y = proj(1.0, 0.0, 0.0)
        ey_x, ey_y = proj(0.0, 1.0, 0.0)
        ez_x, ez_y = proj(0.0, 0.0, 1.0)
        dx = (ex_x - o_x, ex_y - o_y)
        dy = (ey_x - o_x, ey_y - o_y)
        dz = (ez_x - o_x, ez_y - o_y)

        # 1. 绘制暴露在外的侧壁厚度面（严格在主平面后方，仅对边缘有空邻格的方块绘制）
        side_quads: list[tuple[float, np.ndarray, tuple[int, int, int]]] = []

        if orientation == "xy":
            # 侧壁为 ±X 面与 ±Y 面
            show_pos_x = vz_x >= 0.0
            show_pos_y = vz_y >= 0.0
            # X 方向暴露边
            if show_pos_x:
                exp_x = pass_mask.copy()
                exp_x[:, :-1] &= ~pass_mask[:, 1:]
                x_off = 1.0
            else:
                exp_x = pass_mask.copy()
                exp_x[:, 1:] &= ~pass_mask[:, :-1]
                x_off = 0.0
            # Y 方向暴露边（注意 row=0 是顶部 +Y，row=rows-1 是底部 -Y）
            if show_pos_y:
                exp_y = pass_mask.copy()
                exp_y[1:, :] &= ~pass_mask[:-1, :]
                y_off = 1.0
                shade_y = 1.0
            else:
                exp_y = pass_mask.copy()
                exp_y[:-1, :] &= ~pass_mask[1:, :]
                y_off = 0.0
                shade_y = 0.58

            shade_x = 0.72
            if abs(vz_x) > 1e-3:
                rs, cs = np.nonzero(exp_x)
                for r, c in zip(rs.tolist(), cs.tolist()):
                    y0 = (rows - 1) - r
                    col_rgb = grid_rgb[r, c]
                    cr = int(np.clip(col_rgb[0] * shade_x * dim_factor + (1.0 - dim_factor) * 18, 0, 255))
                    cg = int(np.clip(col_rgb[1] * shade_x * dim_factor + (1.0 - dim_factor) * 19, 0, 255))
                    cb = int(np.clip(col_rgb[2] * shade_x * dim_factor + (1.0 - dim_factor) * 26, 0, 255))
                    p0x = o_x + (c + x_off) * dx[0] + y0 * dy[0]
                    p0y = o_y + (c + x_off) * dx[1] + y0 * dy[1]
                    pts = np.array(
                        [
                            [p0x, p0y],
                            [p0x + dy[0], p0y + dy[1]],
                            [p0x + dy[0] + dz[0], p0y + dy[1] + dz[1]],
                            [p0x + dz[0], p0y + dz[1]],
                        ],
                        dtype=np.int32,
                    )
                    _, _, dp = project_3d_point(
                        c + x_off - hw, y0 + 0.5 - hh, 0.5 - hl, cos_y, sin_y, cos_p, sin_p, base_scale, cx, cy
                    )
                    side_quads.append((dp, pts, (cr, cg, cb)))

            if abs(vz_y) > 1e-3:
                rs, cs = np.nonzero(exp_y)
                for r, c in zip(rs.tolist(), cs.tolist()):
                    y0 = (rows - 1) - r
                    col_rgb = grid_rgb[r, c]
                    cr = int(np.clip(col_rgb[0] * shade_y * dim_factor + (1.0 - dim_factor) * 18, 0, 255))
                    cg = int(np.clip(col_rgb[1] * shade_y * dim_factor + (1.0 - dim_factor) * 19, 0, 255))
                    cb = int(np.clip(col_rgb[2] * shade_y * dim_factor + (1.0 - dim_factor) * 26, 0, 255))
                    p0x = o_x + c * dx[0] + (y0 + y_off) * dy[0]
                    p0y = o_y + c * dx[1] + (y0 + y_off) * dy[1]
                    pts = np.array(
                        [
                            [p0x, p0y],
                            [p0x + dx[0], p0y + dx[1]],
                            [p0x + dx[0] + dz[0], p0y + dx[1] + dz[1]],
                            [p0x + dz[0], p0y + dz[1]],
                        ],
                        dtype=np.int32,
                    )
                    _, _, dp = project_3d_point(
                        c + 0.5 - hw, y0 + y_off - hh, 0.5 - hl, cos_y, sin_y, cos_p, sin_p, base_scale, cx, cy
                    )
                    side_quads.append((dp, pts, (cr, cg, cb)))
        else:
            # 平铺模式 (xz)：侧壁为 ±X 面与 ±Z 面
            show_pos_x = vz_x >= 0.0
            show_pos_z = vz_z >= 0.0
            if show_pos_x:
                exp_x = pass_mask.copy()
                exp_x[:, :-1] &= ~pass_mask[:, 1:]
                x_off = 1.0
            else:
                exp_x = pass_mask.copy()
                exp_x[:, 1:] &= ~pass_mask[:, :-1]
                x_off = 0.0
            if show_pos_z:
                exp_z = pass_mask.copy()
                exp_z[:-1, :] &= ~pass_mask[1:, :]
                z_off = 1.0
            else:
                exp_z = pass_mask.copy()
                exp_z[1:, :] &= ~pass_mask[:-1, :]
                z_off = 0.0

            shade_x, shade_z = 0.72, 0.84
            if abs(vz_x) > 1e-3:
                rs, cs = np.nonzero(exp_x)
                for r, c in zip(rs.tolist(), cs.tolist()):
                    col_rgb = grid_rgb[r, c]
                    cr = int(np.clip(col_rgb[0] * shade_x * dim_factor + (1.0 - dim_factor) * 18, 0, 255))
                    cg = int(np.clip(col_rgb[1] * shade_x * dim_factor + (1.0 - dim_factor) * 19, 0, 255))
                    cb = int(np.clip(col_rgb[2] * shade_x * dim_factor + (1.0 - dim_factor) * 26, 0, 255))
                    p0x = o_x + (c + x_off) * dx[0] + r * dz[0]
                    p0y = o_y + (c + x_off) * dx[1] + r * dz[1]
                    pts = np.array(
                        [
                            [p0x, p0y],
                            [p0x + dy[0], p0y + dy[1]],
                            [p0x + dy[0] + dz[0], p0y + dy[1] + dz[1]],
                            [p0x + dz[0], p0y + dz[1]],
                        ],
                        dtype=np.int32,
                    )
                    _, _, dp = project_3d_point(
                        c + x_off - hw, 0.5 - hh, r + 0.5 - hl, cos_y, sin_y, cos_p, sin_p, base_scale, cx, cy
                    )
                    side_quads.append((dp, pts, (cr, cg, cb)))

            if abs(vz_z) > 1e-3:
                rs, cs = np.nonzero(exp_z)
                for r, c in zip(rs.tolist(), cs.tolist()):
                    col_rgb = grid_rgb[r, c]
                    cr = int(np.clip(col_rgb[0] * shade_z * dim_factor + (1.0 - dim_factor) * 18, 0, 255))
                    cg = int(np.clip(col_rgb[1] * shade_z * dim_factor + (1.0 - dim_factor) * 19, 0, 255))
                    cb = int(np.clip(col_rgb[2] * shade_z * dim_factor + (1.0 - dim_factor) * 26, 0, 255))
                    p0x = o_x + c * dx[0] + (r + z_off) * dz[0]
                    p0y = o_y + c * dx[1] + (r + z_off) * dz[1]
                    pts = np.array(
                        [
                            [p0x, p0y],
                            [p0x + dx[0], p0y + dx[1]],
                            [p0x + dx[0] + dy[0], p0y + dx[1] + dy[1]],
                            [p0x + dy[0], p0y + dy[1]],
                        ],
                        dtype=np.int32,
                    )
                    _, _, dp = project_3d_point(
                        c + 0.5 - hw, 0.5 - hh, r + z_off - hl, cos_y, sin_y, cos_p, sin_p, base_scale, cx, cy
                    )
                    side_quads.append((dp, pts, (cr, cg, cb)))

        if side_quads:
            side_quads.sort(key=lambda item: item[0])
            for _, pts, col_t in side_quads:
                cv2.fillConvexPoly(canvas, pts, col_t)

        # 2. 通过精确仿射变换绘制主平面（所有面严格位于侧壁前方，0.8ms 完成全量绘制）
        if orientation == "xy":
            normal_dot = vz_z
            if abs(normal_dot) > 1e-3:
                z_plane = 1.0 if normal_dot > 0 else 0.0
                shade_main = 0.90 if normal_dot > 0 else 0.80
                # 图坐标 (0,0) -> 世界 (0, rows, z_plane); (cols,0) -> (cols, rows, z_plane); (0,rows) -> (0, 0, z_plane)
                p_tl = proj(0.0, float(rows), z_plane)
                p_tr = proj(float(cols), float(rows), z_plane)
                p_bl = proj(0.0, 0.0, z_plane)
                _warp_plane(canvas, grid_rgb, pass_mask, p_tl, p_tr, p_bl, shade_main, dim_factor, base_scale)
        else:
            normal_dot = vz_y
            if abs(normal_dot) > 1e-3:
                y_plane = 1.0 if normal_dot > 0 else 0.0
                shade_main = 1.00 if normal_dot > 0 else 0.62
                # 图坐标 (0,0) -> 世界 (0, y_plane, 0); (cols,0) -> (cols, y_plane, 0); (0,rows) -> (0, y_plane, rows)
                p_tl = proj(0.0, y_plane, 0.0)
                p_tr = proj(float(cols), y_plane, 0.0)
                p_bl = proj(0.0, y_plane, float(rows))
                _warp_plane(canvas, grid_rgb, pass_mask, p_tl, p_tr, p_bl, shade_main, dim_factor, base_scale)

    if selected_color is not None:
        sel_mask = active_mask & (rgb_ints == selected_color)
        other_mask = active_mask & (~sel_mask)
        draw_pass(other_mask, dim_factor=0.22)
        draw_pass(sel_mask, dim_factor=1.0)
    else:
        draw_pass(active_mask, dim_factor=1.0)

    return canvas, base_scale, cx, cy


def _warp_plane(
    canvas: np.ndarray,
    grid_rgb: np.ndarray,
    plane_mask: np.ndarray,
    p_tl: tuple[float, float],
    p_tr: tuple[float, float],
    p_bl: tuple[float, float],
    shade: float,
    dim_factor: float,
    base_scale: float,
) -> None:
    rows, cols = plane_mask.shape
    vp_h, vp_w = canvas.shape[:2]

    src_pts = np.array([[0.0, 0.0], [float(cols), 0.0], [0.0, float(rows)]], dtype=np.float32)
    dst_pts = np.array([p_tl, p_tr, p_bl], dtype=np.float32)
    mat = cv2.getAffineTransform(src_pts, dst_pts)

    shaded = np.clip(
        grid_rgb.astype(np.float32) * (shade * dim_factor) + (1.0 - dim_factor) * np.array([18.0, 19.0, 26.0]),
        0,
        255,
    ).astype(np.uint8)

    warped_rgb = cv2.warpAffine(
        shaded, mat, (vp_w, vp_h), flags=cv2.INTER_NEAREST, borderMode=cv2.BORDER_CONSTANT, borderValue=(0, 0, 0)
    )
    warped_mask = cv2.warpAffine(
        plane_mask.astype(np.uint8) * 255,
        mat,
        (vp_w, vp_h),
        flags=cv2.INTER_NEAREST,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=0,
    )
    hit = warped_mask > 127
    canvas[hit] = warped_rgb[hit]

    # 放大到足够清晰时，给每个彩色方块边缘绘制极淡的微缝增强立体颗粒感
    if base_scale >= 7.0 and hit.any():
        # 用逆仿射检查像素距离格线的比例
        inv_mat = cv2.invertAffineTransform(mat)
        ys_h, xs_h = np.nonzero(hit)
        u = inv_mat[0, 0] * xs_h + inv_mat[0, 1] * ys_h + inv_mat[0, 2]
        v = inv_mat[1, 0] * xs_h + inv_mat[1, 1] * ys_h + inv_mat[1, 2]
        fu = u - np.floor(u)
        fv = v - np.floor(v)
        edge = (fu < 0.06) | (fv < 0.06)
        if edge.any():
            ey = ys_h[edge]
            ex = xs_h[edge]
            canvas[ey, ex] = (canvas[ey, ex].astype(np.float32) * 0.88).astype(np.uint8)


class BlueprintPreviewWindow(tk.Toplevel):
    """与蓝图桌 BlueprintInspectorScreen 一致的 3D 预览与导出窗口。"""

    def __init__(
        self,
        master: tk.Misc,
        grid_rgb: np.ndarray,
        mask: np.ndarray,
        orientation: Literal["xy", "xz"],
        title_name: str,
        default_export_dir: str = "",
        on_orientation_change: Callable[[Literal["xy", "xz"]], None] | None = None,
        on_grid_color_change: Callable[[np.ndarray], None] | None = None,
    ) -> None:
        super().__init__(master)
        self.title(f"蓝图桌 3D 预览 - {title_name}")
        self.geometry("1180x760")
        self.minsize(820, 540)
        self.configure(bg=BG_CANVAS)

        self.grid_rgb = grid_rgb.copy()
        self.mask = mask.copy()
        self.orientation: Literal["xy", "xz"] = orientation
        self.title_name = title_name
        self.default_export_dir = default_export_dir
        self.on_orientation_change = on_orientation_change
        self.on_grid_color_change = on_grid_color_change

        # 相机状态（与 BlueprintInspectorScreen 默认一致：等轴 45°, 30°）
        self.yaw = 45.0
        self.pitch = 30.0
        self.zoom = 1.0
        self.pan_x = 0.0
        self.pan_y = 0.0
        self.show_bounds_axes = True
        self.single_layer_mode = False
        self.current_layer = self._max_layer()
        self.sidebar_collapsed = False
        self.sort_by_count = True
        self.selected_color: int | None = None

        self._drag_mode: Literal["none", "orbit", "pan"] = "none"
        self._last_mx = 0
        self._last_my = 0
        self._photo_ref: ImageTk.PhotoImage | None = None
        self._all_stats: list[tuple[int, int]] = []
        self._filtered_stats: list[tuple[int, int]] = []
        self._render_pending = False

        self._build_ui()
        self._refresh_stats()
        self._bind_events()
        self.after(30, self._schedule_render)

    def _struct_dims(self) -> tuple[int, int, int]:
        rows, cols = self.mask.shape
        return (cols, rows, 1) if self.orientation == "xy" else (cols, 1, rows)

    def _max_layer(self) -> int:
        _, h, _ = self._struct_dims()
        return max(0, h - 1)

    def _build_ui(self) -> None:
        # 顶栏
        self.top_bar = tk.Frame(self, bg=BG_BAR, height=TOP_BAR_H, highlightthickness=1, highlightbackground=BORDER_COLOR)
        self.top_bar.pack(side=tk.TOP, fill=tk.X)
        self.top_bar.pack_propagate(False)

        self.btn_export = tk.Button(
            self.top_bar,
            text="导出蓝图 (.litematic)",
            command=self._on_export,
            bg="#265C3B",
            fg="#FFFFFF",
            activebackground="#327A4E",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=10,
            font=("Microsoft YaHei UI", 9, "bold"),
            cursor="hand2",
        )
        self.btn_export.pack(side=tk.LEFT, padx=(8, 4), pady=4)

        self.btn_orient = tk.Button(
            self.top_bar,
            text=self._orient_btn_text(),
            command=self._toggle_orientation,
            bg="#273452",
            fg=ACCENT_CYAN,
            activebackground="#33456B",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=8,
            font=("Microsoft YaHei UI", 9),
            cursor="hand2",
        )
        self.btn_orient.pack(side=tk.LEFT, padx=4, pady=4)

        self.btn_close = tk.Button(
            self.top_bar,
            text="返回",
            command=self.destroy,
            bg="#2B2E40",
            fg=TEXT_MAIN,
            activebackground="#3A3F56",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=8,
            font=("Microsoft YaHei UI", 9),
            cursor="hand2",
        )
        self.btn_close.pack(side=tk.LEFT, padx=4, pady=4)

        self.lbl_title = tk.Label(
            self.top_bar,
            text=self.title_name,
            bg=BG_BAR,
            fg=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 10, "bold"),
        )
        self.lbl_title.pack(side=tk.LEFT, padx=12)

        self.btn_sidebar = tk.Button(
            self.top_bar,
            text="≡",
            command=self._toggle_sidebar,
            bg="#2B2E40",
            fg=TEXT_MAIN,
            activebackground="#3A3F56",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            width=3,
            font=("Microsoft YaHei UI", 10, "bold"),
            cursor="hand2",
        )
        self.btn_sidebar.pack(side=tk.RIGHT, padx=6, pady=4)

        # 中间主容器（左 3D 视口 + 右侧可折叠材料清单）
        self.body = tk.Frame(self, bg=BG_CANVAS)
        self.body.pack(side=tk.TOP, fill=tk.BOTH, expand=True)

        # 左侧视口+底栏容器
        self.left_pane = tk.Frame(self.body, bg=BG_CANVAS)
        self.left_pane.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(self.left_pane, bg=BG_CANVAS, highlightthickness=0, cursor="fleur")
        self.canvas.pack(side=tk.TOP, fill=tk.BOTH, expand=True)

        # 底栏（切层 + 模式 + 5 预设视角 + 提示）
        self.bottom_bar = tk.Frame(
            self.left_pane, bg=BG_BAR, height=BOTTOM_BAR_H, highlightthickness=1, highlightbackground=BORDER_COLOR
        )
        self.bottom_bar.pack(side=tk.BOTTOM, fill=tk.X)
        self.bottom_bar.pack_propagate(False)

        btn_cfg = dict(
            bg="#262A3C",
            fg=TEXT_MAIN,
            activebackground="#373D57",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            font=("Microsoft YaHei UI", 9),
            cursor="hand2",
        )

        tk.Button(self.bottom_bar, text="-", width=2, command=lambda: self._step_layer(-1), **btn_cfg).pack(
            side=tk.LEFT, padx=(6, 2), pady=4
        )
        self.lbl_layer = tk.Label(
            self.bottom_bar, text="1/1", bg=BG_BAR, fg=ACCENT_YELLOW, width=8, font=("Consolas", 9, "bold")
        )
        self.lbl_layer.pack(side=tk.LEFT, padx=2)
        tk.Button(self.bottom_bar, text="+", width=2, command=lambda: self._step_layer(1), **btn_cfg).pack(
            side=tk.LEFT, padx=(2, 6), pady=4
        )

        self.btn_mode = tk.Button(self.bottom_bar, text="累计模式", width=8, command=self._toggle_layer_mode, **btn_cfg)
        self.btn_mode.pack(side=tk.LEFT, padx=(0, 8), pady=4)

        preset_names = ["0:俯视", "1:正前", "2:右侧", "3:等轴", "4:仰视"]
        for idx, pname in enumerate(preset_names):
            tk.Button(
                self.bottom_bar, text=pname, padx=6, command=lambda i=idx: self._apply_preset(i), **btn_cfg
            ).pack(side=tk.LEFT, padx=2, pady=4)

        tk.Label(
            self.bottom_bar,
            text="左键:旋转  右键:平移  滚轮:缩放  [/]:切层  L:单层  B:清单",
            bg=BG_BAR,
            fg=TEXT_MUTED,
            font=("Microsoft YaHei UI", 8),
        ).pack(side=tk.RIGHT, padx=8)

        # 右侧材料清单栏
        self.sidebar = tk.Frame(
            self.body, bg=BG_SIDEBAR, width=SIDEBAR_W, highlightthickness=1, highlightbackground=BORDER_COLOR
        )
        self.sidebar.pack(side=tk.RIGHT, fill=tk.Y)
        self.sidebar.pack_propagate(False)

        self.lbl_sb_header = tk.Label(
            self.sidebar,
            text="彩色方块原色清单",
            bg=BG_SIDEBAR,
            fg=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 10, "bold"),
            anchor="w",
        )
        self.lbl_sb_header.pack(fill=tk.X, padx=10, pady=(8, 2))

        self.lbl_sb_sub = tk.Label(
            self.sidebar,
            text="单击高亮色块 · 双击微调原色",
            bg=BG_SIDEBAR,
            fg=TEXT_MUTED,
            font=("Microsoft YaHei UI", 8),
            anchor="w",
        )
        self.lbl_sb_sub.pack(fill=tk.X, padx=10, pady=(0, 6))

        self.search_var = tk.StringVar()
        self.search_var.trace_add("write", lambda *_: self._filter_stats())
        self.search_entry = tk.Entry(
            self.sidebar,
            textvariable=self.search_var,
            bg="#10121A",
            fg=TEXT_MAIN,
            insertbackground=TEXT_MAIN,
            relief=tk.FLAT,
            highlightthickness=1,
            highlightbackground=BORDER_COLOR,
            font=("Consolas", 9),
        )
        self.search_entry.pack(fill=tk.X, padx=10, pady=(0, 6), ipady=3)

        sb_btns = tk.Frame(self.sidebar, bg=BG_SIDEBAR)
        sb_btns.pack(fill=tk.X, padx=10, pady=(0, 6))

        self.btn_sort = tk.Button(sb_btns, text="排序:数量↓", command=self._toggle_sort, **btn_cfg)
        self.btn_sort.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 4))

        tk.Button(sb_btns, text="重置视角", command=self._reset_camera, **btn_cfg).pack(
            side=tk.RIGHT, fill=tk.X, expand=True, padx=(4, 0)
        )

        # 可滚动色块列表
        list_wrap = tk.Frame(self.sidebar, bg=BG_SIDEBAR)
        list_wrap.pack(fill=tk.BOTH, expand=True, padx=6, pady=(0, 8))

        self.sb_canvas = tk.Canvas(list_wrap, bg=BG_SIDEBAR, highlightthickness=0)
        self.sb_scroll = tk.Scrollbar(list_wrap, orient=tk.VERTICAL, command=self.sb_canvas.yview)
        self.sb_inner = tk.Frame(self.sb_canvas, bg=BG_SIDEBAR)
        self.sb_inner.bind("<Configure>", lambda e: self.sb_canvas.configure(scrollregion=self.sb_canvas.bbox("all")))
        self.sb_win = self.sb_canvas.create_window((0, 0), window=self.sb_inner, anchor="nw")
        self.sb_canvas.bind("<Configure>", lambda e: self.sb_canvas.itemconfigure(self.sb_win, width=e.width))
        self.sb_canvas.configure(yscrollcommand=self.sb_scroll.set)

        self.sb_canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.sb_scroll.pack(side=tk.RIGHT, fill=tk.Y)

    def _orient_btn_text(self) -> str:
        return "方向: X,Y 竖放" if self.orientation == "xy" else "方向: X,Z 平铺"

    def _bind_events(self) -> None:
        self.canvas.bind("<Configure>", lambda _e: self._schedule_render())
        self.canvas.bind("<ButtonPress-1>", self._on_left_down)
        self.canvas.bind("<B1-Motion>", self._on_drag)
        self.canvas.bind("<ButtonRelease-1>", self._on_mouse_up)
        self.canvas.bind("<ButtonPress-2>", self._on_pan_down)
        self.canvas.bind("<B2-Motion>", self._on_drag)
        self.canvas.bind("<ButtonRelease-2>", self._on_mouse_up)
        self.canvas.bind("<ButtonPress-3>", self._on_pan_down)
        self.canvas.bind("<B3-Motion>", self._on_drag)
        self.canvas.bind("<ButtonRelease-3>", self._on_mouse_up)
        self.canvas.bind("<MouseWheel>", self._on_wheel)
        self.sb_canvas.bind(
            "<MouseWheel>", lambda e: self.sb_canvas.yview_scroll(int(-1 * (e.delta / 120)), "units")
        )
        self.bind("<KeyPress>", self._on_key_press)

    def _on_key_press(self, event: tk.Event) -> None:
        if self.focus_get() == self.search_entry:
            if event.keysym == "Escape":
                self.canvas.focus_set()
            return

        ks = event.keysym
        ch = event.char.lower() if event.char else ""
        if ks == "Escape":
            self.destroy()
        elif ch == "b":
            self._toggle_sidebar()
        elif ch == "l":
            self._toggle_layer_mode()
        elif ch == "r":
            self._reset_camera()
        elif ch == "[" or ks == "Down":
            self._step_layer(-1)
        elif ch == "]" or ks == "Up":
            self._step_layer(1)
        elif ks == "Prior":
            self._step_layer(5)
        elif ks == "Next":
            self._step_layer(-5)
        elif ks == "Home":
            self.current_layer = 0
            self._schedule_render()
        elif ks == "End":
            self.current_layer = self._max_layer()
            self._schedule_render()
        elif ch in ("0", "1", "2", "3", "4"):
            self._apply_preset(int(ch))

    def _on_left_down(self, event: tk.Event) -> None:
        self.canvas.focus_set()
        # 检查是否点击了左下角方位罗盘
        ch = self.canvas.winfo_height()
        comp_y = ch - 38
        if (
            COMPASS_CENTER_X - COMPASS_HALF <= event.x <= COMPASS_CENTER_X + COMPASS_HALF
            and comp_y - COMPASS_HALF <= event.y <= comp_y + COMPASS_HALF
        ):
            self.show_bounds_axes = not self.show_bounds_axes
            self._schedule_render()
            return
        self._drag_mode = "orbit"
        self._last_mx, self._last_my = event.x, event.y

    def _on_pan_down(self, event: tk.Event) -> None:
        self.canvas.focus_set()
        self._drag_mode = "pan"
        self._last_mx, self._last_my = event.x, event.y

    def _on_drag(self, event: tk.Event) -> None:
        dx = event.x - self._last_mx
        dy = event.y - self._last_my
        self._last_mx, self._last_my = event.x, event.y
        if self._drag_mode == "orbit":
            self.yaw = (self.yaw - dx * 0.55) % 360.0
            self.pitch = max(-89.0, min(89.0, self.pitch + dy * 0.55))
            self._schedule_render()
        elif self._drag_mode == "pan":
            self.pan_x += dx
            self.pan_y += dy
            self._schedule_render()

    def _on_mouse_up(self, _event: tk.Event) -> None:
        self._drag_mode = "none"

    def _on_wheel(self, event: tk.Event) -> None:
        factor = 1.15 if event.delta > 0 else (1.0 / 1.15)
        self.zoom = max(0.15, min(10.0, self.zoom * factor))
        self._schedule_render()

    def _toggle_orientation(self) -> None:
        self.orientation = "xz" if self.orientation == "xy" else "xy"
        self.btn_orient.configure(text=self._orient_btn_text())
        self.current_layer = self._max_layer()
        if self.on_orientation_change:
            self.on_orientation_change(self.orientation)
        self._schedule_render()

    def _toggle_sidebar(self) -> None:
        self.sidebar_collapsed = not self.sidebar_collapsed
        if self.sidebar_collapsed:
            self.sidebar.pack_forget()
        else:
            self.sidebar.pack(side=tk.RIGHT, fill=tk.Y)
        self._schedule_render()

    def _toggle_layer_mode(self) -> None:
        self.single_layer_mode = not self.single_layer_mode
        self.btn_mode.configure(text="单层模式" if self.single_layer_mode else "累计模式")
        self._schedule_render()

    def _step_layer(self, delta: int) -> None:
        self.current_layer = max(0, min(self._max_layer(), self.current_layer + delta))
        self._schedule_render()

    def _apply_preset(self, idx: int) -> None:
        self.pan_x = 0.0
        self.pan_y = 0.0
        presets = {
            0: (0.0, 89.0),  # 俯视
            1: (0.0, 0.0),  # 正前
            2: (90.0, 0.0),  # 右侧
            3: (45.0, 30.0),  # 等轴
            4: (0.0, -89.0),  # 仰视
        }
        self.yaw, self.pitch = presets.get(idx, (45.0, 30.0))
        self._schedule_render()

    def _reset_camera(self) -> None:
        self.selected_color = None
        self.yaw = 45.0
        self.pitch = 30.0
        self.zoom = 1.0
        self.pan_x = 0.0
        self.pan_y = 0.0
        self._rebuild_sidebar_rows()
        self._schedule_render()

    def _toggle_sort(self) -> None:
        self.sort_by_count = not self.sort_by_count
        self.btn_sort.configure(text="排序:数量↓" if self.sort_by_count else "排序:色值↑")
        self._refresh_stats()

    def _refresh_stats(self) -> None:
        stats = compute_color_stats(self.grid_rgb, self.mask)
        if self.sort_by_count:
            stats.sort(key=lambda x: (-x[1], x[0]))
        else:
            stats.sort(key=lambda x: x[0])
        self._all_stats = stats
        self._filter_stats()

    def _filter_stats(self) -> None:
        q = self.search_var.get().strip().lower().lstrip("#")
        if not q:
            self._filtered_stats = list(self._all_stats)
        else:
            self._filtered_stats = [
                (col, cnt) for col, cnt in self._all_stats if q in f"{col:06x}" or q in "彩色方块"
            ]
        self._rebuild_sidebar_rows()

    def _rebuild_sidebar_rows(self) -> None:
        for w in self.sb_inner.winfo_children():
            w.destroy()
        total = max(1, int(self.mask.sum()))
        self.lbl_sb_header.configure(text=f"彩色方块原色清单 ({len(self._all_stats)}色)")

        for col, cnt in self._filtered_stats:
            hex_str = f"#{col:06X}"
            is_sel = self.selected_color == col
            row_bg = BG_CARD_SEL if is_sel else BG_CARD

            row = tk.Frame(
                self.sb_inner,
                bg=row_bg,
                highlightthickness=1,
                highlightbackground=ACCENT_YELLOW if is_sel else BORDER_COLOR,
                cursor="hand2",
            )
            row.pack(fill=tk.X, padx=3, pady=2)

            swatch = tk.Frame(row, bg=hex_str, width=22, height=22, highlightthickness=1, highlightbackground="#FFFFFF")
            swatch.pack(side=tk.LEFT, padx=(6, 8), pady=5)
            swatch.pack_propagate(False)

            info = tk.Frame(row, bg=row_bg)
            info.pack(side=tk.LEFT, fill=tk.X, expand=True, pady=3)
            pct = cnt * 100.0 / total
            lbl1 = tk.Label(
                info,
                text=f"彩色方块  {hex_str}",
                bg=row_bg,
                fg=ACCENT_YELLOW if is_sel else TEXT_MAIN,
                font=("Consolas", 9, "bold"),
                anchor="w",
            )
            lbl1.pack(fill=tk.X)
            lbl2 = tk.Label(
                info,
                text=f"数量: {cnt:,}  ({pct:.1f}%)",
                bg=row_bg,
                fg=TEXT_MUTED,
                font=("Microsoft YaHei UI", 8),
                anchor="w",
            )
            lbl2.pack(fill=tk.X)

            for widget in (row, swatch, info, lbl1, lbl2):
                widget.bind("<Button-1>", lambda _e, c=col: self._on_click_color(c))
                widget.bind("<Double-Button-1>", lambda _e, c=col: self._on_edit_color(c))

    def _on_click_color(self, col: int) -> None:
        self.selected_color = None if self.selected_color == col else col
        self._rebuild_sidebar_rows()
        self._schedule_render()

    def _on_edit_color(self, old_col: int) -> None:
        init_hex = f"#{old_col:06X}"
        picked = colorchooser.askcolor(color=init_hex, title=f"修改彩色方块原色 ({init_hex})", parent=self)
        if not picked or not picked[0]:
            return
        nr, ng, nb = (int(round(picked[0][0])), int(round(picked[0][1])), int(round(picked[0][2])))
        new_col = (nr << 16) | (ng << 8) | nb
        if new_col == old_col:
            return
        rgb_ints = (
            (self.grid_rgb[:, :, 0].astype(np.int32) << 16)
            | (self.grid_rgb[:, :, 1].astype(np.int32) << 8)
            | self.grid_rgb[:, :, 2].astype(np.int32)
        )
        hit = self.mask & (rgb_ints == old_col)
        self.grid_rgb[hit] = (nr, ng, nb)
        if self.selected_color == old_col:
            self.selected_color = new_col
        if self.on_grid_color_change:
            self.on_grid_color_change(self.grid_rgb.copy())
        self._refresh_stats()
        self._schedule_render()

    def _schedule_render(self) -> None:
        if self._render_pending:
            return
        self._render_pending = True
        self.after(16, self._render_now)

    def _render_now(self) -> None:
        self._render_pending = False
        vp_w = max(64, self.canvas.winfo_width())
        vp_h = max(64, self.canvas.winfo_height())

        img_rgb, scale, cx, cy = render_blueprint_3d_image(
            self.grid_rgb,
            self.mask,
            self.orientation,
            vp_w,
            vp_h,
            self.yaw,
            self.pitch,
            self.zoom,
            self.pan_x,
            self.pan_y,
            self.current_layer,
            self.single_layer_mode,
            self.selected_color,
        )

        pil_img = Image.fromarray(img_rgb)
        self._photo_ref = ImageTk.PhotoImage(pil_img)

        self.canvas.delete("all")
        self.canvas.create_image(0, 0, anchor=tk.NW, image=self._photo_ref)

        w, h, l = self._struct_dims()
        self.lbl_layer.configure(text=f"{self.current_layer + 1}/{max(1, h)}")

        rad_y = math.radians(self.yaw)
        rad_p = math.radians(self.pitch)
        cos_y, sin_y = math.cos(rad_y), math.sin(rad_y)
        cos_p, sin_p = math.cos(rad_p), math.sin(rad_p)

        # 绘制 3D 外框、XYZ 标尺与切层黄框
        if self.show_bounds_axes:
            self._draw_bounds_and_rulers(w, h, l, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)

        show_slice = self.single_layer_mode or (self.current_layer < h - 1)
        if show_slice:
            self._draw_slice_box(w, h, l, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)

        # 绘制左下角 3D 方位罗盘
        self._draw_compass(vp_h, cos_y, sin_y, cos_p, sin_p)

        # 绘制左上角规格浮窗胶囊（与 BlueprintInspectorScreen 一致）
        dim_text = f"尺寸: {w} × {h} × {l}  |  彩色方块: {int(self.mask.sum()):,}  |  原色: {len(self._all_stats)}"
        t_id = self.canvas.create_text(
            14, 15, text=dim_text, anchor=tk.W, fill=ACCENT_CYAN, font=("Microsoft YaHei UI", 9, "bold")
        )
        bb = self.canvas.bbox(t_id)
        if bb:
            bg_id = self.canvas.create_rectangle(
                bb[0] - 6, bb[1] - 3, bb[2] + 6, bb[3] + 3, fill="#141620", outline="#32384E"
            )
            self.canvas.tag_lower(bg_id, t_id)

        if self.selected_color is not None:
            f_text = f"高亮过滤: 彩色方块 #{self.selected_color:06X}  (再次点击取消)"
            ft_id = self.canvas.create_text(
                14, 39, text=f_text, anchor=tk.W, fill=ACCENT_YELLOW, font=("Microsoft YaHei UI", 9)
            )
            fbb = self.canvas.bbox(ft_id)
            if fbb:
                fbg = self.canvas.create_rectangle(
                    fbb[0] - 6, fbb[1] - 3, fbb[2] + 6, fbb[3] + 3, fill="#2A2410", outline="#FFCC44"
                )
                self.canvas.tag_lower(fbg, ft_id)

    def _draw_bounds_and_rulers(
        self,
        w: int,
        h: int,
        l: int,
        cos_y: float,
        sin_y: float,
        cos_p: float,
        sin_p: float,
        scale: float,
        cx: float,
        cy: float,
    ) -> None:
        hw, hh, hl = w * 0.5, h * 0.5, l * 0.5

        def p2d(x: float, y: float, z: float) -> tuple[float, float]:
            sx, sy, _ = project_3d_point(x, y, z, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)
            return sx, sy

        corners = [
            p2d(-hw, -hh, -hl),
            p2d(hw, -hh, -hl),
            p2d(hw, -hh, hl),
            p2d(-hw, -hh, hl),
            p2d(-hw, hh, -hl),
            p2d(hw, hh, -hl),
            p2d(hw, hh, hl),
            p2d(-hw, hh, hl),
        ]
        edges = [
            (0, 1), (1, 2), (2, 3), (3, 0),
            (4, 5), (5, 6), (6, 7), (7, 4),
            (0, 4), (1, 5), (2, 6), (3, 7),
        ]
        for a, b in edges:
            self.canvas.create_line(
                corners[a][0], corners[a][1], corners[b][0], corners[b][1], fill="#4A5470", width=1
            )

        gap = max(0.6, 8.0 / max(1.0, scale))
        rulers = [
            ((-hw, -hh, hl + gap), (hw, -hh, hl + gap), "#FF5555", f"X:{w}"),
            ((hw + gap, -hh, -hl), (hw + gap, -hh, hl), "#5599FF", f"Z:{l}"),
            ((-hw - gap, -hh, hl), (-hw - gap, hh, hl), "#55FF55", f"Y:{h}"),
        ]
        for p0, p1, col, label in rulers:
            x0, y0 = p2d(*p0)
            x1, y1 = p2d(*p1)
            self.canvas.create_line(x0, y0, x1, y1, fill=col, width=2)
            if math.hypot(x1 - x0, y1 - y0) > 18:
                self.canvas.create_text(
                    (x0 + x1) * 0.5,
                    (y0 + y1) * 0.5 - 8,
                    text=label,
                    fill=col,
                    font=("Consolas", 9, "bold"),
                )

    def _draw_slice_box(
        self,
        w: int,
        h: int,
        l: int,
        cos_y: float,
        sin_y: float,
        cos_p: float,
        sin_p: float,
        scale: float,
        cx: float,
        cy: float,
    ) -> None:
        hw, hh, hl = w * 0.5, h * 0.5, l * 0.5
        sy_val = (self.current_layer + 1.0) - hh
        pad = 0.05
        pts = [
            project_3d_point(-hw - pad, sy_val, -hl - pad, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)[:2],
            project_3d_point(hw + pad, sy_val, -hl - pad, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)[:2],
            project_3d_point(hw + pad, sy_val, hl + pad, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)[:2],
            project_3d_point(-hw - pad, sy_val, hl + pad, cos_y, sin_y, cos_p, sin_p, scale, cx, cy)[:2],
        ]
        for i in range(4):
            a, b = pts[i], pts[(i + 1) % 4]
            self.canvas.create_line(a[0], a[1], b[0], b[1], fill="#FFDD44", width=2)

    def _draw_compass(
        self, vp_h: int, cos_y: float, sin_y: float, cos_p: float, sin_p: float
    ) -> None:
        cx = COMPASS_CENTER_X
        cy = vp_h - 38
        r = COMPASS_HALF
        border = "#32384E" if self.show_bounds_axes else "#5A4D30"
        self.canvas.create_rectangle(cx - r, cy - r, cx + r, cy + r, fill="#161822", outline=border, width=1)

        axis_len = 17.0
        axes = [
            (1.0, 0.0, 0.0, "#FF5555", "东"),
            (0.0, 1.0, 0.0, "#55FF55", "上"),
            (0.0, 0.0, 1.0, "#5599FF", "南"),
        ]
        items = []
        for ax, ay, az, col, lbl in axes:
            sx, sy, depth = project_3d_point(ax, ay, az, cos_y, sin_y, cos_p, sin_p, axis_len, cx, cy)
            items.append((depth, sx, sy, col, lbl))
        items.sort(key=lambda t: t[0])
        for _, sx, sy, col, lbl in items:
            self.canvas.create_line(cx, cy, sx, sy, fill=col, width=2)
            self.canvas.create_text(sx + (sx - cx) * 0.25, sy + (sy - cy) * 0.25, text=lbl, fill=col, font=("Microsoft YaHei UI", 7, "bold"))

        state_txt = "外框:显" if self.show_bounds_axes else "外框:隐"
        self.canvas.create_text(
            cx, cy + r + 9, text=state_txt, fill=TEXT_MUTED, font=("Microsoft YaHei UI", 7)
        )

    def _on_export(self) -> None:
        safe_name = "".join(c if c.isalnum() or c in "-_ " else "_" for c in self.title_name).strip() or "bead_art"
        default_file = f"{safe_name}_{self.orientation}.litematic"
        out_path = filedialog.asksaveasfilename(
            parent=self,
            title="导出蓝图大炮文件 (.litematic)",
            initialdir=self.default_export_dir or None,
            initialfile=default_file,
            defaultextension=".litematic",
            filetypes=[("Litematica 蓝图文件", "*.litematic"), ("所有文件", "*.*")],
        )
        if not out_path:
            return
        try:
            info = export_litematic(
                out_path,
                self.grid_rgb,
                self.mask,
                orientation=self.orientation,
                blueprint_name=f"{safe_name}_{self.orientation}",
            )
            messagebox.showinfo(
                "导出成功",
                f"已成功导出蓝图文件！\n\n"
                f"路径: {info['path']}\n"
                f"尺寸: {info['width']} × {info['height']} × {info['length']} ({self.orientation.upper()})\n"
                f"彩色方块数: {info['total_blocks']:,}\n\n"
                f"放入游戏目录 schematics 文件夹后，即可在蓝图桌导入并交由蓝图大炮建造！",
                parent=self,
            )
        except Exception as exc:
            messagebox.showerror("导出失败", f"导出蓝图时出错:\n{exc}", parent=self)
