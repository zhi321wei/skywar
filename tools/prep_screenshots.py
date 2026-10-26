# -*- coding: utf-8 -*-
"""
TapTap 截图预处理工具。

用途：把手机直出的截图整理成商店可用的规格——去掉系统状态栏/导航栏、统一宽度、可选裁成固定比例。

用法（在 skywar 目录下）：

    # 1) 先看一眼每张图的实际尺寸与「疑似状态栏高度」
    python tools/prep_screenshots.py --probe raw/*.png

    # 2) 直接处理（默认去掉顶部 0.9%、底部 1.2% 的系统栏，宽度统一到 1080）
    python tools/prep_screenshots.py raw/*.png

    # 3) 精确指定裁切像素（probe 结果里的建议值）
    python tools/prep_screenshots.py raw/*.png --top 96 --bottom 132

    # 4) 强制裁成 9:16（1:1 封面、部分平台的视频首帧位会用到）
    python tools/prep_screenshots.py raw/*.png --fit 9:16

输出目录默认 dist/taptap/screenshots/，文件名沿用原名并加 -1080x1920 后缀。
"""

import argparse
import os
import sys

from PIL import Image


def probe(path):
    """打印尺寸并估算状态栏/导航栏高度。"""
    im = Image.open(path)
    w, h = im.size
    ratio = w / h
    # 常见竖屏截图比例
    guess = "9:16" if abs(ratio - 9 / 16) < 0.02 else (
        "9:18" if abs(ratio - 9 / 18) < 0.03 else (
            "9:19.5" if abs(ratio - 9 / 19.5) < 0.03 else (
                "9:20" if abs(ratio - 9 / 20) < 0.03 else (
                    "9:21" if abs(ratio - 9 / 21) < 0.04 else "未知"))))
    # 启发式：状态栏约占高度 1.0%（1080x2400 约 96px），导航栏约 1.4%
    top_est = int(round(h * 0.010))
    bot_est = int(round(h * 0.014))
    print("%-34s %4dx%-4d  ratio %.3f  ~%s   建议 --top %d --bottom %d"
          % (os.path.basename(path), w, h, ratio, guess, top_est, bot_est))
    return im


def convert(path, out_dir, top, bottom, width, fit):
    im = Image.open(path)
    if im.mode in ("RGBA", "LA", "P"):
        im = im.convert("RGBA")
        bg = Image.new("RGB", im.size, (0, 0, 0))
        bg.paste(im, mask=im.split()[-1])
        im = bg
    else:
        im = im.convert("RGB")

    w, h = im.size

    # 1) 去掉系统栏
    t = int(top) if top is not None else int(round(h * 0.010))
    b = int(bottom) if bottom is not None else int(round(h * 0.014))
    if t + b >= h:
        raise SystemExit("裁切参数过大：top=%d bottom=%d 高度=%d" % (t, b, h))
    if t or b:
        im = im.crop((0, t, w, h - b))
        w, h = im.size

    # 2) 缩放到目标宽度
    if width and w != width:
        nh = int(round(h * width / float(w)))
        im = im.resize((width, nh), Image.LANCZOS)
        w, h = im.size

    # 3) 可选：裁成固定比例（居中，向上偏一点，避免裁掉战机）
    if fit and fit != "keep":
        rw, rh = [int(x) for x in fit.split(":")]
        target = rw / float(rh)
        cur = w / float(h)
        if abs(cur - target) < 0.005:
            pass
        elif cur > target:
            nw = int(round(h * target))
            x0 = (w - nw) // 2
            im = im.crop((x0, 0, x0 + nw, h))
        else:
            nh = int(round(w / target))
            # 向上偏 35%：保留上部 HUD 与战机，裁掉底部较多
            y0 = int(round((h - nh) * 0.35))
            y0 = max(0, min(y0, h - nh))
            im = im.crop((0, y0, w, y0 + nh))
        w, h = im.size

    os.makedirs(out_dir, exist_ok=True)
    base = os.path.splitext(os.path.basename(path))[0]
    out = os.path.join(out_dir, "%s-%dx%d.png" % (base, w, h))
    im.save(out, optimize=True)
    print("  -> %s  (%dx%d, %.0f KB)" % (out, w, h, os.path.getsize(out) / 1024.0))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("inputs", nargs="+", help="截图文件或目录")
    ap.add_argument("--out", default=os.path.join("dist", "taptap", "screenshots"))
    ap.add_argument("--top", type=int, default=None, help="裁掉顶部像素（状态栏）")
    ap.add_argument("--bottom", type=int, default=None, help="裁掉底部像素（导航栏）")
    ap.add_argument("--width", type=int, default=1080, help="输出宽度，0 表示不缩放")
    ap.add_argument("--fit", default="keep", help="keep / 9:16 / 1:1 / 16:9")
    ap.add_argument("--probe", action="store_true", help="只打印尺寸与建议值，不写文件")
    args = ap.parse_args()

    files = []
    for p in args.inputs:
        if os.path.isdir(p):
            for f in sorted(os.listdir(p)):
                if f.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
                    files.append(os.path.join(p, f))
        elif os.path.isfile(p):
            files.append(p)
        else:
            print("跳过（不存在）:", p, file=sys.stderr)

    if not files:
        raise SystemExit("没有找到可处理的图片")

    print("共 %d 张" % len(files))
    if args.probe:
        for f in files:
            probe(f)
        return

    for f in files:
        print(os.path.basename(f))
        convert(f, args.out, args.top, args.bottom, args.width, args.fit)
    print("\n输出目录：%s" % args.out)


if __name__ == "__main__":
    main()
