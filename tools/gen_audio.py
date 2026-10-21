# -*- coding: utf-8 -*-
"""用纯 Python 合成音效与循环 BGM（WAV 16bit mono 22050Hz）。"""
import wave, math, random, struct, os

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "res", "raw")
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(20261021)


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


# ==================== 音效 ====================
print("sfx:")
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


# ==================== BGM ====================
# 史诗电子：A 小调，BPM 140，12 小节无缝循环，四层编曲
# 段落 A(1-4) 铺陈 → B(5-8) 主旋律 → C(9-12) 上行变奏回到起始
print("bgm:")
BPM = 140
beat = 60.0 / BPM
bar = beat * 4
BARS = 12
total = int(SR * bar * BARS)

# 和弦：(低音根音, [三音区三和弦])
CH = [
    (110.00, [220.00, 261.63, 329.63]),   # Am
    (87.31, [174.61, 220.00, 261.63]),    # F
    (130.81, [261.63, 329.63, 392.00]),   # C
    (98.00, [196.00, 246.94, 293.66]),    # G
]

pad_t = [0.0] * total
bass_t = [0.0] * total
drum_t = [0.0] * total
arp_t = [0.0] * total
lead_t = [0.0] * total


def add_to(track, at, buf, gain=1.0):
    if at < 0:
        buf = buf[-at:]
        at = 0
    end = min(len(track), at + len(buf))
    for i in range(at, end):
        track[i] += buf[i - at] * gain


def synth_pad(freqs, dur):
    """失谐锯齿堆叠 + 二级低通，慢慢铺开"""
    n = int(SR * dur)
    raw = [0.0] * n
    for f in freqs:
        for oct_, dets in ((1.0, (-0.008, -0.003, 0.004, 0.011)),
                           (2.0, (-0.005, 0.006))):
            for det in dets:
                ph = rnd.random()
                inc = f * oct_ * (1.0 + det) / SR
                for i in range(n):
                    ph += inc
                    if ph >= 1.0:
                        ph -= 1.0
                    raw[i] += (2.0 * ph - 1.0) * (0.55 if oct_ > 1 else 1.0)
    lp1 = 0.0
    lp2 = 0.0
    att = int(SR * 0.22)
    out = [0.0] * n
    for i in range(n):
        lp1 += (raw[i] - lp1) * 0.50
        lp2 += (lp1 - lp2) * 0.50
        e = i / att if i < att else 1.0
        out[i] = lp2 * e
    return out


def synth_bass(f, dur):
    n = int(SR * dur)
    out = [0.0] * n
    ph = 0.0
    for i in range(n):
        t = i / n
        ph += f * (1.0 + 0.02 * (1.0 - t)) / SR
        if ph >= 1.0:
            ph -= 1.0
        v = math.tanh(math.sin(2 * math.pi * ph) * 1.8) * 0.70
        e = (1.0 - t) ** 0.55
        if i < 140:
            e *= i / 140.0
        out[i] = v * e
    return out


def synth_kick():
    n = int(SR * 0.26)
    out = [0.0] * n
    ph = 0.0
    for i in range(n):
        t = i / n
        ph += (46.0 + 118.0 * math.exp(-t * 24.0)) / SR
        v = math.sin(2 * math.pi * ph) * math.exp(-t * 5.0)
        v += (rnd.random() * 2 - 1) * 0.22 * math.exp(-t * 70.0)
        out[i] = v
    return out


def synth_snare():
    n = int(SR * 0.20)
    out = [0.0] * n
    prev = 0.0
    for i in range(n):
        t = i / n
        nz = rnd.random() * 2 - 1
        hp = nz - prev
        prev = nz
        v = hp * 0.52 * math.exp(-t * 15.0)
        v += math.sin(2 * math.pi * 186.0 * i / SR) * 0.42 * math.exp(-t * 24.0)
        out[i] = v
    return out


def synth_hat(gain=0.10, dur=0.045):
    n = int(SR * dur)
    out = [0.0] * n
    prev = 0.0
    for i in range(n):
        nz = rnd.random() * 2 - 1
        hp = nz - prev
        prev = nz
        out[i] = hp * math.exp(-(i / n) * 9.0) * gain
    return out


def synth_crash(gain=0.16):
    n = int(SR * 1.15)
    out = [0.0] * n
    prev = 0.0
    for i in range(n):
        nz = rnd.random() * 2 - 1
        hp = nz - prev
        prev = nz
        out[i] = hp * math.exp(-(i / n) * 4.2) * gain
    return out


def synth_pluck(f, dur):
    n = int(SR * dur)
    out = [0.0] * n
    ph = 0.0
    lp = 0.0
    for i in range(n):
        t = i / n
        ph += f / SR
        s = 2.0 * (ph % 1.0) - 1.0
        lp += (s - lp) * 0.42
        e = (1.0 - t) ** 2.6
        if i < 70:
            e *= i / 70.0
        out[i] = lp * e
    return out


def synth_lead(f, dur, vib=0.0):
    n = int(SR * dur)
    out = [0.0] * n
    ph = 0.0
    ph2 = 0.0
    for i in range(n):
        t = i / n
        vf = 1.0 + vib * math.sin(2 * math.pi * 5.4 * i / SR)
        ph += f * vf / SR
        ph2 += f * vf * 2.004 / SR
        v = (math.sin(2 * math.pi * ph) * 0.62
             + (2.0 * (ph2 % 1.0) - 1.0) * 0.16
             + math.sin(2 * math.pi * ph * 2.0) * 0.22)
        e = min(1.0, i / (SR * 0.014))
        e *= (1.0 - t) ** 0.42
        if t > 0.86:
            e *= (1.0 - t) / 0.14
        out[i] = v * e
    return out


def with_delay(src, dly_s, fb, mixv):
    """给音轨加一层回声，不改动干声"""
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


# ---- 编曲：12 小节做「渐强 - 高潮 - 回落」的情绪曲线 ----
SECTION = [0.72, 0.76, 0.82, 0.88, 0.98, 1.00, 1.02, 1.02, 1.05, 1.05, 1.00, 0.94]

for b in range(BARS):
    base = int(b * bar * SR)
    root, chord = CH[b % 4]
    sec = SECTION[b]

    if b < 2:                       # 引子：只有铺底和稀疏低音
        pad_gain, kick_on, hat_div, arp_gain, snare_on = 0.050, False, 4, 0.0, False
    elif b < 4:                     # 进入节奏
        pad_gain, kick_on, hat_div, arp_gain, snare_on = 0.054, True, 2, 0.060, False
    elif b < 8:                     # 主歌：全鼓组 + 旋律
        pad_gain, kick_on, hat_div, arp_gain, snare_on = 0.056, True, 1, 0.100, True
    else:                           # 副歌：更密的琶音
        pad_gain, kick_on, hat_div, arp_gain, snare_on = 0.058, True, 1, 0.112, True

    # 铺底 Pad（整小节）
    add_to(pad_t, base, synth_pad(chord, bar * 1.03), pad_gain * sec)

    # Bass：八分音符推进，每小节第 4、8 个八分跳高八度
    steps = 4 if b < 2 else 8
    for k in range(steps):
        f = root * (2.0 if (steps == 8 and k in (3, 7)) else 1.0)
        dur = beat if steps == 4 else beat * 0.5
        add_to(bass_t, base + int(k * (bar / steps) * SR), synth_bass(f, dur * 0.9), 0.17 * sec)

    # 鼓组
    if kick_on:
        kicks = (0.0, 2.0) if b < 4 else (0.0, 2.0, 3.5)
        for pos in kicks:
            add_to(drum_t, base + int(pos * beat * SR), synth_kick(), 0.34 * sec)
    if snare_on:
        for pos in (1.0, 3.0):
            add_to(drum_t, base + int(pos * beat * SR), synth_snare(), 0.26 * sec)
    for k in range(16 // hat_div):
        step = beat * 0.25 * hat_div
        if hat_div == 1:
            g = 0.095 if k % 4 == 2 else (0.052 if k % 2 == 0 else 0.030)
        else:
            g = 0.070 if k % 2 == 0 else 0.040
        add_to(drum_t, base + int(k * step * SR), synth_hat(g), sec)
    if b % 4 == 0:
        add_to(drum_t, base, synth_crash(), 0.9 * sec)

    # 琶音织体
    if arp_gain > 0:
        seq = [0, 1, 2, 1, 2, 0, 1, 2]
        for k in range(16):
            f = chord[seq[k % 8]] * 4.0
            add_to(arp_t, base + int(k * beat * 0.25 * SR), synth_pluck(f, beat * 0.26), arp_gain * sec)

# ---- 主旋律（第 5 小节进入）----
MEL = []


def m(bar_, pos, dur, f):
    MEL.append((bar_ * 4 + pos, dur, f))


m(4, 0, 1.0, 659.25); m(4, 1, 1.0, 587.33); m(4, 2, 1.0, 523.25); m(4, 3, 1.0, 493.88)
m(5, 0, 0.5, 523.25); m(5, 0.5, 0.5, 587.33); m(5, 1, 1.0, 659.25); m(5, 2, 1.9, 440.00)
m(6, 0, 1.0, 659.25); m(6, 1, 1.0, 783.99); m(6, 2, 1.0, 659.25); m(6, 3, 1.0, 587.33)
m(7, 0, 1.4, 523.25); m(7, 2, 1.9, 587.33)
m(8, 0, 1.0, 659.25); m(8, 1, 1.0, 698.46); m(8, 2, 1.0, 783.99); m(8, 3, 1.0, 880.00)
m(9, 0, 1.0, 783.99); m(9, 1, 1.0, 698.46); m(9, 2, 1.0, 659.25); m(9, 3, 1.0, 587.33)
m(10, 0, 0.9, 523.25); m(10, 1, 0.45, 587.33); m(10, 1.5, 0.45, 659.25); m(10, 2, 1.9, 523.25)
m(11, 0, 1.0, 587.33); m(11, 1, 1.0, 493.88); m(11, 2, 1.9, 440.00)

for pos, dur, f in MEL:
    add_to(lead_t, int(pos * beat * SR), synth_lead(f, dur * beat, 0.004), 0.145)

# 回声只加在琶音与旋律上
arp_t = with_delay(arp_t, beat * 0.75, 0.42, 0.30)
lead_t = with_delay(lead_t, beat * 1.5, 0.30, 0.22)

# ---- 总线混音 ----
track = [0.0] * total
for i in range(total):
    track[i] = (pad_t[i] * 1.05 + bass_t[i] * 1.0 + drum_t[i] * 1.05
                + arp_t[i] * 1.0 + lead_t[i] * 1.0)

# 软限幅 + 归一
peak = max(1e-6, max(abs(v) for v in track))
norm = 1.35 / peak
for i in range(total):
    track[i] = math.tanh(track[i] * norm) * 0.86

# 首尾各 6ms 淡入淡出，消除循环接缝爆音
fade = int(SR * 0.006)
for i in range(fade):
    k = i / fade
    track[i] *= k
    track[total - 1 - i] *= k

save("bgm", track)
print("audio done")
