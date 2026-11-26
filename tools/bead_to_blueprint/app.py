"""
拼豆图纸 -> 蓝图大炮 (.litematic) 转换工具主界面。
支持导入拼豆图纸、识别红色 10x10 大格与精确原色、选择 X,Y(竖放) 或 X,Z(平铺)、
打开与蓝图桌一致的 3D 预览窗口，并导出使用 tuanzis_mod:color_block 的 .litematic 蓝图。
"""

from __future__ import annotations

import os
import sys
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, messagebox
from typing import Literal

import numpy as np
from PIL import Image, ImageTk

from bead_parser import (
    BeadPatternResult,
    ParseOptions,
    parse_bead_image,
    render_grid_overlay,
    render_pixel_preview,
)
from litematic_io import export_litematic
from preview_window import (
    ACCENT_CYAN,
    ACCENT_YELLOW,
    BG_BAR,
    BG_CANVAS,
    BG_CARD,
    BG_SIDEBAR,
    BORDER_COLOR,
    BlueprintPreviewWindow,
    TEXT_MAIN,
    TEXT_MUTED,
)


def _detect_default_schematics_dir() -> str:
    """自动优先定位项目 run/schematics 或 .minecraft/schematics 目录。"""
    here = Path(__file__).resolve()
    candidates = [
        here.parents[2] / "run" / "schematics",
        Path.cwd() / "run" / "schematics",
        Path(os.environ.get("APPDATA", "")) / ".minecraft" / "schematics",
    ]
    for p in candidates:
        if p.exists() and p.is_dir():
            return str(p)
    return str(Path.cwd())


def _fit_pil_image(img_rgb: np.ndarray, max_w: int, max_h: int, nearest: bool = False) -> ImageTk.PhotoImage:
    h, w = img_rgb.shape[:2]
    max_w = max(64, max_w)
    max_h = max(64, max_h)
    scale = min(max_w / max(1, w), max_h / max(1, h))
    nw = max(1, int(round(w * scale)))
    nh = max(1, int(round(h * scale)))
    resample = Image.Resampling.NEAREST if nearest else Image.Resampling.LANCZOS
    pil = Image.fromarray(img_rgb).resize((nw, nh), resample)
    return ImageTk.PhotoImage(pil)


class BeadToBlueprintApp(tk.Tk):
    def __init__(self, initial_image: str | None = None) -> None:
        super().__init__()
        self.title("团子模组 · 拼豆图纸转蓝图工具 (彩色方块原色版)")
        self.geometry("1160x760")
        self.minsize(920, 600)
        self.configure(bg=BG_CANVAS)

        self.image_path: Path | None = None
        self.result: BeadPatternResult | None = None
        self.default_export_dir = _detect_default_schematics_dir()

        self.orient_var = tk.StringVar(value="xy")
        self.major_cells_var = tk.StringVar(value="10")
        self.delta_e_var = tk.DoubleVar(value=14.5)
        self.white_mode_var = tk.StringVar(value="all_white")
        self.clean_specks_var = tk.BooleanVar(value=True)
        self.manual_grid_var = tk.BooleanVar(value=False)
        self.manual_cols_var = tk.StringVar(value="")
        self.manual_rows_var = tk.StringVar(value="")

        self._photo_left: ImageTk.PhotoImage | None = None
        self._photo_right: ImageTk.PhotoImage | None = None
        self._preview_win: BlueprintPreviewWindow | None = None
        self._reparse_job: str | None = None

        self._build_ui()
        if initial_image and Path(initial_image).is_file():
            self.after(50, lambda: self._load_image(Path(initial_image)))

    def _build_ui(self) -> None:
        # 1. 顶栏操作区
        top = tk.Frame(self, bg=BG_BAR, highlightthickness=1, highlightbackground=BORDER_COLOR)
        top.pack(side=tk.TOP, fill=tk.X)

        tk.Button(
            top,
            text="导入拼豆图纸...",
            command=self._choose_image,
            bg="#2D5A88",
            fg="#FFFFFF",
            activebackground="#3972AC",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=12,
            pady=5,
            font=("Microsoft YaHei UI", 9, "bold"),
            cursor="hand2",
        ).pack(side=tk.LEFT, padx=(10, 8), pady=7)

        # 输出方向选择 (X,Y 竖放 / X,Z 平铺)
        orient_box = tk.Frame(top, bg=BG_CARD, highlightthickness=1, highlightbackground=BORDER_COLOR)
        orient_box.pack(side=tk.LEFT, padx=6, pady=6)
        tk.Label(
            orient_box, text="输出格式:", bg=BG_CARD, fg=TEXT_MUTED, font=("Microsoft YaHei UI", 9)
        ).pack(side=tk.LEFT, padx=(8, 4), pady=3)
        tk.Radiobutton(
            orient_box,
            text="X, Y (竖着立面)",
            variable=self.orient_var,
            value="xy",
            bg=BG_CARD,
            fg=TEXT_MAIN,
            selectcolor="#12131A",
            activebackground=BG_CARD,
            activeforeground=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 9),
            cursor="hand2",
        ).pack(side=tk.LEFT, padx=4)
        tk.Radiobutton(
            orient_box,
            text="X, Z (平铺地面)",
            variable=self.orient_var,
            value="xz",
            bg=BG_CARD,
            fg=TEXT_MAIN,
            selectcolor="#12131A",
            activebackground=BG_CARD,
            activeforeground=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 9),
            cursor="hand2",
        ).pack(side=tk.LEFT, padx=(4, 8))

        self.btn_export = tk.Button(
            top,
            text="导出蓝图 (.litematic)",
            command=self._export_litematic,
            state=tk.DISABLED,
            bg="#265C3B",
            fg="#FFFFFF",
            disabledforeground="#7A8694",
            activebackground="#327A4E",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=12,
            pady=5,
            font=("Microsoft YaHei UI", 9, "bold"),
            cursor="hand2",
        )
        self.btn_export.pack(side=tk.RIGHT, padx=(6, 10), pady=7)

        self.btn_preview = tk.Button(
            top,
            text="蓝图桌 3D 预览",
            command=self._open_3d_preview,
            state=tk.DISABLED,
            bg="#4B3E8C",
            fg="#FFFFFF",
            disabledforeground="#7A8694",
            activebackground="#6051B0",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=12,
            pady=5,
            font=("Microsoft YaHei UI", 9, "bold"),
            cursor="hand2",
        )
        self.btn_preview.pack(side=tk.RIGHT, padx=6, pady=7)

        # 2. 识别参数控制栏
        params = tk.Frame(self, bg=BG_SIDEBAR, highlightthickness=1, highlightbackground=BORDER_COLOR)
        params.pack(side=tk.TOP, fill=tk.X)

        lbl_cfg = dict(bg=BG_SIDEBAR, fg=TEXT_MUTED, font=("Microsoft YaHei UI", 9))
        ent_cfg = dict(
            bg="#10121A",
            fg=TEXT_MAIN,
            insertbackground=TEXT_MAIN,
            relief=tk.FLAT,
            highlightthickness=1,
            highlightbackground=BORDER_COLOR,
            font=("Consolas", 9),
        )

        tk.Label(params, text="红线大格:", **lbl_cfg).pack(side=tk.LEFT, padx=(10, 2), pady=6)
        tk.Entry(params, textvariable=self.major_cells_var, width=4, **ent_cfg).pack(side=tk.LEFT, padx=(0, 2))
        tk.Label(params, text="格", **lbl_cfg).pack(side=tk.LEFT, padx=(0, 10))

        tk.Label(params, text="防模糊原色合并(ΔE):", **lbl_cfg).pack(side=tk.LEFT, padx=(2, 2))
        self.scale_de = tk.Scale(
            params,
            from_=0.0,
            to=28.0,
            resolution=0.5,
            orient=tk.HORIZONTAL,
            variable=self.delta_e_var,
            length=120,
            showvalue=True,
            bg=BG_SIDEBAR,
            fg=ACCENT_CYAN,
            troughcolor="#10121A",
            highlightthickness=0,
            font=("Consolas", 8),
            command=lambda _v: self._schedule_reparse(),
        )
        self.scale_de.pack(side=tk.LEFT, padx=(0, 10))

        tk.Checkbutton(
            params,
            text="保留无字内部白色",
            variable=self.white_mode_var,
            onvalue="outer_only",
            offvalue="all_white",
            command=self._schedule_reparse,
            bg=BG_SIDEBAR,
            fg=TEXT_MAIN,
            selectcolor="#10121A",
            activebackground=BG_SIDEBAR,
            activeforeground=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 9),
        ).pack(side=tk.LEFT, padx=6)

        tk.Checkbutton(
            params,
            text="清理孤立杂点",
            variable=self.clean_specks_var,
            command=self._schedule_reparse,
            bg=BG_SIDEBAR,
            fg=TEXT_MAIN,
            selectcolor="#10121A",
            activebackground=BG_SIDEBAR,
            activeforeground=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 9),
        ).pack(side=tk.LEFT, padx=6)

        tk.Checkbutton(
            params,
            text="手动指定格数:",
            variable=self.manual_grid_var,
            command=self._on_toggle_manual,
            bg=BG_SIDEBAR,
            fg=TEXT_MAIN,
            selectcolor="#10121A",
            activebackground=BG_SIDEBAR,
            activeforeground=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 9),
        ).pack(side=tk.LEFT, padx=(8, 2))

        self.ent_cols = tk.Entry(params, textvariable=self.manual_cols_var, width=5, state=tk.DISABLED, **ent_cfg)
        self.ent_cols.pack(side=tk.LEFT, padx=2)
        tk.Label(params, text="×", **lbl_cfg).pack(side=tk.LEFT)
        self.ent_rows = tk.Entry(params, textvariable=self.manual_rows_var, width=5, state=tk.DISABLED, **ent_cfg)
        self.ent_rows.pack(side=tk.LEFT, padx=(2, 6))

        tk.Button(
            params,
            text="重新识别",
            command=self._reparse_now,
            bg="#2B3046",
            fg=TEXT_MAIN,
            activebackground="#3C4361",
            activeforeground="#FFFFFF",
            relief=tk.FLAT,
            padx=8,
            pady=2,
            font=("Microsoft YaHei UI", 9),
            cursor="hand2",
        ).pack(side=tk.LEFT, padx=4, pady=4)

        # 3. 双栏对比预览区
        split = tk.Frame(self, bg=BG_CANVAS)
        split.pack(side=tk.TOP, fill=tk.BOTH, expand=True, padx=10, pady=8)
        split.columnconfigure(0, weight=1)
        split.columnconfigure(1, weight=1)
        split.rowconfigure(0, weight=1)

        left_box = tk.Frame(split, bg=BG_SIDEBAR, highlightthickness=1, highlightbackground=BORDER_COLOR)
        left_box.grid(row=0, column=0, sticky="nsew", padx=(0, 5))
        tk.Label(
            left_box,
            text="原图与红线大格识别核对（绿线 = 检测到的 10×10 大格线）",
            bg=BG_BAR,
            fg=TEXT_MAIN,
            font=("Microsoft YaHei UI", 9, "bold"),
            anchor="w",
            padx=8,
            pady=5,
        ).pack(fill=tk.X)
        self.canvas_left = tk.Canvas(left_box, bg=BG_CANVAS, highlightthickness=0)
        self.canvas_left.pack(fill=tk.BOTH, expand=True)
        self.canvas_left.bind("<Configure>", lambda _e: self._refresh_previews())

        right_box = tk.Frame(split, bg=BG_SIDEBAR, highlightthickness=1, highlightbackground=BORDER_COLOR)
        right_box.grid(row=0, column=1, sticky="nsew", padx=(5, 0))
        tk.Label(
            right_box,
            text="彩色方块原色还原预览（点击「蓝图桌 3D 预览」可旋转/切层/单色微调）",
            bg=BG_BAR,
            fg=ACCENT_CYAN,
            font=("Microsoft YaHei UI", 9, "bold"),
            anchor="w",
            padx=8,
            pady=5,
        ).pack(fill=tk.X)
        self.canvas_right = tk.Canvas(right_box, bg=BG_CANVAS, highlightthickness=0)
        self.canvas_right.pack(fill=tk.BOTH, expand=True)
        self.canvas_right.bind("<Configure>", lambda _e: self._refresh_previews())

        # 4. 底部状态栏
        status_bar = tk.Frame(self, bg=BG_BAR, height=28, highlightthickness=1, highlightbackground=BORDER_COLOR)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)
        status_bar.pack_propagate(False)
        self.lbl_status = tk.Label(
            status_bar,
            text="请点击左上角「导入拼豆图纸...」选择图片（支持 PNG/JPG/WEBP/BMP，模糊图片也可自动识别）",
            bg=BG_BAR,
            fg=ACCENT_YELLOW,
            font=("Microsoft YaHei UI", 9),
            anchor="w",
            padx=10,
        )
        self.lbl_status.pack(fill=tk.BOTH, expand=True)

    def _on_toggle_manual(self) -> None:
        st = tk.NORMAL if self.manual_grid_var.get() else tk.DISABLED
        self.ent_cols.configure(state=st)
        self.ent_rows.configure(state=st)
        if not self.manual_grid_var.get():
            self._reparse_now()

    def _choose_image(self) -> None:
        path = filedialog.askopenfilename(
            parent=self,
            title="选择拼豆图纸图片",
            filetypes=[
                ("图片文件", "*.png;*.jpg;*.jpeg;*.webp;*.bmp"),
                ("所有文件", "*.*"),
            ],
        )
        if path:
            self._load_image(Path(path))

    def _load_image(self, path: Path) -> None:
        self.image_path = path
        self.manual_grid_var.set(False)
        self.ent_cols.configure(state=tk.DISABLED)
        self.ent_rows.configure(state=tk.DISABLED)
        self._reparse_now()

    def _schedule_reparse(self) -> None:
        if self.image_path is None:
            return
        if self._reparse_job is not None:
            self.after_cancel(self._reparse_job)
        self._reparse_job = self.after(180, self._reparse_now)

    def _build_options(self) -> ParseOptions:
        try:
            cpm = max(2, min(50, int(self.major_cells_var.get().strip() or "10")))
        except ValueError:
            cpm = 10
        m_cols: int | None = None
        m_rows: int | None = None
        if self.manual_grid_var.get():
            try:
                v = int(self.manual_cols_var.get().strip())
                if v > 0:
                    m_cols = v
            except ValueError:
                pass
            try:
                v = int(self.manual_rows_var.get().strip())
                if v > 0:
                    m_rows = v
            except ValueError:
                pass
        wm: Literal["all_white", "outer_only"] = (
            "outer_only" if self.white_mode_var.get() == "outer_only" else "all_white"
        )
        return ParseOptions(
            cells_per_major=cpm,
            merge_delta_e=float(self.delta_e_var.get()),
            white_bg_mode=wm,
            clean_isolated_specks=bool(self.clean_specks_var.get()),
            manual_cols=m_cols,
            manual_rows=m_rows,
        )

    def _reparse_now(self) -> None:
        self._reparse_job = None
        if self.image_path is None:
            return
        self.lbl_status.configure(text=f"正在识别图纸: {self.image_path.name} ...", fg=ACCENT_CYAN)
        self.update_idletasks()
        try:
            opts = self._build_options()
            res = parse_bead_image(self.image_path, opts)
            self.result = res
            if not self.manual_grid_var.get():
                self.manual_cols_var.set(str(res.cols))
                self.manual_rows_var.set(str(res.rows))
            self.btn_preview.configure(state=tk.NORMAL)
            self.btn_export.configure(state=tk.NORMAL)
            self.lbl_status.configure(text=f"[{self.image_path.name}]  {res.note}", fg=ACCENT_CYAN)
            self._refresh_previews()
        except Exception as exc:
            self.lbl_status.configure(text=f"识别失败: {exc}", fg="#FF6666")
            messagebox.showerror("识别失败", f"解析拼豆图纸时发生错误:\n{exc}", parent=self)

    def _refresh_previews(self) -> None:
        if self.result is None:
            return
        lw = max(80, self.canvas_left.winfo_width() - 12)
        lh = max(80, self.canvas_left.winfo_height() - 12)
        rw = max(80, self.canvas_right.winfo_width() - 12)
        rh = max(80, self.canvas_right.winfo_height() - 12)

        overlay_rgb = render_grid_overlay(self.result)
        pixel_rgb = render_pixel_preview(self.result)

        self._photo_left = _fit_pil_image(overlay_rgb, lw, lh, nearest=False)
        self._photo_right = _fit_pil_image(pixel_rgb, rw, rh, nearest=True)

        self.canvas_left.delete("all")
        self.canvas_left.create_image(
            self.canvas_left.winfo_width() // 2,
            self.canvas_left.winfo_height() // 2,
            anchor=tk.CENTER,
            image=self._photo_left,
        )
        self.canvas_right.delete("all")
        self.canvas_right.create_image(
            self.canvas_right.winfo_width() // 2,
            self.canvas_right.winfo_height() // 2,
            anchor=tk.CENTER,
            image=self._photo_right,
        )

    def _on_preview_orient_change(self, new_orient: Literal["xy", "xz"]) -> None:
        self.orient_var.set(new_orient)

    def _on_preview_color_change(self, updated_rgb: np.ndarray) -> None:
        if self.result is not None:
            self.result.grid_rgb = updated_rgb
            self._refresh_previews()

    def _open_3d_preview(self) -> None:
        if self.result is None:
            return
        if self._preview_win is not None and self._preview_win.winfo_exists():
            self._preview_win.destroy()
        orient: Literal["xy", "xz"] = "xz" if self.orient_var.get() == "xz" else "xy"
        title_name = self.image_path.stem if self.image_path else "bead_art"
        self._preview_win = BlueprintPreviewWindow(
            self,
            self.result.grid_rgb,
            self.result.mask,
            orientation=orient,
            title_name=title_name,
            default_export_dir=self.default_export_dir,
            on_orientation_change=self._on_preview_orient_change,
            on_grid_color_change=self._on_preview_color_change,
        )

    def _export_litematic(self) -> None:
        if self.result is None:
            return
        orient: Literal["xy", "xz"] = "xz" if self.orient_var.get() == "xz" else "xy"
        stem = self.image_path.stem if self.image_path else "bead_art"
        safe_stem = "".join(c if c.isalnum() or c in "-_ " else "_" for c in stem).strip() or "bead_art"
        out_path = filedialog.asksaveasfilename(
            parent=self,
            title="导出蓝图大炮文件 (.litematic)",
            initialdir=self.default_export_dir or None,
            initialfile=f"{safe_stem}_{orient}.litematic",
            defaultextension=".litematic",
            filetypes=[("Litematica 蓝图文件", "*.litematic"), ("所有文件", "*.*")],
        )
        if not out_path:
            return
        try:
            info = export_litematic(
                out_path,
                self.result.grid_rgb,
                self.result.mask,
                orientation=orient,
                blueprint_name=f"{safe_stem}_{orient}",
            )
            messagebox.showinfo(
                "导出成功",
                f"蓝图导出成功！\n\n"
                f"路径: {info['path']}\n"
                f"尺寸: {info['width']} × {info['height']} × {info['length']} ({orient.upper()})\n"
                f"彩色方块数: {info['total_blocks']:,}\n\n"
                f"已使用 tuanzis_mod:color_block 写入全真原色，放入 schematics 文件夹即可在蓝图桌导入。",
                parent=self,
            )
        except Exception as exc:
            messagebox.showerror("导出失败", f"导出蓝图时出错:\n{exc}", parent=self)


def main() -> None:
    init_img = sys.argv[1] if len(sys.argv) > 1 else None
    app = BeadToBlueprintApp(initial_image=init_img)
    app.mainloop()


if __name__ == "__main__":
    main()
