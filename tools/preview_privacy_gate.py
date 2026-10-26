# -*- coding: utf-8 -*-
"""按 PrivacyGate.java 的 dp / sp 参数，在 1080x1920 画布上模拟首次启动的隐私政策弹窗排版。

只是排版预览（用于确认字号、留白、按钮比例是否合适），不是真机截图。
"""

import os
from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 1920
DENSITY = 3.0          # 1080p 手机常见 density
OUT = os.path.join("dist", "taptap", "privacy-gate-preview.png")

FONT_R = "C:/Windows/Fonts/msyh.ttc"
FONT_B = "C:/Windows/Fonts/msyhbd.ttc"


def dp(v):
    return int(round(v * DENSITY))


def sp(v):
    return int(round(v * DENSITY))


def font(bold, size):
    return ImageFont.truetype(FONT_B if bold else FONT_R, size)


TXT = """《苍穹战机》（英文名 SkyWar，应用包名 com.zhiwei.skywar）由个人开发者独立开发与运营，为纯单机游戏。请您在使用前阅读以下内容。

一、我们收集哪些信息
本游戏不收集任何个人信息。不需要注册或登录，没有账号体系；不采集设备标识符（IMEI、Android ID、OAID、MAC 地址等）；不采集位置、通讯录、短信、相册、麦克风、摄像头内容；不采集任何可用于识别个人身份的信息。

二、我们在设备上保存什么
为让您下次打开时进度不丢失，本游戏会在您自己的设备本地保存一份存档，内容包括：金币数量、武器解锁与等级、配件强化等级、关卡解锁与最佳成绩、音量开关设置。这些数据存放于应用私有目录（Android SharedPreferences），仅存在于本机、仅用于本游戏读取，不会上传、不会共享给任何第三方。卸载本游戏即一并删除。

三、系统权限说明
本游戏的安装包未声明任何系统权限，包括网络访问、存储与相册读写、位置信息、通讯录与短信、麦克风与摄像头、设备信息与电话状态、悬浮窗与通知。由于未申请网络访问权限，在系统层面即无法建立网络连接，因此不存在把数据上传到服务器的可能。"""


def wrap(draw, text, fnt, maxw):
    lines = []
    for para in text.split("\n"):
        if not para:
            lines.append("")
            continue
        cur = ""
        for ch in para:
            test = cur + ch
            if draw.textlength(test, font=fnt) > maxw and cur:
                lines.append(cur)
                cur = ch
            else:
                cur = test
        if cur:
            lines.append(cur)
    return lines


def main():
    img = Image.new("RGB", (W, H), (0x06, 0x0A, 0x12))
    d = ImageDraw.Draw(img)

    # 背景加一点深空渐变，贴近游戏气质
    for y in range(H):
        t = y / H
        r = int(6 + 10 * (1 - abs(t - 0.5) * 2))
        g = int(10 + 16 * (1 - abs(t - 0.5) * 2))
        b = int(18 + 34 * (1 - abs(t - 0.5) * 2))
        d.line([(0, y), (W, y)], fill=(r, g, b))

    # 卡片
    mx, mt, mb = dp(14), dp(26), dp(20)
    card = [mx, mt, W - mx, H - mb]
    d.rounded_rectangle(card, radius=dp(16), fill=(0x0D, 0x17, 0x27),
                        outline=(0x20, 0x36, 0x4F), width=dp(1))

    pad = dp(20)
    x0 = card[0] + pad
    x1 = card[2] - pad
    y = card[1] + pad
    inner_w = x1 - x0

    # 标题
    f_title = font(True, sp(20))
    d.text((x0, y), "隐私政策", font=f_title, fill=(0xDF, 0xF1, 0xFF))
    y += dp(30)

    # 副标题
    f_ver = font(False, sp(12))
    y += dp(6)
    d.text((x0, y), "苍穹战机 SkyWar　·　版本 v1.0",
           font=f_ver, fill=(0x7E, 0x95, 0xAE))
    y += dp(20) + dp(12)

    # 底部固定区（从下往上算）
    btn_h = dp(46)
    btn_y = card[3] - pad + dp(4) - btn_h + dp(16)
    hint_h = dp(34)
    hint_y = btn_y - dp(12) - hint_h * 2
    link_h = dp(18)
    link_y = hint_y - dp(24) - link_h

    # 正文滚动区
    body_bottom = link_y - dp(10)
    body_top = y
    f_body = font(False, sp(13))
    lh = int(sp(13) * 1.35) + dp(2)
    lines = wrap(d, TXT, f_body, inner_w - dp(6))

    # 底部渐隐表示还有内容
    drawn = 0
    yy = body_top
    while yy + lh < body_bottom and drawn < len(lines):
        d.text((x0, yy), lines[drawn], font=f_body, fill=(0xC3, 0xD3, 0xE4))
        yy += lh
        drawn += 1
    fade_top = body_bottom - lh * 2
    for i in range(lh * 2):
        a = i / float(lh * 2)
        d.line([(x0, fade_top + i), (x1, fade_top + i)],
               fill=(int(0x0D + (0xC3 - 0x0D) * (1 - a) * 0),
                     int(0x17), int(0x27)))

    # 滚动条
    track_x = x1 - dp(2)
    d.rounded_rectangle([track_x, body_top, track_x + dp(2), body_bottom],
                        radius=dp(1), fill=(0x18, 0x27, 0x38))
    bar_h = int((body_bottom - body_top) * 0.32)
    d.rounded_rectangle([track_x, body_top, track_x + dp(2), body_top + bar_h],
                        radius=dp(1), fill=(0x3A, 0x5C, 0x7A))

    # 链接
    f_link = font(False, sp(12.5))
    d.text((x0, link_y), "在浏览器中查看完整版 →", font=f_link, fill=(0x4F, 0xD8, 0xFF))

    # 提示
    f_hint = font(False, sp(11.5))
    hint_lines = wrap(d, "请阅读以上条款后选择。点击「同意」表示您已阅读并同意本政策。",
                      f_hint, inner_w)
    hy = hint_y
    for ln in hint_lines[:2]:
        d.text((x0, hy), ln, font=f_hint, fill=(0x7E, 0x95, 0xAE))
        hy += int(sp(11.5) * 1.45)

    # 按钮
    gap = dp(6)
    bw = (inner_w - gap * 2) / 2
    f_btn = font(True, sp(15))
    for i, (label, fill, tcol) in enumerate([
            ("不同意", (0x24, 0x34, 0x47), (0xA9, 0xBC, 0xCD)),
            ("同意并继续", (0x12, 0x3D, 0x52), (0x9F, 0xE4, 0xFF))]):
        bx0 = x0 + i * (bw + gap * 2)
        bx1 = bx0 + bw
        d.rounded_rectangle([bx0, btn_y, bx1, btn_y + btn_h], radius=dp(10),
                            fill=fill, outline=(0x2C, 0x47, 0x63), width=dp(1))
        tw = d.textlength(label, font=f_btn)
        th = sp(15)
        d.text(((bx0 + bx1 - tw) / 2, btn_y + (btn_h - th) / 2 - dp(2)),
               label, font=f_btn, fill=tcol)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT, optimize=True)
    print("%s  %dx%d  %d bytes  (正文显示 %d/%d 行，需滚动)"
          % (OUT, W, H, os.path.getsize(OUT), drawn, len(lines)))


if __name__ == "__main__":
    main()
