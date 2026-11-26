"""
拼豆图纸识别引擎：
1. 自动定位图纸网格区域（自动剔除顶部标题、底部图例栏、侧边水印与行列号）。
2. 自动检测红色 10x10 大格线（支持非均匀缩放/防盗轻度变形图纸与模糊图片），
   在无红线或极度模糊场景下自动降级为边缘周期估计或手动指定网格尺寸。
3. 抗色号文字干扰的子像素环形 6-NN Medoid 采样，还原真实原色 RGB。
4. CIE L*a*b* 原色聚类去噪 + 白底背景过滤 + 物理孤立 1px 杂点清理。
"""

from __future__ import annotations

from collections import deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Literal

import cv2
import numpy as np


@dataclass
class ParseOptions:
    cells_per_major: int = 10
    merge_delta_e: float = 14.5
    white_bg_mode: Literal["all_white", "outer_only"] = "all_white"
    white_threshold: int = 228
    detect_white_text: bool = True
    clean_isolated_specks: bool = True
    manual_cols: int | None = None
    manual_rows: int | None = None


@dataclass
class BeadPatternResult:
    source_rgb: np.ndarray
    grid_rgb: np.ndarray  # (rows, cols, 3) uint8
    mask: np.ndarray  # (rows, cols) bool, True = 彩色方块, False = 空气
    bbox: tuple[int, int, int, int]  # (gx0, gy0, gx1, gy1)
    x_major: list[float] = field(default_factory=list)
    y_major: list[float] = field(default_factory=list)
    x_bounds: np.ndarray = field(default_factory=lambda: np.zeros(0))
    y_bounds: np.ndarray = field(default_factory=lambda: np.zeros(0))
    color_stats: list[tuple[int, int]] = field(default_factory=list)  # [(0xRRGGBB, count), ...]
    method: str = "red_grid"
    note: str = ""

    @property
    def rows(self) -> int:
        return int(self.mask.shape[0])

    @property
    def cols(self) -> int:
        return int(self.mask.shape[1])

    @property
    def total_blocks(self) -> int:
        return int(self.mask.sum())


def load_image_rgb(path: str | Path) -> np.ndarray:
    """支持 Windows 中文/Unicode 路径读取图片并转为 RGB uint8。"""
    raw = np.fromfile(str(path), dtype=np.uint8)
    bgr = cv2.imdecode(raw, cv2.IMREAD_COLOR)
    if bgr is None:
        raise ValueError(f"无法读取图片文件: {path}")
    return cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)


def rgb_to_cielab(rgb_arr: np.ndarray) -> np.ndarray:
    """将 (..., 3) RGB uint8/float 转为标准 CIE L*a*b* (L* in [0,100], a*,b* in [-128,127])。"""
    shape = rgb_arr.shape
    flat = np.clip(rgb_arr, 0, 255).astype(np.uint8).reshape(-1, 1, 3)
    lab = cv2.cvtColor(flat, cv2.COLOR_RGB2LAB).astype(np.float32).reshape(-1, 3)
    lab[:, 0] *= 100.0 / 255.0
    lab[:, 1] -= 128.0
    lab[:, 2] -= 128.0
    return lab.reshape(shape)


def _compute_line_maps(rgb_f: np.ndarray) -> dict[str, np.ndarray]:
    r, g, b = rgb_f[:, :, 0], rgb_f[:, :, 1], rgb_f[:, :, 2]
    bright = (r + g + b) / 3.0
    redness = r - 0.5 * (g + b)
    near_white = cv2.dilate((bright > 205).astype(np.uint8), np.ones((5, 5), np.uint8)) > 0

    # 多尺度红线脊检测（同时兼容清晰 1px 细红线与模糊变粗的 2~4px 红线）
    vx2 = np.zeros_like(redness)
    vx2[:, 2:-2] = redness[:, 2:-2] - 0.5 * (redness[:, :-4] + redness[:, 4:])
    vx = vx2
    if redness.shape[1] > 10:
        vx4 = np.zeros_like(redness)
        vx4[:, 4:-4] = redness[:, 4:-4] - 0.5 * (redness[:, :-8] + redness[:, 8:])
        vx = np.maximum(vx2, vx4)

    hy2 = np.zeros_like(redness)
    hy2[2:-2, :] = redness[2:-2, :] - 0.5 * (redness[:-4, :] + redness[4:, :])
    hy = hy2
    if redness.shape[0] > 10:
        hy4 = np.zeros_like(redness)
        hy4[4:-4, :] = redness[4:-4, :] - 0.5 * (redness[:-8, :] + redness[8:, :])
        hy = np.maximum(hy2, hy4)

    v_dark = np.zeros_like(bright)
    v_dark[:, 2:-2] = 0.5 * (bright[:, :-4] + bright[:, 4:]) - bright[:, 2:-2]
    h_dark = np.zeros_like(bright)
    h_dark[2:-2, :] = 0.5 * (bright[:-4, :] + bright[4:, :]) - bright[2:-2, :]

    return {
        "bright": bright,
        "redness": redness,
        "near_white": near_white,
        "vx": vx,
        "hy": hy,
        "vx2": vx2,
        "hy2": hy2,
        "v_dark": v_dark,
        "h_dark": h_dark,
    }


def _detect_grid_bbox(maps: dict[str, np.ndarray], w: int, h: int) -> tuple[int, int, int, int] | None:
    """利用红线起止区间自动定位网格主框，排除标题、底部图例与侧边文字。"""
    nw = maps["near_white"]
    vx = maps["vx"]
    hy = maps["hy"]
    v_dark = maps["v_dark"]
    h_dark = maps["h_dark"]

    v_red_score = np.where(nw, np.maximum(0.0, vx), np.maximum(0.0, vx) * 0.4).sum(axis=0)
    h_red_score = np.where(nw, np.maximum(0.0, hy), np.maximum(0.0, hy) * 0.4).sum(axis=1)

    if v_red_score.max() < h * 0.5 or h_red_score.max() < w * 0.5:
        return None

    # 选出最显著的若干垂直/水平红线，统计每条红线的起止坐标，取中位数过滤图例/行列号干扰
    def strong_peaks(sig: np.ndarray, min_dist: int = 16, k: int = 10) -> list[int]:
        idx = np.argsort(sig)[::-1]
        picked: list[int] = []
        thr = sig.max() * 0.25
        for p in idx:
            if sig[p] < thr:
                break
            if all(abs(int(p) - q) >= min_dist for q in picked):
                picked.append(int(p))
                if len(picked) >= k:
                    break
        return picked

    v_cols = strong_peaks(v_red_score, min_dist=max(12, w // 45), k=10)
    h_rows = strong_peaks(h_red_score, min_dist=max(12, h // 45), k=10)
    if len(v_cols) < 2 or len(h_rows) < 2:
        return None

    y_starts, y_ends = [], []
    for c in v_cols:
        col_strip = np.maximum(0.0, vx[:, max(0, c - 1) : min(w, c + 2)]).max(axis=1)
        ys = np.where(col_strip > 4.5)[0]
        if ys.size >= max(20, h // 8):
            y_starts.append(int(ys[0]))
            y_ends.append(int(ys[-1]))

    x_starts, x_ends = [], []
    for r in h_rows:
        row_strip = np.maximum(0.0, hy[max(0, r - 1) : min(h, r + 2), :]).max(axis=0)
        xs = np.where(row_strip > 4.5)[0]
        if xs.size >= max(20, w // 8):
            x_starts.append(int(xs[0]))
            x_ends.append(int(xs[-1]))

    if not y_starts or not x_starts:
        return None

    gy0 = int(np.median(y_starts))
    gy1 = int(np.median(y_ends))
    gx0 = int(np.median(x_starts))
    gx1 = int(np.median(x_ends))

    # 吸附到网格外框细线（使用窄尺度信号避免吸附到外侧图例条）
    h_border_sig = (nw & ((h_dark > 8) | (maps["hy2"] > 4))).mean(axis=1)
    v_border_sig = (nw & ((v_dark > 8) | (maps["vx2"] > 4))).mean(axis=0)

    def snap_edge(pos: int, sig: np.ndarray, lo_off: int, hi_off: int) -> int:
        a = max(0, pos + lo_off)
        b = min(len(sig), pos + hi_off + 1)
        if a >= b:
            return pos
        window = sig[a:b]
        best_rel = int(np.argmax(window))
        return a + best_rel if window[best_rel] > 0.12 else pos

    gy0 = snap_edge(gy0, h_border_sig, -8, 4)
    gy1 = snap_edge(gy1, h_border_sig, -8, 4)
    gx0 = snap_edge(gx0, v_border_sig, -4, 10)
    gx1 = snap_edge(gx1, v_border_sig, -4, 4)

    if gx1 - gx0 < 20 or gy1 - gy0 < 20:
        return None
    return gx0, gy0, gx1, gy1


def _extract_major_chain(
    red_sig: np.ndarray,
    dark_sig: np.ndarray,
    start_coord: int,
    end_coord: int,
    cells_per_major: int,
) -> tuple[list[float], np.ndarray] | None:
    """
    在 [start_coord, end_coord] 区间内提取平滑有序的红线大格序列，并细分为逐格边界。
    特性：
    - 支持防盗图纸的局部非均匀拉伸/压缩；
    - 自动剔除 5 格虚线伪峰；
    - 自动插值补全被大面积深色色块遮挡的缺失红线；
    - 利用 5 格虚线或局部格宽精确计算末尾/开头残余不足 10 格的列数/行数。
    """
    span = len(red_sig)
    if span < 20:
        return None

    med = float(np.median(red_sig))
    mx = float(red_sig.max())
    if mx <= med + 0.5:
        return None

    thr = med + (mx - med) * 0.16
    raw_peaks = [
        i
        for i in range(span)
        if red_sig[i] > thr
        and (i == 0 or red_sig[i] >= red_sig[i - 1])
        and (i == span - 1 or red_sig[i] >= red_sig[i + 1])
    ]
    if len(raw_peaks) < 2:
        return None

    # 非极大值抑制（最小峰距先设为 12px）
    raw_peaks.sort(key=lambda i: float(red_sig[i]), reverse=True)
    kept: list[int] = []
    min_nms = max(10, int(span * 0.015))
    for p in raw_peaks:
        if all(abs(p - k) >= min_nms for k in kept):
            kept.append(p)
    kept.sort()
    if len(kept) < 2:
        return None

    # 从最强若干峰估计标称大格步长
    strong_k = sorted(kept, key=lambda i: float(red_sig[i]), reverse=True)[: min(14, len(kept))]
    strong_k.sort()
    raw_diffs = np.diff(strong_k)
    valid_diffs = raw_diffs[(raw_diffs >= max(14, cells_per_major * 2)) & (raw_diffs <= span * 0.55)]
    if valid_diffs.size == 0:
        return None
    nominal_step = float(np.median(valid_diffs))

    # 过滤半步长杂峰（如 5 格虚线被轻微染红），并保留主序列
    # 使用前向贪心+局部步长跟踪构建平滑红线链
    # 先确保起点附近若有明显边框峰被纳入
    candidates = kept[:]
    if candidates[0] > nominal_step * 0.55:
        # 起点本身可能是未被红线高亮的边框（如 top border y=31）
        if dark_sig[0] > 0.12 or candidates[0] <= nominal_step * 1.35:
            candidates.insert(0, 0)

    # 动态规划选出最优平滑红线链：奖励红线得分 + 惩罚步长剧烈跳变（允许 2x 跨步后补点）
    n = len(candidates)
    peak_scores = np.array(
        [float(red_sig[c]) / max(mx, 1e-6) + (0.35 if c == 0 else 0.0) for c in candidates],
        dtype=np.float64,
    )

    best_chain: list[int] = []
    # 状态: dp[i] = (score, prev_idx, last_step)
    dp_score = peak_scores.copy()
    dp_prev = [-1] * n
    dp_step = [nominal_step] * n

    for i in range(1, n):
        ci = candidates[i]
        best_s = dp_score[i]
        best_p = -1
        best_st = nominal_step
        for j in range(i - 1, -1, -1):
            d = ci - candidates[j]
            ref = dp_step[j]
            if d > ref * 3.3:
                break
            # 判定跨越的大格数 k in {1, 2, 3}
            k_step = int(round(d / ref))
            if k_step < 1 or k_step > 3:
                continue
            unit = d / k_step
            ratio = unit / ref
            if ratio < 0.58 or ratio > 1.65:
                continue
            # 惩罚跳格与步长突变
            jump_pen = (k_step - 1) * 0.35
            smooth_pen = abs(np.log(ratio)) * 1.4
            cand_s = dp_score[j] + peak_scores[i] - jump_pen - smooth_pen
            if cand_s > best_s:
                best_s = cand_s
                best_p = j
                best_st = 0.65 * unit + 0.35 * ref
        dp_score[i] = best_s
        dp_prev[i] = best_p
        dp_step[i] = best_st

    end_i = int(np.argmax(dp_score))
    cur = end_i
    while cur != -1:
        best_chain.append(candidates[cur])
        cur = dp_prev[cur]
    best_chain.reverse()

    if len(best_chain) < 2:
        return None

    # 补全缺失的中间红线（2x / 3x 跨步）以及首尾边框
    majors: list[float] = [float(best_chain[0])]
    chain_diffs = np.diff(best_chain)
    med_step = float(np.median(chain_diffs))

    for idx in range(1, len(best_chain)):
        a = majors[-1]
        b = float(best_chain[idx])
        local_ref = (majors[-1] - majors[-2]) if len(majors) >= 2 else med_step
        d = b - a
        k_step = max(1, int(round(d / max(local_ref, 1e-3))))
        if k_step > 1 and d / k_step >= local_ref * 0.6:
            for s in range(1, k_step):
                est = a + d * (s / k_step)
                lo = max(0, int(est - 6))
                hi = min(span, int(est + 7))
                if hi > lo and red_sig[lo:hi].max() > med + (mx - med) * 0.08:
                    snapped = float(lo + int(np.argmax(red_sig[lo:hi])))
                else:
                    snapped = est
                majors.append(snapped)
        majors.append(b)

    # 若首条红线距离左/上边框超过 ~0.85 个大格，说明起点处是第 0 条红线（如顶部黑框线）
    first_step = majors[1] - majors[0]
    if majors[0] >= first_step * 0.78:
        k_pre = max(1, int(round(majors[0] / first_step)))
        pre_lines = [majors[0] - first_step * s for s in range(k_pre, 0, -1)]
        pre_lines = [max(0.0, p) for p in pre_lines]
        majors = pre_lines + majors

    # 若末条红线距离右/下边框约 1 个大格，补上终点红线
    last_step = majors[-1] - majors[-2]
    rem_end = (span - 1) - majors[-1]
    if rem_end >= last_step * 0.85:
        k_post = int(round(rem_end / last_step))
        if k_post >= 1 and abs(rem_end / k_post - last_step) <= last_step * 0.35:
            for s in range(1, k_post + 1):
                majors.append(majors[-1] + rem_end / k_post)

    # 构建所有逐格边界 x_bounds / y_bounds
    bounds: list[float] = []

    # 开头不足 1 大格的残余格
    if majors[0] > (majors[1] - majors[0]) * 0.14:
        cw = (majors[1] - majors[0]) / cells_per_major
        rem = max(1, min(cells_per_major - 1, int(round(majors[0] / cw))))
        for s in range(rem):
            bounds.append(start_coord + majors[0] * (s / rem))

    # 完整大格区间
    for k in range(len(majors) - 1):
        a, b = majors[k], majors[k + 1]
        for s in range(cells_per_major):
            bounds.append(start_coord + a + (b - a) * (s / cells_per_major))
    bounds.append(start_coord + majors[-1])

    # 末尾不足 1 大格的残余格（优先检测区间内 5 格虚线以校准残余格宽）
    tail_px = (span - 1) - majors[-1]
    last_cw = (majors[-1] - majors[-2]) / cells_per_major
    if tail_px > last_cw * 0.8:
        # 检查末尾区间是否存在 5 格虚线峰值
        half_cells = cells_per_major // 2
        half_est = majors[-1] + last_cw * half_cells
        if half_cells >= 2 and half_est < span - 3:
            lo = max(int(majors[-1] + last_cw * (half_cells - 1.5)), 0)
            hi = min(int(majors[-1] + last_cw * (half_cells + 1.5)) + 1, span)
            if hi > lo and dark_sig[lo:hi].max() > 0.25:
                half_pos = float(lo + int(np.argmax(dark_sig[lo:hi])))
                refined_cw = (half_pos - majors[-1]) / half_cells
                if 0.65 * last_cw <= refined_cw <= 1.45 * last_cw:
                    last_cw = refined_cw
        rem = max(1, min(cells_per_major, int(round(tail_px / last_cw))))
        for s in range(1, rem + 1):
            bounds.append(start_coord + majors[-1] + tail_px * (s / rem))

    major_abs = [start_coord + m for m in majors]
    return major_abs, np.asarray(bounds, dtype=np.float64)


def _fallback_uniform_grid(
    rgb_f: np.ndarray,
    manual_cols: int | None,
    manual_rows: int | None,
) -> tuple[tuple[int, int, int, int], np.ndarray, np.ndarray]:
    """当图片无红线或极度模糊无法识别红线时，自动裁剪前景并用边缘自相关估计网格。"""
    h, w = rgb_f.shape[:2]
    gray = rgb_f.mean(axis=2)
    non_white = gray < 238
    ys, xs = np.where(non_white)
    if xs.size > 0 and ys.size > 0:
        gx0, gx1 = int(xs.min()), int(xs.max())
        gy0, gy1 = int(ys.min()), int(ys.max())
    else:
        gx0, gy0, gx1, gy1 = 0, 0, w - 1, h - 1

    sub_gray = gray[gy0 : gy1 + 1, gx0 : gx1 + 1]
    sw, sh = gx1 - gx0 + 1, gy1 - gy0 + 1

    def estimate_pitch(axis_grad: np.ndarray, max_dim: int) -> float:
        sig = axis_grad - axis_grad.mean()
        if np.std(sig) < 1e-4:
            return max(4.0, max_dim / 64.0)
        ac = np.correlate(sig, sig, mode="full")[len(sig) - 1 :]
        min_p = max(3, int(max_dim / 260))
        max_p = min(len(ac) - 1, max(min_p + 2, int(max_dim / 8)))
        if max_p <= min_p:
            return max(4.0, max_dim / 64.0)
        best_p = min_p + int(np.argmax(ac[min_p:max_p]))
        return float(best_p)

    if manual_cols and manual_cols > 0:
        cols = manual_cols
    else:
        gx = np.abs(np.diff(sub_gray, axis=1)).mean(axis=0)
        pitch_x = estimate_pitch(gx, sw)
        cols = max(4, min(320, int(round(sw / pitch_x))))

    if manual_rows and manual_rows > 0:
        rows = manual_rows
    else:
        gy = np.abs(np.diff(sub_gray, axis=0)).mean(axis=1)
        pitch_y = estimate_pitch(gy, sh)
        rows = max(4, min(320, int(round(sh / pitch_y))))

    x_bounds = np.linspace(float(gx0), float(gx1), cols + 1)
    y_bounds = np.linspace(float(gy0), float(gy1), rows + 1)
    return (gx0, gy0, gx1, gy1), x_bounds, y_bounds


def _sample_cells_medoid(
    rgb_f: np.ndarray,
    x_bounds: np.ndarray,
    y_bounds: np.ndarray,
) -> np.ndarray:
    """
    在每个格子内部避开边缘网格线与中心色号文字区域，进行 12 点子像素环形采样，
    并取最紧密的 6-NN Medoid 均值，极大降低色号文字与模糊压缩带来的偏色。
    """
    ncols = len(x_bounds) - 1
    nrows = len(y_bounds) - 1
    pts_uv = [
        (0.24, 0.24),
        (0.50, 0.20),
        (0.76, 0.24),
        (0.20, 0.50),
        (0.80, 0.50),
        (0.24, 0.76),
        (0.50, 0.80),
        (0.76, 0.76),
        (0.32, 0.28),
        (0.68, 0.28),
        (0.32, 0.72),
        (0.68, 0.72),
    ]
    k_pts = len(pts_uv)
    xc = x_bounds[:-1]
    xw = np.diff(x_bounds)
    yc = y_bounds[:-1]
    yw = np.diff(y_bounds)

    samples = np.zeros((nrows, ncols, k_pts, 3), dtype=np.float32)
    for k, (u, v) in enumerate(pts_uv):
        mx = np.broadcast_to((xc + u * xw)[None, :], (nrows, ncols)).astype(np.float32)
        my = np.broadcast_to((yc + v * yw)[:, None], (nrows, ncols)).astype(np.float32)
        samples[:, :, k, :] = cv2.remap(
            rgb_f, mx, my, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE
        )

    diff = samples[:, :, :, None, :] - samples[:, :, None, :, :]
    dist_sq = (diff * diff).sum(axis=-1)
    sorted_d = np.sort(dist_sq, axis=-1)
    best_k = np.argmin(sorted_d[:, :, :, :6].sum(axis=-1), axis=-1)

    r_idx = np.arange(nrows)[:, None]
    c_idx = np.arange(ncols)[None, :]
    best_dists = dist_sq[r_idx, c_idx, best_k, :]
    nn6_idx = np.argpartition(best_dists, 6, axis=-1)[:, :, :6]
    gathered = np.take_along_axis(samples, nn6_idx[:, :, :, None], axis=2)
    return gathered.mean(axis=2)


def _classify_white_like(cell_colors: np.ndarray, white_threshold: int) -> np.ndarray:
    """识别外观接近白色的格子（纯白格或被模糊红线轻微晕染的白格；肤色等浅色色块彩度通常 >= 30）。"""
    c_min = cell_colors.min(axis=2)
    c_max = cell_colors.max(axis=2)
    c_mean = cell_colors.mean(axis=2)
    chroma = c_max - c_min
    return (
        ((c_min >= white_threshold) & (chroma <= 18.0))
        | ((c_mean >= max(white_threshold + 1, 229)) & (chroma <= 26.0) & (c_min >= 215))
    )


def _detect_dark_text_cells(
    rgb_f: np.ndarray,
    x_bounds: np.ndarray,
    y_bounds: np.ndarray,
    candidate_mask: np.ndarray,
    dip_threshold: float = 32.0,
) -> np.ndarray:
    """
    在候选白色格子内检测是否存在居中的黑色/深色色号文字（代表需填充白色/浅色拼豆，而非空白空气）。
    使用 HSV 明度通道 max(R, G, B) 天然忽略红色 10x10 大格线，并通过「上下亮边夹中间暗谷」
    的垂直对比度 dip = min(top, bot) - mid 排除相邻黑色描边单侧渗入造成的误判。
    """
    nrows, ncols = candidate_mask.shape
    has_text = np.zeros((nrows, ncols), dtype=bool)
    if not candidate_mask.any():
        return has_text

    val = rgb_f.max(axis=2).astype(np.float32)
    xc = x_bounds[:-1]
    xw = np.diff(x_bounds)
    yc = y_bounds[:-1]
    yw = np.diff(y_bounds)
    med_cw = float(np.median(xw))
    med_ch = float(np.median(yw))

    if med_cw < 12.0 or med_ch < 12.0:
        # 低分辨率小格（如 5~11px）：亚像素双线性重采样，保留精确对齐
        us = np.linspace(0.18, 0.82, 11, dtype=np.float32)
        vs_top = np.array([0.12, 0.20, 0.27], dtype=np.float32)
        vs_mid = np.array([0.34, 0.42, 0.50, 0.58, 0.66], dtype=np.float32)
        vs_bot = np.array([0.73, 0.80, 0.88], dtype=np.float32)

        def sample_band(vs: np.ndarray) -> np.ndarray:
            out = np.empty((nrows, ncols, len(vs), len(us)), dtype=np.float32)
            for iv, v in enumerate(vs):
                my = np.broadcast_to((yc + v * yw)[:, None], (nrows, ncols)).astype(np.float32)
                for iu, u in enumerate(us):
                    mx = np.broadcast_to((xc + u * xw)[None, :], (nrows, ncols)).astype(np.float32)
                    out[:, :, iv, iu] = cv2.remap(
                        val, mx, my, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE
                    )
            return out

        top = sample_band(vs_top).max(axis=2)
        mid = sample_band(vs_mid).min(axis=2)
        bot = sample_band(vs_bot).max(axis=2)
        dip = np.minimum(top, bot) - mid
        top2_mean = np.sort(dip, axis=2)[:, :, -2:].mean(axis=2)
        has_text = candidate_mask & (top2_mean > dip_threshold)
    else:
        # 中高分辨率格子（>= 12px）：逐候选格直接切片检查全部像素，避免漏掉 1px 细笔画字体
        h_img, w_img = val.shape
        for r, c in np.argwhere(candidate_mask):
            x0, x1 = float(x_bounds[c]), float(x_bounds[c + 1])
            y0, y1 = float(y_bounds[r]), float(y_bounds[r + 1])
            cw, ch = x1 - x0, y1 - y0
            ix0 = max(0, int(np.floor(x0 + cw * 0.16)))
            ix1 = min(w_img, int(np.ceil(x0 + cw * 0.84)))
            t0 = max(int(np.floor(y0)) + 1, int(np.floor(y0 + ch * 0.08)))
            t1 = min(h_img, int(np.ceil(y0 + ch * 0.28)))
            m0 = max(0, int(np.floor(y0 + ch * 0.30)))
            m1 = min(h_img, int(np.ceil(y0 + ch * 0.70)))
            b0 = max(0, int(np.floor(y0 + ch * 0.72)))
            b1 = min(int(np.ceil(y1)) - 1, int(np.ceil(y0 + ch * 0.92)))
            t0 = max(0, t0)
            b1 = min(h_img, b1)
            if ix1 <= ix0 or t1 <= t0 or m1 <= m0 or b1 <= b0:
                continue
            top = val[t0:t1, ix0:ix1].max(axis=0)
            mid = val[m0:m1, ix0:ix1].min(axis=0)
            bot = val[b0:b1, ix0:ix1].max(axis=0)
            dip = np.minimum(top, bot) - mid
            if dip.size >= 2:
                score = float(np.partition(dip, -2)[-2:].mean())
            else:
                score = float(dip[0])
            if score > dip_threshold:
                has_text[r, c] = True

    return has_text


def _compute_foreground_mask(
    cell_colors: np.ndarray,
    white_bg_mode: Literal["all_white", "outer_only"],
    white_threshold: int,
    clean_isolated_specks: bool,
    is_white_like: np.ndarray | None = None,
    has_white_text: np.ndarray | None = None,
) -> np.ndarray:
    """区分有效色块与空白背景（空气）；带有黑色色号文字的白色格子将作为有效白色方块保留。"""
    if is_white_like is None:
        is_white_like = _classify_white_like(cell_colors, white_threshold)

    is_empty_bg = is_white_like & (~has_white_text) if has_white_text is not None else is_white_like

    if white_bg_mode == "all_white":
        mask = ~is_empty_bg
    else:
        # 仅将与图像四边连通的无字白色背景视为空白，保留内部无字白色色块（有字白色格始终保留）
        nrows, ncols = is_empty_bg.shape
        visited = np.zeros((nrows, ncols), dtype=bool)
        q: deque[tuple[int, int]] = deque()
        for c in range(ncols):
            if is_empty_bg[0, c]:
                visited[0, c] = True
                q.append((0, c))
            if is_empty_bg[nrows - 1, c] and not visited[nrows - 1, c]:
                visited[nrows - 1, c] = True
                q.append((nrows - 1, c))
        for r in range(nrows):
            if is_empty_bg[r, 0] and not visited[r, 0]:
                visited[r, 0] = True
                q.append((r, 0))
            if is_empty_bg[r, ncols - 1] and not visited[r, ncols - 1]:
                visited[r, ncols - 1] = True
                q.append((r, ncols - 1))

        while q:
            r, c = q.popleft()
            for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                nr, nc = r + dr, c + dc
                if 0 <= nr < nrows and 0 <= nc < ncols and not visited[nr, nc] and is_empty_bg[nr, nc]:
                    visited[nr, nc] = True
                    q.append((nr, nc))
        mask = ~visited

    if clean_isolated_specks and mask.any():
        # 物理拼豆中孤立悬浮（8邻域全为空气）的 1x1 像素几乎必为红线交点/水印噪点
        kernel = np.array([[1, 1, 1], [1, 0, 1], [1, 1, 1]], dtype=np.uint8)
        neighbor_count = cv2.filter2D(mask.astype(np.uint8), -1, kernel, borderType=cv2.BORDER_CONSTANT)
        mask = mask & (neighbor_count > 0)

    return mask


def _purify_color_subset(
    pts_rgb: np.ndarray,
    merge_delta_e: float,
    min_cluster_cnt: int,
) -> np.ndarray:
    """在 CIE L*a*b* 空间对一组前景格做原色归并聚类，返回对应 uint8 RGB 数组。"""
    if merge_delta_e <= 0.5 or pts_rgb.shape[0] <= 1:
        return np.clip(np.round(pts_rgb), 0, 255).astype(np.uint8)

    nb_lab = rgb_to_cielab(pts_rgb)
    bin_step = max(1.5, merge_delta_e * 0.35)
    q = np.round(nb_lab / bin_step).astype(np.int32)

    bins: dict[tuple[int, int, int], list] = {}
    for idx in range(len(pts_rgb)):
        key = (int(q[idx, 0]), int(q[idx, 1]), int(q[idx, 2]))
        entry = bins.get(key)
        if entry is None:
            bins[key] = [1, pts_rgb[idx].copy(), nb_lab[idx].copy()]
        else:
            entry[0] += 1
            entry[1] += pts_rgb[idx]
            entry[2] += nb_lab[idx]

    centers_lab: list[np.ndarray] = []
    centers_rgb: list[np.ndarray] = []

    for cnt, srgb, slab in sorted(bins.values(), key=lambda x: -x[0]):
        if cnt < min_cluster_cnt and len(centers_lab) > 0:
            break
        mlab = slab / cnt
        if all(float(np.linalg.norm(mlab - c)) > merge_delta_e for c in centers_lab):
            centers_lab.append(mlab)
            centers_rgb.append(srgb / cnt)

    if not centers_lab:
        return np.clip(np.round(pts_rgb), 0, 255).astype(np.uint8)

    c_lab_arr = np.asarray(centers_lab, dtype=np.float32)
    dists = np.linalg.norm(nb_lab[:, None, :] - c_lab_arr[None, :, :], axis=2)
    labels = np.argmin(dists, axis=1)

    # 每个色簇取中位数 RGB，忠实还原该拼豆色号的原色
    final_rgb = np.zeros((len(centers_rgb), 3), dtype=np.uint8)
    for k in range(len(centers_rgb)):
        pts = pts_rgb[labels == k]
        if pts.shape[0] > 0:
            final_rgb[k] = np.clip(np.round(np.median(pts, axis=0)), 0, 255).astype(np.uint8)
        else:
            final_rgb[k] = np.clip(np.round(centers_rgb[k]), 0, 255).astype(np.uint8)

    return final_rgb[labels]


def _purify_original_colors(
    cell_colors: np.ndarray,
    mask: np.ndarray,
    merge_delta_e: float,
    white_mask: np.ndarray | None = None,
) -> np.ndarray:
    """
    在标准 CIE L*a*b* 空间对模糊/JPEG 压缩引起的杂色做原色归并聚类，
    保留拼豆原图原本的真实 RGB 色彩（支持 merge_delta_e <= 0 时保留逐格原始颜色）。
    当存在需保留的白色/近白色拼豆格时单独归并，防止白色格被合并进浅肤色或浅灰等邻近浅色。
    """
    out = np.full(cell_colors.shape, 255, dtype=np.uint8)
    if not mask.any():
        return out

    white_fg = (mask & white_mask) if white_mask is not None else np.zeros_like(mask)
    colored_fg = mask & (~white_fg)

    if colored_fg.any():
        colored_pts = cell_colors[colored_fg]
        min_cnt = max(4, int(len(colored_pts) * 0.0012))
        out[colored_fg] = _purify_color_subset(colored_pts, merge_delta_e, min_cnt)

    if white_fg.any():
        white_pts = cell_colors[white_fg]
        white_de = min(merge_delta_e, 4.0) if merge_delta_e > 0.5 else merge_delta_e
        out[white_fg] = _purify_color_subset(white_pts, white_de, min_cluster_cnt=1)

    return out


def compute_color_stats(grid_rgb: np.ndarray, mask: np.ndarray) -> list[tuple[int, int]]:
    """统计每个精确 RGB 颜色 (0xRRGGBB) 对应的彩色方块数量，按数量降序排列。"""
    if not mask.any():
        return []
    rgbs = grid_rgb[mask]
    ints = (
        (rgbs[:, 0].astype(np.int32) << 16)
        | (rgbs[:, 1].astype(np.int32) << 8)
        | rgbs[:, 2].astype(np.int32)
    )
    vals, cnts = np.unique(ints, return_counts=True)
    pairs = [(int(v), int(c)) for v, c in zip(vals.tolist(), cnts.tolist())]
    pairs.sort(key=lambda x: (-x[1], x[0]))
    return pairs


def parse_bead_image(
    image_or_path: str | Path | np.ndarray,
    options: ParseOptions | None = None,
) -> BeadPatternResult:
    """解析拼豆图纸图片，返回网格、原色阵列与统计信息。"""
    opts = options or ParseOptions()
    rgb = load_image_rgb(image_or_path) if isinstance(image_or_path, (str, Path)) else image_or_path.copy()
    h, w = rgb.shape[:2]
    rgb_f = rgb.astype(np.float32)

    maps = _compute_line_maps(rgb_f)
    bbox = _detect_grid_bbox(maps, w, h)

    x_major: list[float] = []
    y_major: list[float] = []
    x_bounds: np.ndarray | None = None
    y_bounds: np.ndarray | None = None
    method = "red_grid"

    if bbox is not None and opts.manual_cols is None and opts.manual_rows is None:
        gx0, gy0, gx1, gy1 = bbox
        sub_nw = maps["near_white"][gy0 : gy1 + 1, gx0 : gx1 + 1]
        sub_vx = maps["vx"][gy0 : gy1 + 1, gx0 : gx1 + 1]
        sub_hy = maps["hy"][gy0 : gy1 + 1, gx0 : gx1 + 1]
        sub_vd = maps["v_dark"][gy0 : gy1 + 1, gx0 : gx1 + 1]
        sub_hd = maps["h_dark"][gy0 : gy1 + 1, gx0 : gx1 + 1]

        col_red = np.where(sub_nw, np.maximum(0.0, sub_vx), np.maximum(0.0, sub_vx) * 0.4).mean(axis=0)
        row_red = np.where(sub_nw, np.maximum(0.0, sub_hy), np.maximum(0.0, sub_hy) * 0.4).mean(axis=1)
        col_dark = (sub_nw & (sub_vd > 8)).mean(axis=0)
        row_dark = (sub_nw & (sub_hd > 8)).mean(axis=1)

        res_x = _extract_major_chain(col_red, col_dark, gx0, gx1, opts.cells_per_major)
        res_y = _extract_major_chain(row_red, row_dark, gy0, gy1, opts.cells_per_major)

        if res_x is not None and res_y is not None:
            x_major, x_bounds = res_x
            y_major, y_bounds = res_y

    if x_bounds is None or y_bounds is None:
        if bbox is not None and (opts.manual_cols or opts.manual_rows):
            gx0, gy0, gx1, gy1 = bbox
            cols = opts.manual_cols or max(4, int(round((gx1 - gx0) / 6.0)))
            rows = opts.manual_rows or max(4, int(round((gy1 - gy0) / 6.0)))
            x_bounds = np.linspace(float(gx0), float(gx1), cols + 1)
            y_bounds = np.linspace(float(gy0), float(gy1), rows + 1)
            method = "manual_in_bbox"
        else:
            bbox, x_bounds, y_bounds = _fallback_uniform_grid(
                rgb_f, opts.manual_cols, opts.manual_rows
            )
            method = "fallback_auto" if (opts.manual_cols is None and opts.manual_rows is None) else "manual_uniform"

    cell_colors = _sample_cells_medoid(rgb_f, x_bounds, y_bounds)
    is_white_like = _classify_white_like(cell_colors, opts.white_threshold)
    has_white_text = (
        _detect_dark_text_cells(rgb_f, x_bounds, y_bounds, is_white_like)
        if opts.detect_white_text
        else None
    )
    mask = _compute_foreground_mask(
        cell_colors,
        white_bg_mode=opts.white_bg_mode,
        white_threshold=opts.white_threshold,
        clean_isolated_specks=opts.clean_isolated_specks,
        is_white_like=is_white_like,
        has_white_text=has_white_text,
    )
    grid_rgb = _purify_original_colors(
        cell_colors, mask, opts.merge_delta_e, white_mask=is_white_like
    )
    stats = compute_color_stats(grid_rgb, mask)

    method_label = {
        "red_grid": "红线大格自动识别",
        "fallback_auto": "模糊/无红线自动估格",
        "manual_in_bbox": "手动网格尺寸(自动框选)",
        "manual_uniform": "手动网格尺寸",
    }.get(method, method)

    note = (
        f"{method_label}: {grid_rgb.shape[1]} × {grid_rgb.shape[0]} 格 | "
        f"共 {len(stats)} 种原色 | {int(mask.sum()):,} 个彩色方块"
    )
    return BeadPatternResult(
        source_rgb=rgb,
        grid_rgb=grid_rgb,
        mask=mask,
        bbox=bbox,
        x_major=x_major,
        y_major=y_major,
        x_bounds=x_bounds,
        y_bounds=y_bounds,
        color_stats=stats,
        method=method,
        note=note,
    )


def render_grid_overlay(result: BeadPatternResult) -> np.ndarray:
    """在原图上绘制识别到的网格框与红色 10x10 大格线，方便玩家核对对齐情况。"""
    vis = result.source_rgb.copy()
    gx0, gy0, gx1, gy1 = result.bbox
    # 青色外框
    cv2.rectangle(vis, (gx0, gy0), (gx1, gy1), (0, 220, 255), 2)
    # 高亮识别到的红线大格（亮绿色虚/细线便于与原图红线区分核对）
    for xm in result.x_major:
        ix = int(round(xm))
        cv2.line(vis, (ix, gy0), (ix, gy1), (0, 255, 120), 1)
    for ym in result.y_major:
        iy = int(round(ym))
        cv2.line(vis, (gx0, iy), (gx1, iy), (0, 255, 120), 1)
    return vis


def render_pixel_preview(result: BeadPatternResult, bg_rgb: tuple[int, int, int] = (24, 26, 36)) -> np.ndarray:
    """渲染彩色方块原色像素化预览图（含浅色棋盘背景以区分白色方块与空气）。"""
    rows, cols = result.rows, result.cols
    out = np.empty((rows, cols, 3), dtype=np.uint8)
    # 棋盘底色表示空气
    rr, cc = np.indices((rows, cols))
    checker = ((rr // 2 + cc // 2) % 2) == 0
    bg_a = np.array(bg_rgb, dtype=np.uint8)
    bg_b = np.clip(bg_a.astype(np.int16) + 12, 0, 255).astype(np.uint8)
    out[checker] = bg_a
    out[~checker] = bg_b
    out[result.mask] = result.grid_rgb[result.mask]
    return out
