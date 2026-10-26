# -*- coding: utf-8 -*-
"""生成 TapTap 上架用的商店素材：方形图标（不透明、不切圆角）+ 16:9 / 1:1 宣传图。

产物落在 dist/taptap/ 下，尺寸与格式按 TapTap 商店素材规范：
  图标     ≥512×512  JPG/PNG，不得白底/黑底/透明底，不得自行切圆角
   16:9 宣传图 ≥1920×1080，图中只允许出现游戏标题
   1:1  宣传图 ≥1440×1440，同样只放游戏标题
"""
import os
import math
import random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "dist", "taptap")
os.makedirs(OUT, exist_ok=True)

TITLE = "苍穹战机"
FONT_BOLD = "C:/Windows/Fonts/msyhbd.ttc"


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def font(size):
    try:
        return ImageFont.truetype(FONT_BOLD, size)
    except Exception:
        return ImageFont.load_default()


def starfield(d, w, h, n=420, seed=11):
    random.seed(seed)
    for _ in range(n):
        x = random.uniform(0, w)
        y = random.uniform(0, h)
        r = random.choice([1, 1, 1, 1.6, 2.2, 3.0]) * (w / 1024.0)
        a = random.randint(50, 200)
        col = random.choice([(190, 220, 255), (150, 200, 255), (255, 240, 220)])
        d.ellipse([x - r, y - r, x + r, y + r], fill=col + (a,))


def nebula(w, h, seed=3):
    """两块高斯模糊的彩色云气，避免纯渐变太死板"""
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    random.seed(seed)
    blobs = [
        (0.18, 0.30, 0.42, (40, 90, 220, 90)),
        (0.78, 0.22, 0.34, (140, 60, 200, 70)),
        (0.62, 0.78, 0.46, (20, 130, 200, 80)),
        (0.30, 0.88, 0.30, (10, 70, 150, 70)),
    ]
    for cx, cy, rr, col in blobs:
        d.ellipse([(cx - rr) * w, (cy - rr) * h, (cx + rr) * w, (cy + rr) * h], fill=col)
    return layer.filter(ImageFilter.GaussianBlur(w * 0.06))


def make_ship(cw, ch, cx, cy, size):
    """在 cw×ch 画布上画一架俯视战机（朝上），size = 机身长度基准

    机头在 cy-0.34*size，尾焰到 cy+0.95*size，调用方负责留出空间。
    """
    layer = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    def P(pts):
        return [(cx + x * size, cy + y * size) for (x, y) in pts]

    # 尾焰
    flame = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    fd = ImageDraw.Draw(flame)
    fd.polygon(P([(-0.10, 0.30), (0.10, 0.30), (0.0, 0.88)]), fill=(255, 160, 40, 210))
    fd.polygon(P([(-0.055, 0.31), (0.055, 0.31), (0.0, 0.66)]), fill=(255, 248, 210, 240))
    flame = flame.filter(ImageFilter.GaussianBlur(size * 0.06))

    # 主翼
    d.polygon(P([(0.0, -0.26), (-0.075, 0.04), (-0.335, 0.245), (-0.335, 0.30),
                 (-0.085, 0.20), (-0.072, 0.29), (0.072, 0.29), (0.085, 0.20),
                 (0.335, 0.30), (0.335, 0.245), (0.075, 0.04)]), fill=(206, 220, 236, 255))
    d.polygon(P([(0.0, -0.34), (-0.052, -0.06), (-0.045, 0.27), (0.045, 0.27), (0.052, -0.06)]),
              fill=(244, 250, 255, 255))
    d.polygon(P([(0.0, -0.235), (-0.028, -0.10), (-0.022, 0.02), (0.022, 0.02), (0.028, -0.10)]),
              fill=(24, 150, 230, 255))
    d.polygon(P([(0.0, -0.235), (-0.014, -0.11), (-0.010, 0.0), (0.0, 0.0)]),
              fill=(150, 235, 255, 255))
    for sx in (-1, 1):
        d.polygon(P([(sx * 0.285, 0.155), (sx * 0.325, 0.20), (sx * 0.325, 0.275), (sx * 0.285, 0.235)]),
                  fill=(235, 70, 70, 255))
        d.polygon(P([(sx * 0.292, 0.225), (sx * 0.322, 0.245), (sx * 0.33, 0.255),
                     (sx * 0.322, 0.272), (sx * 0.292, 0.252)]), fill=(255, 220, 120, 255))
    lw = max(2, int(size * 0.012))
    d.line(P([(0.0, -0.26), (-0.075, 0.04), (-0.335, 0.245)]), fill=(120, 200, 245, 220), width=lw)
    d.line(P([(0.0, -0.26), (0.075, 0.04), (0.335, 0.245)]), fill=(120, 200, 245, 220), width=lw)

    # 机翼下方的冷光
    under = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    ud = ImageDraw.Draw(under)
    ud.ellipse([cx - size * 0.34, cy - size * 0.30, cx + size * 0.34, cy + size * 0.38],
               fill=(50, 170, 255, 70))
    under = under.filter(ImageFilter.GaussianBlur(size * 0.14))

    base = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    base = Image.alpha_composite(base, under)
    base = Image.alpha_composite(base, flame)
    base = Image.alpha_composite(base, layer)
    return base


def draw_title(img, text, cx, cy, size, glow=True):
    w, h = img.size
    f = font(size)
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    bbox = d.textbbox((0, 0), text, font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    x = cx - tw / 2 - bbox[0]
    y = cy - th / 2 - bbox[1]
    if glow:
        g = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        gd = ImageDraw.Draw(g)
        gd.text((x, y), text, font=f, fill=(70, 200, 255, 235))
        g = g.filter(ImageFilter.GaussianBlur(size * 0.10))
        img = Image.alpha_composite(img, g)
        img = Image.alpha_composite(img, g)
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.text((x, y), text, font=f, fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, layer)
    return img, (x, y, tw, th)


def base_sky(w, h):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=lerp((6, 10, 26), (8, 34, 60), t) + (255,))
    img = Image.alpha_composite(img, nebula(w, h))
    d = ImageDraw.Draw(img)
    starfield(d, w, h)
    return img


def planet(img, w, h, cx, cy, rr):
    """画面下方的大气层弧：只露一条浅浅的剪影 + 边缘光，不抢主体"""
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=(5, 11, 24, 255))
    img = Image.alpha_composite(img, layer)
    rim = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rim)
    rd.arc([cx - rr, cy - rr, cx + rr, cy + rr], start=-120, end=-60,
           fill=(100, 205, 255, 170), width=max(2, int(w * 0.0035)))
    rim = rim.filter(ImageFilter.GaussianBlur(w * 0.006))
    return Image.alpha_composite(img, rim)


def icon_square(size=1024):
    """商店图标：满幅不透明方图，不切圆角"""
    W = size * 2
    img = base_sky(W, W)
    ship = make_ship(W, W, W * 0.5, W * 0.55, W * 0.52)
    img = Image.alpha_composite(img, ship)
    return img.resize((size, size), Image.LANCZOS).convert("RGB")


def key_art(w, h, ship_px, ship_pos, title_pos, title_px, planet_cy=1.62):
    """ship_px / title_px 以画面高度为基准的绝对像素（先在 2 倍画布上算）"""
    W, H = w * 2, h * 2
    img = base_sky(W, H)
    img = planet(img, W, H, W * 0.42, H * planet_cy, H * 1.05)
    ship = make_ship(W, H, ship_pos[0] * W, ship_pos[1] * H, ship_px)
    img = Image.alpha_composite(img, ship)
    img, _ = draw_title(img, TITLE, title_pos[0] * W, title_pos[1] * H, title_px)
    return img.resize((w, h), Image.LANCZOS).convert("RGB")


def main():
    icon_square(1024).save(os.path.join(OUT, "icon-1024.png"))
    # 16:9 主图：标题在左，战机在右（机头留出 8% 余量，尾焰不触底）
    key_art(1920, 1080, ship_px=760, ship_pos=(0.735, 0.50), title_pos=(0.29, 0.47),
            title_px=440).save(os.path.join(OUT, "feature-16x9-1920x1080.png"))
    # 1:1 主图：标题在上，战机在中，星球压到画面底部只露一条弧
    key_art(1440, 1440, ship_px=700, ship_pos=(0.50, 0.52), title_pos=(0.50, 0.19),
            title_px=400, planet_cy=2.05).save(os.path.join(OUT, "feature-1x1-1440x1440.png"))
    # 9:16 竖版（视频封面 / 竖版宣传位）：标题上、战机中、星球弧形压底
    key_art(1080, 1920, ship_px=1360, ship_pos=(0.50, 0.55), title_pos=(0.50, 0.175),
            title_px=420, planet_cy=2.02).save(os.path.join(OUT, "feature-9x16-1080x1920.png"))
    for f in sorted(os.listdir(OUT)):
        p = os.path.join(OUT, f)
        print(f, os.path.getsize(p), "bytes")


main()
