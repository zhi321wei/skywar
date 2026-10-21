# 苍穹战机 SkyWar

一款纵向卷轴空战射击手游（类似《雷电》系列），纯 Java + Canvas 实现，**零第三方依赖**，全部美术、音效、BGM 均由代码/脚本程序化生成。

> 当前版本：**1.0**（versionCode 1）

## 下载安装

- 仓库根目录 [`dist/SkyWar.apk`](dist/SkyWar.apk)（每次发版覆盖同一个文件）
- 或到 [Releases](../../releases) 下载对应版本

安装要求：Android 5.0（API 21）及以上，仅需竖屏，无需任何权限。

## 玩法

| 模块 | 说明 |
| --- | --- |
| 出击作战 | 10 个关卡，每关含成波敌机 + 关底 BOSS。通关获得金币（首通有额外赏金），失败仅保留击杀收获 |
| 机库改造 | 三大页签：**主武器 / 超级武器 / 飞机配件**，消耗金币升级，可随时更换装备 |
| 补给商店 | 解锁新武器、新超级武器，采购作战补给（修理包 / 护盾电池 / 超武充能器） |
| 战斗 | 单指拖动操控战机（自动开火），右下角超级武器按钮（能量满可释放，支持第二根手指触发） |

### 主武器
- **火神机炮**：高射速直射，升级增加并列炮管
- **脉冲激光**：可穿透多个目标，单点高伤
- **离子散射**：扇形弹幕，清理密集小型机

### 超级武器
- **战术核弹**：清空全屏敌弹 + 全场巨额伤害
- **电磁风暴**：全场减速 55% 并持续灼烧 6 秒
- **僚机编队**：召唤 2~4 架僚机协同开火 14 秒

### 飞机配件（各 6 级）
复合装甲 · 矢量引擎 · 护盾发生器 · 磁力线圈 · 翼下副炮

## 版本号规则

每次更新 **versionName +0.1**（1.0 → 1.1 → 1.2 …），同时 **versionCode +1**，使用同一签名证书，
可直接覆盖安装旧版本，存档保留。

修改 `VERSION` 文件后执行 `bash build.sh` 即可：

```
versionName=1.1
versionCode=2
```

## 自行构建

依赖：JDK 17、Android SDK（build-tools 36 / platform android-36）。
不依赖 Gradle，构建脚本直接调用 `aapt2 → javac → d8 → zipalign → apksigner`。

```bash
bash build.sh          # 产出 dist/SkyWar-<version>.apk 与 dist/SkyWar.apk
```

辅助脚本（Python 3，需 Pillow）：

```bash
python tools/gen_icon.py    # 生成全部密度的启动图标
python tools/gen_audio.py   # 合成音效与循环 BGM（WAV）
```

## 工程结构

```
AndroidManifest.xml
res/                      图标、字符串、主题、raw 音频
src/com/zhiwei/skywar/
├── GameActivity.java     全屏竖屏 Activity
├── GameView.java         SurfaceView 游戏循环 + 虚拟坐标缩放 + 多点触控
├── Scene.java            场景基类
├── MenuScene.java        主菜单
├── StageScene.java       关卡选择 + 任务简报
├── HangarScene.java      机库改造
├── ShopScene.java        补给商店
├── BattleScene.java      战斗（敌机/BOSS/弹幕/粒子/超级武器）
├── ResultScene.java      结算
├── Art.java              全部美术资源（矢量绘制并烘焙为 Bitmap）
├── UI.java               面板/按钮/血条/金币等 UI 绘制
├── Backdrop.java         滚动星空 + 星云背景
├── Data.java             数值表：武器/超武/配件/关卡/花费/属性
├── Save.java             SharedPreferences(JSON) 存档
└── Sfx.java              SoundPool 音效 + MediaPlayer 背景音乐
```

渲染采用固定虚拟宽度 720、高度随屏幕比例自适应的坐标系，适配 16:9 ~ 21:9 各种机型。

## 更新日志

### v1.0
- 首个版本：10 关战役、3 主武器、3 超级武器、5 类配件、商店与机库改造系统
- 程序化美术与合成音频，安装包约 1 MB
