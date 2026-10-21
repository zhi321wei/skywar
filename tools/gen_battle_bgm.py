# -*- coding: utf-8 -*-
"""生成关卡战斗 BGM：D 小调，BPM 160，16 小节无缝循环。

跟菜单曲（tools/gen_audio.py 里那首 140BPM 的 Am-F-C-G）刻意区分开：
- 更快：160 BPM
- 更冲：Dm-Bb-F-C 进行 + 八分音符强力和弦 riff（stab）
- 更密：底鼓每拍、军鼓 2/4、踩镲十六分、贝斯十六分音符推进
- 更亮：总线上做一次高频搁架提升，适配手机外放
"""
import wave
import math
import random
import struct
import os

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "res", "raw")
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(160160)


def save(name, samples):
    path = os.path.join(OUT, name + ".wav")
    data = bytearray()
    for s in samples:
        v = int(max(-1.0, min(1.0, s)) * 32000)
        data += struct.pack("<h", v)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(data))
    print("  ", name, round(len(samples) / SR, 2), "s")


# ==================== 合成音色 ====================

def synth_pad(freqs, dur):
    """失谐锯齿堆叠 + 二级低通，铺底用"""
    n = int(SR * dur)
    raw = [0.0] * n
    for f in freqs:
        for oct_, dets in ((1.0, (-0.006, 0.007)), (2.0, (0.004,))):
            for det in dets:
                ph = rnd.random()
                inc = f * oct_ * (1.0 + det) / SR
                for i in range(n):
                    ph += inc
                    if ph >= 1.0:
                        ph -= 1.0
                    raw[i] += (2.0 * ph - 1.0) * (0.5 if oct_ > 1 else 1.0)
    lp1 = 0.0
    lp2 = 0.0
    att = int(SR * 0.16)
    out = [0.0] * n
    for i in range(n):
        lp1 += (raw[i] - lp1) * 0.34
        lp2 += (lp1 - lp2) * 0.55
        e = i / att if i < att else 1.0
        out[i] = lp2 * e
    return out


def synth_bass(f, dur):
    n = int(SR * dur)
    out = [0.0] * n
    ph = 0.0
    for i in range(n):
        t = i / n
        ph += f * (1.0 + 0.03 * (1.0 - t)) / SR
        if ph >= 1.0:
            ph -= 1.0
        v = math.tanh(math.sin(2 * math.pi * ph) * 2.1) * 0.72
        e = (1.0 - t) ** 0.42
        if i < 90:
            e *= i / 90.0
        out[i] = v * e
    return out


def synth_stab(freqs, dur):
    """短促强力和弦（根音 + 五度 + 高八度），riff 的主力音色"""
    n = int(SR * dur)
    out = [0.0] * n
    phs = [0.0] * len(freqs)
    lp = 0.0
    k = len(freqs)
    for i in range(n):
        t = i / n
        s = 0.0
        for j in range(k):
            phs[j] += freqs[j] / SR
            if phs[j] >= 1.0:
                phs[j] -= 1.0
            s += (2.0 * phs[j] - 1.0)
        s /= k
        lp += (s - lp) * 0.62
        e = (1.0 - t) ** 1.1
        if i < 70:
            e *= i / 70.0
        out[i] = lp * e
    return out


def synth_kick():
    n = int(SR * 0.22)
    out = [0.0] * n
    ph = 0.0
    for i in range(n):
        t = i / n
        # 基频抬到 62Hz：手机外放对 50Hz 以下几乎没有响应，压低只会白白吃掉动态
        ph += (62.0 + 150.0 * math.exp(-t * 30.0)) / SR
        v = math.sin(2 * math.pi * ph) * math.exp(-t * 6.0)
        v += (rnd.random() * 2 - 1) * 0.26 * math.exp(-t * 80.0)
        out[i] = v
    return out


def synth_snare():
    n = int(SR * 0.18)
    out = [0.0] * n
    prev = 0.0
    for i in range(n):
        t = i / n
        nz = rnd.random() * 2 - 1
        hp = nz - prev
        prev = nz
        v = hp * 0.56 * math.exp(-t * 16.0)
        v += math.sin(2 * math.pi * 205.0 * i / SR) * 0.36 * math.exp(-t * 26.0)
        out[i] = v
    return out


def synth_hat(gain=0.10, dur=0.038):
    n = int(SR * dur)
    out = [0.0] * n
    prev = 0.0
    for i in range(n):
        nz = rnd.random() * 2 - 1
        hp = nz - prev
        prev = nz
        out[i] = hp * math.exp(-(i / n) * 10.0) * gain
    return out


def synth_crash(gain=0.18):
    n = int(SR * 1.0)
    out = [0.0] * n
    prev = 0.0
    for i in range(n):
        nz = rnd.random() * 2 - 1
        hp = nz - prev
        prev = nz
        out[i] = hp * math.exp(-(i / n) * 4.6) * gain
    return out


def synth_lead(f, dur, vib=0.0):
    n = int(SR * dur)
    out = [0.0] * n
    ph = 0.0
    ph2 = 0.0
    for i in range(n):
        t = i / n
        vf = 1.0 + vib * math.sin(2 * math.pi * 5.6 * i / SR)
        ph += f * vf / SR
        ph2 += f * vf * 2.006 / SR
        v = (math.sin(2 * math.pi * ph) * 0.58
             + (2.0 * (ph2 % 1.0) - 1.0) * 0.22
             + math.sin(2 * math.pi * ph * 2.0) * 0.20)
        e = min(1.0, i / (SR * 0.012))
        e *= (1.0 - t) ** 0.34
        if t > 0.88:
            e *= (1.0 - t) / 0.12
        out[i] = v * e
    return out


def add_to(track, at, buf, gain=1.0):
    if at < 0:
        buf = buf[-at:]
        at = 0
    end = min(len(track), at + len(buf))
    for i in range(at, end):
        track[i] += buf[i - at] * gain


def with_delay(src, dly_s, fb, mixv):
    d = int(SR * dly_s)
    out = src[:]
    if d <= 0:
        return out
    echo = [0.0] * len(src)
    for i in range(d, len(src)):
        echo[i] = src[i - d] * mixv + echo[i - d] * fb
    for i in range(len(src)):
        out[i] += echo[i]
    return out


# ==================== 编曲 ====================

BPM = 160
beat = 60.0 / BPM
bar = beat * 4
BARS = 16
total = int(SR * bar * BARS)

# Dm - Bb - F - C（6-4-1-5，燃向进行）
CH = [
    (73.42, [293.66, 349.23, 440.00]),    # Dm
    (58.27, [233.08, 293.66, 349.23]),    # Bb
    (87.31, [349.23, 440.00, 523.25]),    # F
    (65.41, [261.63, 329.63, 392.00]),    # C
]

pad_t = [0.0] * total
bass_t = [0.0] * total
drum_t = [0.0] * total
stab_t = [0.0] * total
lead_t = [0.0] * total

# 每小节：pad 增益、底鼓、镲细分、贝斯步数、stab 增益、军鼓
STAGE = [
    # b0-1 引子：只有 riff 和稀疏鼓，蓄势
    (0.040, "half", 4, 0, 0.160, False),
    (0.042, "half", 4, 0, 0.168, False),
    # b2-3 进节奏
    (0.046, "four", 2, 8, 0.182, True),
    (0.048, "four", 2, 8, 0.188, True),
    # b4-11 全开
    (0.050, "four", 1, 16, 0.196, True),
    (0.050, "four", 1, 16, 0.200, True),
    (0.051, "four", 1, 16, 0.204, True),
    (0.051, "four", 1, 16, 0.204, True),
    (0.052, "four", 1, 16, 0.208, True),
    (0.052, "four", 1, 16, 0.208, True),
    (0.052, "four", 1, 16, 0.210, True),
    (0.052, "four", 1, 16, 0.210, True),
    # b12-15 收尾段，最后一小节做 fill
    (0.050, "four", 1, 16, 0.206, True),
    (0.050, "four", 1, 16, 0.206, True),
    (0.050, "four", 1, 16, 0.202, True),
    (0.048, "four", 2, 8, 0.196, True),
]

# 八分音符 riff 位置（以拍为单位），第二、四小节做变化
RIFF_A = (0.0, 0.5, 1.5, 2.0, 2.5, 3.0)
RIFF_B = (0.0, 0.5, 1.0, 1.5, 2.5, 3.5)

for b in range(BARS):
    base = int(b * bar * SR)
    root, chord = CH[b % 4]
    pad_g, kick_mode, hat_div, bass_steps, stab_g, snare_on = STAGE[b]

    # 铺底
    add_to(pad_t, base, synth_pad(chord, bar * 1.04), pad_g)

    # 贝斯：十六分音符推进
    if bass_steps > 0:
        step = bar / bass_steps
        for k in range(bass_steps):
            f = root * (2.0 if (bass_steps == 16 and k % 4 == 3) else 1.0)
            add_to(bass_t, base + int(k * step * SR), synth_bass(f, step * 0.92), 0.115)

    # 鼓组
    if kick_mode == "four":
        kicks = (0.0, 1.0, 2.0, 3.0)
    else:
        kicks = (0.0, 2.0)
    for pos in kicks:
        add_to(drum_t, base + int(pos * beat * SR), synth_kick(), 0.26)
    if snare_on:
        for pos in (1.0, 3.0):
            add_to(drum_t, base + int(pos * beat * SR), synth_snare(), 0.31)
    # 十六分踩镲
    n_hat = 16 // hat_div
    hstep = beat * 0.25 * hat_div
    for k in range(n_hat):
        if hat_div == 1:
            g = 0.105 if k % 4 == 2 else (0.058 if k % 2 == 0 else 0.034)
        else:
            g = 0.075 if k % 2 == 0 else 0.044
        add_to(drum_t, base + int(k * hstep * SR), synth_hat(g), 1.0)
    if b % 4 == 0 and b > 0:
        add_to(drum_t, base, synth_crash(), 1.0)
    # 每 4 小节末尾加一段军鼓滚奏（fill）
    if b % 4 == 3 and b < BARS - 1:
        for k in range(8):
            add_to(drum_t, base + int((3.0 + k * 0.125) * beat * SR),
                   synth_snare(), 0.10 + 0.035 * k)

    # 强力和弦 riff
    riff = RIFF_A if (b % 4) in (0, 1) else RIFF_B
    for pos in riff:
        add_to(stab_t, base + int(pos * beat * SR),
               synth_stab([root * 2, root * 3, root * 4], beat * 0.42), stab_g)

# ---- 主旋律（第 5 小节进入，八分/十六分密集，上行动机）----
MEL = []


def m(bar_, pos, dur, f, g=1.0):
    MEL.append((bar_ * 4 + pos, dur, f, g))


D5, E5, F5, G5, A5, Bb5, C6, D6 = 587.33, 659.25, 698.46, 783.99, 880.00, 932.33, 1046.50, 1174.66

# A 段
m(4, 0, 0.5, D5); m(4, 0.5, 0.5, F5); m(4, 1, 0.5, A5); m(4, 1.5, 0.5, F5)
m(4, 2, 1.0, D5); m(4, 3, 0.5, C6, 0.8); m(4, 3.5, 0.5, D5)
m(5, 0, 0.5, F5); m(5, 0.5, 0.5, A5); m(5, 1, 1.5, Bb5); m(5, 2.5, 0.5, A5)
m(5, 3, 1.0, F5)
m(6, 0, 0.5, A5); m(6, 0.5, 0.5, C6); m(6, 1, 1.0, A5); m(6, 2, 0.5, F5)
m(6, 2.5, 0.5, A5); m(6, 3, 1.0, F5)
m(7, 0, 1.0, E5); m(7, 1, 0.5, D5); m(7, 1.5, 0.5, E5); m(7, 2, 2.0, F5)

# B 段：更高、更密
m(8, 0, 0.5, A5); m(8, 0.5, 0.5, Bb5); m(8, 1, 0.5, C6); m(8, 1.5, 0.5, Bb5)
m(8, 2, 1.0, A5); m(8, 3, 0.5, G5); m(8, 3.5, 0.5, A5)
m(9, 0, 0.5, Bb5); m(9, 0.5, 0.5, C6); m(9, 1, 1.5, D6); m(9, 2.5, 0.5, C6)
m(9, 3, 1.0, Bb5)
m(10, 0, 0.5, C6); m(10, 0.5, 0.5, D6); m(10, 1, 1.0, C6); m(10, 2, 0.5, A5)
m(10, 2.5, 0.5, C6); m(10, 3, 1.0, Bb5)
m(11, 0, 1.0, A5); m(11, 1, 0.5, G5); m(11, 1.5, 0.5, A5); m(11, 2, 2.0, Bb5)

# C 段：回 A 动机，末尾上行后落回 D，接回第 1 小节
m(12, 0, 0.5, D5); m(12, 0.5, 0.5, F5); m(12, 1, 0.5, A5); m(12, 1.5, 0.5, F5)
m(12, 2, 1.0, D5); m(12, 3, 0.5, C6, 0.8); m(12, 3.5, 0.5, D5)
m(13, 0, 0.5, F5); m(13, 0.5, 0.5, A5); m(13, 1, 1.5, Bb5); m(13, 2.5, 0.5, A5)
m(13, 3, 1.0, F5)
m(14, 0, 0.5, A5); m(14, 0.5, 0.5, C6); m(14, 1, 1.0, D6); m(14, 2, 0.5, C6)
m(14, 2.5, 0.5, Bb5); m(14, 3, 1.0, A5)
m(15, 0, 0.5, Bb5); m(15, 0.5, 0.5, A5); m(15, 1, 0.5, G5); m(15, 1.5, 0.5, F5)
m(15, 2, 2.0, D5)

for pos, dur, f, g in MEL:
    add_to(lead_t, int(pos * beat * SR), synth_lead(f, dur * beat, 0.004), 0.240 * g)

# 旋律加一层高八度的薄衬，提升穿透力
for pos, dur, f, g in MEL:
    add_to(lead_t, int(pos * beat * SR), synth_lead(f * 2.0, dur * beat, 0.0), 0.078 * g)

# 回声只给旋律，riff 保持干脆
lead_t = with_delay(lead_t, beat * 0.75, 0.28, 0.20)

# ---- 总线混音 ----
track = [0.0] * total
for i in range(total):
    track[i] = (pad_t[i] * 1.0 + bass_t[i] * 1.0 + drum_t[i] * 1.05
                + stab_t[i] * 1.0 + lead_t[i] * 1.0)

# 高频搁架：手机外放时中高频更「亮」，不靠低频堆厚度
lp = 0.0
for i in range(total):
    v = track[i]
    lp += (v - lp) * 0.28
    track[i] = v + (v - lp) * 0.85

# 软限幅 + 归一
peak = max(1e-6, max(abs(v) for v in track))
norm = 1.58 / peak
for i in range(total):
    track[i] = math.tanh(track[i] * norm) * 0.88

# 首尾各 6ms 淡入淡出，消除循环接缝爆音
fade = int(SR * 0.006)
for i in range(fade):
    k = i / fade
    track[i] *= k
    track[total - 1 - i] *= k

save("bgm_battle", track)
print("battle bgm done:", round(total / SR, 2), "s,", BARS, "bars @", BPM, "BPM")
