"""
纯 Python Litematica (.litematic) NBT 读写模块。
兼容 tuanzis_mod 蓝图桌 (BlueprintTableScreen) 与蓝图大炮 (BlueprintCannonBlockEntity)，
使用 tuanzis_mod:color_block 及其 BlockEntity (tuanzis_mod:color_block_be) 的 Color 字段实现全真原色还原。
"""

from __future__ import annotations

import gzip
import math
import struct
import time
from io import BytesIO
from pathlib import Path
from typing import Any, Literal

import numpy as np

# NBT Tag Type IDs
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

COLOR_BLOCK_ID = "tuanzis_mod:color_block"
COLOR_BLOCK_BE_ID = "tuanzis_mod:color_block_be"
MINECRAFT_DATA_VERSION = 3953


class NbtWriter:
    """标准 Big-Endian Java NBT 二进制写入器。"""

    def __init__(self) -> None:
        self.buf = BytesIO()

    def _write_str(self, s: str) -> None:
        raw = s.encode("utf-8")
        self.buf.write(struct.pack(">H", len(raw)))
        self.buf.write(raw)

    def _write_tag_header(self, tag_type: int, name: str) -> None:
        self.buf.write(struct.pack(">B", tag_type))
        self._write_str(name)

    def write_int(self, name: str, value: int) -> None:
        self._write_tag_header(TAG_INT, name)
        self.buf.write(struct.pack(">i", int(value)))

    def write_long(self, name: str, value: int) -> None:
        self._write_tag_header(TAG_LONG, name)
        self.buf.write(struct.pack(">q", int(value)))

    def write_string(self, name: str, value: str) -> None:
        self._write_tag_header(TAG_STRING, name)
        self._write_str(value)

    def write_long_array(self, name: str, values: np.ndarray | list[int]) -> None:
        self._write_tag_header(TAG_LONG_ARRAY, name)
        arr = np.asarray(values, dtype=">i8")
        self.buf.write(struct.pack(">i", len(arr)))
        if len(arr) > 0:
            self.buf.write(arr.tobytes())

    def begin_compound(self, name: str) -> None:
        self._write_tag_header(TAG_COMPOUND, name)

    def end_compound(self) -> None:
        self.buf.write(struct.pack(">B", TAG_END))

    def begin_list(self, name: str, elem_type: int, count: int) -> None:
        self._write_tag_header(TAG_LIST, name)
        self.buf.write(struct.pack(">Bi", elem_type, count))

    def write_payload_compound_end(self) -> None:
        self.buf.write(struct.pack(">B", TAG_END))

    def getvalue(self) -> bytes:
        return self.buf.getvalue()


def pack_litematica_block_states(block_indices: np.ndarray, palette_size: int) -> np.ndarray:
    """
    按 Litematica 规范将方块索引数组位打包为有符号 64-bit long 数组。
    与 LitematicaParser.java#packLitematic / parseLitematic 完全一致：
    bits = max(2, ceil(log2(palette_size)))，低位优先连续跨 long 拼接。
    """
    total = int(block_indices.size)
    if palette_size <= 1 or total == 0:
        return np.zeros(0, dtype=np.int64)

    bits = max(2, int(math.ceil(math.log2(palette_size))))
    longs_count = (total * bits + 63) // 64
    out = np.zeros(longs_count, dtype=np.uint64)

    # 按位索引向量化打包，避免 Python 逐格循环瓶颈
    flat = np.asarray(block_indices, dtype=np.uint64).ravel()
    nz = np.nonzero(flat)[0]
    if nz.size == 0:
        return out.view(np.int64)

    bit_offsets = nz.astype(np.uint64) * np.uint64(bits)
    long_idxs = (bit_offsets >> np.uint64(6)).astype(np.intp)
    bit_shifts = bit_offsets & np.uint64(63)
    vals = flat[nz]

    # 当 bits == 2 时（本项目固定调色板大小 2：air + color_block），每个值永不跨 64 位边界
    if bits == 2:
        np.bitwise_or.at(out, long_idxs, vals << bit_shifts)
    else:
        for i in range(nz.size):
            li = int(long_idxs[i])
            sh = int(bit_shifts[i])
            v = int(vals[i])
            out[li] |= np.uint64((v << sh) & 0xFFFFFFFFFFFFFFFF)
            if sh + bits > 64 and li + 1 < longs_count:
                out[li + 1] |= np.uint64(v >> (64 - sh))

    return out.view(np.int64)


def build_voxel_grid_from_beads(
    grid_rgb: np.ndarray,
    mask: np.ndarray,
    orientation: Literal["xy", "xz"] = "xy",
) -> tuple[int, int, int, np.ndarray, list[tuple[int, int, int, int]]]:
    """
    将 2D 拼豆网格 (rows, cols) 转换为 3D 蓝图体素数据。

    返回:
      (width, height, length, blocks_flat, tile_entities)
      - blocks_flat: 一维 int32 数组，索引公式 y * width * length + z * width + x，0=air, 1=color_block
      - tile_entities: [(x, y, z, color_rgb_int), ...]
    """
    rows, cols = mask.shape
    if orientation == "xy":
        # 竖着放置：X=列(左->右), Y=行(底->顶, row=0在最顶), Z=0
        width, height, length = cols, rows, 1
    elif orientation == "xz":
        # 平铺放置：X=列(左->右), Y=0, Z=行(北->南, row=0在z=0)
        width, height, length = cols, 1, rows
    else:
        raise ValueError(f"不支持的方向格式: {orientation}")

    total_voxels = width * height * length
    blocks = np.zeros(total_voxels, dtype=np.int32)
    tile_entities: list[tuple[int, int, int, int]] = []

    rs, cs = np.nonzero(mask)
    if rs.size > 0:
        if orientation == "xy":
            xs = cs
            ys = (rows - 1) - rs
            zs = np.zeros_like(xs)
        else:
            xs = cs
            ys = np.zeros_like(xs)
            zs = rs

        flat_idxs = ys * (width * length) + zs * width + xs
        blocks[flat_idxs] = 1

        rgbs = grid_rgb[rs, cs]
        color_ints = (
            (rgbs[:, 0].astype(np.int32) << 16)
            | (rgbs[:, 1].astype(np.int32) << 8)
            | rgbs[:, 2].astype(np.int32)
        )
        for x, y, z, col_int in zip(xs.tolist(), ys.tolist(), zs.tolist(), color_ints.tolist()):
            tile_entities.append((x, y, z, col_int))

    return width, height, length, blocks, tile_entities


def export_litematic(
    output_path: str | Path,
    grid_rgb: np.ndarray,
    mask: np.ndarray,
    orientation: Literal["xy", "xz"] = "xy",
    blueprint_name: str = "bead_blueprint",
    author: str = "Tuanzi Bead Tool",
) -> dict[str, Any]:
    """
    将拼豆网格导出为 tuanzis_mod 蓝图桌 / 蓝图大炮可直接导入的 .litematic 文件。
    所有非空色块统一使用 tuanzis_mod:color_block，并通过 TileEntities 写入精确原色 Color (0xRRGGBB)。
    """
    width, height, length, blocks, tile_entities = build_voxel_grid_from_beads(
        grid_rgb, mask, orientation=orientation
    )
    packed_states = pack_litematica_block_states(blocks, palette_size=2)
    now_ms = int(time.time() * 1000)

    w = NbtWriter()
    w.begin_compound("")  # Root compound

    w.write_int("Version", 5)
    w.write_int("SubVersion", 1)
    w.write_int("MinecraftDataVersion", MINECRAFT_DATA_VERSION)

    # Metadata
    w.begin_compound("Metadata")
    w.write_string("Name", blueprint_name)
    w.write_string("Author", author)
    w.write_string("Description", f"Bead pattern ({width}x{height}x{length}, {orientation.upper()})")
    w.write_int("RegionCount", 1)
    w.write_long("TimeCreated", now_ms)
    w.write_long("TimeModified", now_ms)
    w.write_int("TotalBlocks", len(tile_entities))
    w.write_int("TotalVolume", width * height * length)
    w.begin_compound("EnclosingSize")
    w.write_int("x", width)
    w.write_int("y", height)
    w.write_int("z", length)
    w.end_compound()
    w.end_compound()

    # Regions -> Region
    w.begin_compound("Regions")
    w.begin_compound("Region")

    w.begin_compound("Position")
    w.write_int("x", 0)
    w.write_int("y", 0)
    w.write_int("z", 0)
    w.end_compound()

    w.begin_compound("Size")
    w.write_int("x", width)
    w.write_int("y", height)
    w.write_int("z", length)
    w.end_compound()

    # BlockStatePalette: [0]=minecraft:air, [1]=tuanzis_mod:color_block
    w.begin_list("BlockStatePalette", TAG_COMPOUND, 2)
    w.write_string("Name", "minecraft:air")
    w.write_payload_compound_end()
    w.write_string("Name", COLOR_BLOCK_ID)
    w.write_payload_compound_end()

    # BlockStates
    w.write_long_array("BlockStates", packed_states)

    # TileEntities: 每个彩色方块写入精确 RGB 颜色 Color
    w.begin_list("TileEntities", TAG_COMPOUND, len(tile_entities))
    for x, y, z, color_int in tile_entities:
        w.write_string("id", COLOR_BLOCK_BE_ID)
        w.write_int("x", x)
        w.write_int("y", y)
        w.write_int("z", z)
        w.write_int("Color", color_int)
        w.write_payload_compound_end()

    # 空列表字段（与 Litematica 标准保持一致）
    w.begin_list("Entities", TAG_COMPOUND, 0)
    w.begin_list("PendingBlockTicks", TAG_COMPOUND, 0)
    w.begin_list("PendingFluidTicks", TAG_COMPOUND, 0)

    w.end_compound()  # Region
    w.end_compound()  # Regions
    w.end_compound()  # Root

    out_path = Path(output_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(out_path, "wb", compresslevel=6) as f:
        f.write(w.getvalue())

    return {
        "path": str(out_path),
        "width": width,
        "height": height,
        "length": length,
        "total_blocks": len(tile_entities),
        "orientation": orientation,
    }


# ---------- 轻量 NBT 读取器（用于单元测试与格式自校验） ----------

def read_nbt_gzip(path: str | Path) -> dict[str, Any]:
    with gzip.open(path, "rb") as f:
        data = f.read()
    stream = BytesIO(data)
    tag_type = struct.unpack(">B", stream.read(1))[0]
    assert tag_type == TAG_COMPOUND, f"Unexpected root tag type: {tag_type}"
    _read_str(stream)
    return _read_compound(stream)


def _read_str(stream: BytesIO) -> str:
    n = struct.unpack(">H", stream.read(2))[0]
    return stream.read(n).decode("utf-8")


def _read_payload(stream: BytesIO, tag_type: int) -> Any:
    if tag_type == TAG_BYTE:
        return struct.unpack(">b", stream.read(1))[0]
    if tag_type == TAG_SHORT:
        return struct.unpack(">h", stream.read(2))[0]
    if tag_type == TAG_INT:
        return struct.unpack(">i", stream.read(4))[0]
    if tag_type == TAG_LONG:
        return struct.unpack(">q", stream.read(8))[0]
    if tag_type == TAG_FLOAT:
        return struct.unpack(">f", stream.read(4))[0]
    if tag_type == TAG_DOUBLE:
        return struct.unpack(">d", stream.read(8))[0]
    if tag_type == TAG_BYTE_ARRAY:
        n = struct.unpack(">i", stream.read(4))[0]
        return stream.read(n)
    if tag_type == TAG_STRING:
        return _read_str(stream)
    if tag_type == TAG_LIST:
        elem_type, n = struct.unpack(">Bi", stream.read(5))
        return [_read_payload(stream, elem_type) for _ in range(n)]
    if tag_type == TAG_COMPOUND:
        return _read_compound(stream)
    if tag_type == TAG_INT_ARRAY:
        n = struct.unpack(">i", stream.read(4))[0]
        return list(struct.unpack(f">{n}i", stream.read(4 * n)))
    if tag_type == TAG_LONG_ARRAY:
        n = struct.unpack(">i", stream.read(4))[0]
        return np.frombuffer(stream.read(8 * n), dtype=">i8").astype(np.int64)
    raise ValueError(f"Unsupported NBT tag type: {tag_type}")


def _read_compound(stream: BytesIO) -> dict[str, Any]:
    res: dict[str, Any] = {}
    while True:
        tag_type = struct.unpack(">B", stream.read(1))[0]
        if tag_type == TAG_END:
            return res
        name = _read_str(stream)
        res[name] = _read_payload(stream, tag_type)
