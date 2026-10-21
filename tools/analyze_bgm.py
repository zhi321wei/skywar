# -*- coding: utf-8 -*-
"""粗略对比两首 BGM 的频段分布与动态（纯 Python，无 numpy）。"""
import wave
import math
import struct
import sys


def load(p):
    w = wave.open(p, 'rb')
    n = w.getnframes()
    sr = w.getframerate()
    d = struct.unpack('<%dh' % n, w.readframes(n))
    w.close()
    return [v / 32768.0 for v in d], sr


def lp(x, fc, sr):
    a = 1.0 - math.exp(-2 * math.pi * fc / sr)
    y = [0.0] * len(x)
    p = 0.0
    for i in range(len(x)):
        p += a * (x[i] - p)
        y[i] = p
    return y


def rms(x):
    return math.sqrt(sum(v * v for v in x) / max(1, len(x)))


BANDS = [(0, 200), (200, 640), (640, 2560), (2560, 5000), (5000, 11025)]


def report(name, path):
    x, sr = load(path)
    total = rms(x)
    lps = [lp(x, fc, sr) for fc in (200, 640, 2560, 5000)]
    acc = [0.0] * 5
    for i in range(len(x)):
        vals = [lps[0][i], lps[1][i] - lps[0][i], lps[2][i] - lps[1][i],
                lps[3][i] - lps[2][i], x[i] - lps[3][i]]
        for k in range(5):
            acc[k] += vals[k] * vals[k]
    tot = sum(acc) or 1
    sec = [rms(x[i:i + sr]) for i in range(0, len(x) - sr, sr)]
    mean = sum(sec) / len(sec)
    var = sum((v - mean) ** 2 for v in sec) / len(sec)
    peak = max(abs(v) for v in x)
    print('%-16s %5.2fs  RMS %.4f  peak %.3f' % (name, len(x) / sr, total, peak))
    for (lo, hi), e in zip(BANDS, acc):
        print('    %5d-%5dHz  %5.1f%%' % (lo, hi, e / tot * 100))
    print('    每秒RMS 均值 %.4f std %.4f min %.3f max %.3f'
          % (mean, math.sqrt(var), min(sec), max(sec)))


if __name__ == '__main__':
    for p in sys.argv[1:]:
        report(p.split('/')[-1], p)
        print()
