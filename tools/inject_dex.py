# -*- coding: utf-8 -*-
"""把 classes.dex 注入 aapt2 产出的基础 APK，保证 resources.arsc 位于前部。"""
import sys, zipfile, shutil, os

def main():
    src, dex, dst = sys.argv[1], sys.argv[2], sys.argv[3]
    tmp = dst + ".tmp"
    with zipfile.ZipFile(src, "r") as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            zout.writestr(item, zin.read(item.filename))
        zout.write(dex, "classes.dex")
    shutil.move(tmp, dst)
    print("   dex injected ->", dst, os.path.getsize(dst), "bytes")

main()
