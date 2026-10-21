# -*- coding: utf-8 -*-
"""生成 SkyWar 的启动图标（多密度 PNG）"""
import os, math
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "res")
S = 1024
SS = 2  # supersample factor on top of S

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))

def make_icon():
    W = S * SS
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # ---- 背景：深空渐变圆角方
    bg = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bg)
    for y in range(W):
        t = y / (W - 1)
        c = lerp((10, 16, 38), (4, 32, 58), t)
        bd.line([(0, y), (W, y)], fill=c)
    # 星点
    import random
    random.seed(7)
    for _ in range(260):
        x = random.randint(0, W - 1); y = random.randint(0, W - 1)
        r = random.choice([1, 1, 1, 2, 2, 3]) * SS
        a = random.randint(60, 210)
        bd.ellipse([x - r, y - r, x + r, y + r], fill=(200, 225, 255, a))

    # 圆角遮罩
    mask = Image.new("L", (W, W), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.22), fill=255)
    img.paste(bg, (0, 0), mask)

    # ---- 中心能量环
    glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy = W // 2, W // 2
    for i in range(26, 0, -1):
        rr = int(W * 0.30 * i / 26)
        a = int(26 * (1 - i / 26.0) + 8)
        gd.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=(40, 170, 255, a))
    glow = glow.filter(ImageFilter.GaussianBlur(W * 0.03))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)

    # 环线
    for k, (rr, col, wd) in enumerate([
        (int(W * 0.355), (60, 210, 255, 190), int(W * 0.012)),
        (int(W * 0.315), (30, 120, 200, 120), int(W * 0.006)),
    ]):
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=col, width=wd)
    # 环上刻度
    for i in range(36):
        ang = i * math.pi * 2 / 36
        r1 = W * 0.345
        r2 = W * 0.375 if i % 3 == 0 else W * 0.362
        x1, y1 = cx + r1 * math.cos(ang), cy + r1 * math.sin(ang)
        x2, y2 = cx + r2 * math.cos(ang), cy + r2 * math.sin(ang)
        d.line([x1, y1, x2, y2], fill=(80, 220, 255, 150), width=int(W * 0.006))

    # ---- 战机（俯视，朝上）
    def P(points):
        return [(cx + x * W, cy + y * W) for (x, y) in points]

    # 尾焰
    flame = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    fd = ImageDraw.Draw(flame)
    fd.polygon(P([(-0.055, 0.26), (0.055, 0.26), (0.0, 0.62)]), fill=(255, 175, 50, 230))
    fd.polygon(P([(-0.032, 0.27), (0.032, 0.27), (0.0, 0.48)]), fill=(255, 246, 205, 250))
    flame = flame.filter(ImageFilter.GaussianBlur(W * 0.012))
    img = Image.alpha_composite(img, flame)
    d = ImageDraw.Draw(img)

    # 主翼（后掠三角翼）
    d.polygon(P([(0.0, -0.26), (-0.075, 0.04), (-0.335, 0.245), (-0.335, 0.30),
                 (-0.085, 0.20), (-0.072, 0.29), (0.072, 0.29), (0.085, 0.20),
                 (0.335, 0.30), (0.335, 0.245), (0.075, 0.04)]), fill=(206, 220, 236, 255))
    # 机身高光
    d.polygon(P([(0.0, -0.34), (-0.052, -0.06), (-0.045, 0.27), (0.045, 0.27), (0.052, -0.06)]),
              fill=(244, 250, 255, 255))
    # 座舱
    d.polygon(P([(0.0, -0.235), (-0.028, -0.10), (-0.022, 0.02), (0.022, 0.02), (0.028, -0.10)]),
              fill=(24, 150, 230, 255))
    d.polygon(P([(0.0, -0.235), (-0.014, -0.11), (-0.010, 0.0), (0.0, 0.0)]),
              fill=(150, 235, 255, 255))
    # 翼尖导弹（红）
    for sx in (-1, 1):
        d.polygon(P([(sx * 0.285, 0.155), (sx * 0.325, 0.20), (sx * 0.325, 0.275), (sx * 0.285, 0.235)]),
                  fill=(235, 70, 70, 255))
        d.polygon(P([(sx * 0.292, 0.225), (sx * 0.322, 0.245), (sx * 0.33, 0.255),
                     (sx * 0.322, 0.272), (sx * 0.292, 0.252)]), fill=(255, 220, 120, 255))
    # 机翼描边
    d.line(P([(0.0, -0.26), (-0.075, 0.04), (-0.335, 0.245)]), fill=(120, 200, 245, 220), width=int(W * 0.008))
    d.line(P([(0.0, -0.26), (0.075, 0.04), (0.335, 0.245)]), fill=(120, 200, 245, 220), width=int(W * 0.008))

    # 下方光晕增强对比
    under = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ud = ImageDraw.Draw(under)
    ud.ellipse([cx - W * 0.20, cy - W * 0.20, cx + W * 0.20, cy + W * 0.20], fill=(0, 120, 255, 60))
    under = under.filter(ImageFilter.GaussianBlur(W * 0.05))

    final = img.resize((S, S), Image.LANCZOS)
    return final

def main():
    icon = make_icon()
    sizes = {"mipmap-ldpi": 36, "mipmap-mdpi": 48, "mipmap-hdpi": 72,
             "mipmap-xhdpi": 96, "mipmap-xxhdpi": 144, "mipmap-xxxhdpi": 192}
    for folder, px in sizes.items():
        p = os.path.join(OUT, folder)
        os.makedirs(p, exist_ok=True)
        icon.resize((px, px), Image.LANCZOS).save(os.path.join(p, "ic_launcher.png"))
    # 商店/README 用大图
    icon.save(os.path.join(OUT, "..", "tools", "icon_preview.png"))
    print("icon done")

main()
