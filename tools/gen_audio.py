# -*- coding: utf-8 -*-
"""用纯 Python 合成音效与循环 BGM（WAV 16bit mono 22050Hz）。"""
import wave, math, random, struct, os

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "res", "raw")
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(2026)


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
    print("  ", name, len(samples) / SR, "s")


def env(i, n, a=0.005, r=0.5):
    t = i / SR
    total = n / SR
    atk = min(1.0, t / a) if a > 0 else 1.0
    rel = max(0.0, (total - t) / (total * r)) if r > 0 else 1.0
    return atk * min(1.0, rel)


def sq(phase):
    return 1.0 if (phase % 1.0) < 0.5 else -1.0


def saw(phase):
    return 2.0 * (phase % 1.0) - 1.0


def s_square_sweep(dur, f0, f1, vol=0.5, noise=0.0):
    n = int(SR * dur)
    out = []
    ph = 0.0
    for i in range(n):
        t = i / n
        f = f0 + (f1 - f0) * t
        ph += f / SR
        v = sq(ph) * 0.7 + (saw(ph * 0.5) * 0.3)
        v += (rnd.random() * 2 - 1) * noise
        out.append(v * vol * env(i, n, 0.002, 0.85))
    return out


def s_noise_burst(dur, vol=0.6, low=0.15, thump=0.0):
    n = int(SR * dur)
    out = []
    lp = 0.0
    for i in range(n):
        t = i / n
        nz = rnd.random() * 2 - 1
        lp += (nz - lp) * low
        v = lp * (1.0 - t) ** 1.6
        if thump > 0:
            v += math.sin(2 * math.pi * (110 - 60 * t) * i / SR) * thump * (1 - t) ** 3
        out.append(v * vol)
    return out


def s_arp(dur, notes, vol=0.38):
    n = int(SR * dur)
    out = [0.0] * n
    seg = n // len(notes)
    for k, f in enumerate(notes):
        for i in range(seg):
            idx = k * seg + i
            if idx >= n:
                break
            t = i / seg
            v = math.sin(2 * math.pi * f * (idx / SR)) * 0.6
            v += math.sin(2 * math.pi * f * 2 * (idx / SR)) * 0.18
            out[idx] += v * vol * (1 - t) ** 1.2
    return out


def mix(*tracks):
    n = max(len(t) for t in tracks)
    out = [0.0] * n
    for t in tracks:
        for i, v in enumerate(t):
            out[i] += v
    return out


# ---------------- 音效 ----------------
save("sfx_shoot", s_square_sweep(0.055, 1250, 620, 0.28, 0.05))
save("sfx_laser", s_square_sweep(0.16, 1500, 260, 0.30, 0.03))
save("sfx_explode", s_noise_burst(0.42, 0.75, 0.09, 0.5))
save("sfx_hit", s_noise_burst(0.045, 0.32, 0.45, 0.0))
save("sfx_power", s_arp(0.30, [660, 880, 1320], 0.34))
save("sfx_ui", s_arp(0.07, [1180], 0.30))
save("sfx_coin", s_arp(0.13, [1500, 2100], 0.26))
save("sfx_sup", mix(s_square_sweep(0.75, 180, 1400, 0.30, 0.02), s_noise_burst(0.75, 0.35, 0.06, 0.35)))
save("sfx_boss", s_noise_burst(1.1, 0.42, 0.03, 0.55))
save("sfx_death", mix(s_noise_burst(0.85, 0.6, 0.05, 0.6), s_square_sweep(0.8, 700, 90, 0.22, 0.05)))

# ---------------- BGM ----------------
BPM = 128
beat = 60.0 / BPM
bar = beat * 4
CHORDS = [
    [220.00, 261.63, 329.63],   # Am
    [174.61, 220.00, 261.63],   # F
    [196.00, 261.63, 329.63],   # C/G-ish
    [196.00, 246.94, 293.66],   # G
]
BASS = [55.0, 43.65, 49.0, 49.0]
bars = 8
total = int(SR * bar * bars)
track = [0.0] * total

def add(at, samples, gain=1.0):
    for i, v in enumerate(samples):
        idx = at + i
        if 0 <= idx < total:
            track[idx] += v * gain

for b in range(bars):
    ci = b % 4
    base = int(b * bar * SR)
    chord = CHORDS[ci]
    # 低音
    for k in range(8):
        f = BASS[ci] * (2 if k % 4 == 3 else 1)
        dur = beat * 0.5
        n = int(SR * dur)
        seg = []
        for i in range(n):
            t = i / n
            v = math.sin(2 * math.pi * f * i / SR) * 0.55
            v += sq(2 * math.pi * f * i / SR) * 0.12
            seg.append(v * (1 - t) ** 0.7)
        add(base + int(k * beat * 0.5 * SR), seg, 0.45)
    # 琶音
    seq = [chord[0], chord[1], chord[2], chord[1] * 2, chord[2], chord[1], chord[0] * 2, chord[2]]
    for k in range(16):
        f = seq[k % 8] * 2
        dur = beat * 0.25
        n = int(SR * dur)
        seg = []
        for i in range(n):
            t = i / n
            v = math.sin(2 * math.pi * f * i / SR) * 0.35
            v += math.sin(2 * math.pi * f * 2 * i / SR) * 0.10
            seg.append(v * (1 - t) ** 1.8)
        add(base + int(k * beat * 0.25 * SR), seg, 0.30)
    # 踩镲
    for k in range(8):
        dur = 0.045
        n = int(SR * dur)
        seg = [(rnd.random() * 2 - 1) * (1 - i / n) ** 3 for i in range(n)]
        add(base + int(k * beat * 0.5 * SR), seg, 0.16)
    # 军鼓
    for k in (1, 3):
        dur = 0.12
        n = int(SR * dur)
        seg = []
        for i in range(n):
            t = i / n
            seg.append(((rnd.random() * 2 - 1) * 0.6 + math.sin(2 * math.pi * 190 * i / SR) * 0.4) * (1 - t) ** 2.2)
        add(base + int(k * beat * SR), seg, 0.20)

# 主旋律（简单英雄感，放在高八度）
MEL = [
    [659.25, 0, 587.33, 523.25, 587.33, 0, 659.25, 783.99],
    [698.46, 0, 659.25, 587.33, 523.25, 0, 587.33, 523.25],
    [523.25, 587.33, 659.25, 783.99, 880.00, 0, 783.99, 659.25],
    [587.33, 0, 523.25, 493.88, 440.00, 0, 493.88, 587.33],
]
for b in range(bars):
    base = int(b * bar * SR)
    line = MEL[(b // 2) % 4]
    for k, f in enumerate(line):
        if f <= 0:
            continue
        dur = beat * 0.5
        n = int(SR * dur)
        seg = []
        for i in range(n):
            t = i / n
            v = math.sin(2 * math.pi * f * i / SR) * 0.4
            v += math.sin(2 * math.pi * f * 2 * i / SR) * 0.12
            seg.append(v * (1 - t) ** 0.9 * min(1.0, t * 30))
        add(base + int(k * beat * 0.5 * SR), seg, 0.26)

# 归一 + 首尾淡入淡出（保证循环不爆音）
peak = max(1e-6, max(abs(v) for v in track))
scale = 0.82 / peak
fade = int(SR * 0.02)
for i in range(total):
    track[i] *= scale
    if i < fade:
        track[i] *= i / fade
    if i > total - fade:
        track[i] *= (total - i) / fade
save("bgm", track)
print("audio done")
