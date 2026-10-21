package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public class MenuScene extends Scene {

    private float scroll;
    private final RectF bStart = new RectF();
    private final RectF bHangar = new RectF();
    private final RectF bShop = new RectF();
    private final RectF bSfx = new RectF();
    private final RectF bMusic = new RectF();
    private int pressed = -1;

    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void layout() {
        float bw = Math.min(600f, W - 120f);
        float left = (W - bw) / 2f;
        float baseY = H - 200f;
        bStart.set(left, baseY - 250f, left + bw, baseY - 150f);
        float half = (bw - 24f) / 2f;
        bHangar.set(left, baseY - 130f, left + half, baseY - 40f);
        bShop.set(left + half + 24f, baseY - 130f, left + bw, baseY - 40f);
        bSfx.set(22f, 30f, 76f, 80f);
        bMusic.set(86f, 30f, 140f, 80f);
    }

    @Override
    public void onEnter() {
        super.onEnter();
        layout();
    }

    @Override
    public void onResize() {
        super.onResize();
        layout();
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        scroll += dt * 26f;
    }

    @Override
    public void draw(Canvas c) {
        UI.resetPaint(P);   // 每帧先复位自己的画笔，避免上一帧的 alpha/style 串到这一帧
        Backdrop.draw(c, W, H, scroll);
        Backdrop.stars(c, W, H, t);

        // ---- 战机展示
        float cx = W / 2f;
        float shipY = H * 0.40f + (float) Math.sin(t * 1.6f) * 9f;
        float sc = 1.85f;
        // 引擎光
        P.setShader(new RadialGradient(cx, shipY + 108 * sc, 78,
                new int[]{0x7740D8FF, 0x2230A8FF, 0x00000000}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, shipY + 105 * sc, 78, P);
        P.setShader(null);
        // 动态焰苗
        float flick = 1f + (float) Math.sin(t * 22f) * 0.16f;
        P.setColor(0xCC66E6FF);
        c.drawRoundRect(new RectF(cx - 13, shipY + 96 * sc, cx + 13, shipY + 96 * sc + 34 * flick), 12, 12, P);
        P.setColor(0xFFEAFEFF);
        c.drawRoundRect(new RectF(cx - 6, shipY + 96 * sc, cx + 6, shipY + 96 * sc + 20 * flick), 6, 6, P);

        c.save();
        c.translate(cx, shipY);
        c.scale(sc, sc);
        c.drawBitmap(Art.player, -Art.player.getWidth() / 2f, -Art.player.getHeight() / 2f, P);
        c.restore();

        // ---- 标题
        float titleY = H * 0.155f;
        UI.glowText(c, "苍 穹 战 机", cx, titleY, 74f, 0xFFF2FBFF, 0xFF2FC6FF);
        UI.bold(c, "S K Y   W A R", cx, titleY + 40f, 24f, 0xFF66D8FF, 1);
        UI.text(c, "空 战 射 击 · 装 备 改 造 · 十 大 关 卡", cx, titleY + 74f, 19f, 0xFF88A6C0, 1);

        // ---- 金币
        float gw = 150f;
        UI.panel(c, W - gw - 22f, 26f, W - 22f, 78f);
        UI.goldLabel(c, W - gw - 4f, 65f, game.save.gold, 28f);

        // 音效 / 音乐开关
        drawToggle(c, bSfx, "音", game.save.sfxOn == 1);
        drawToggle(c, bMusic, "乐", game.save.musicOn == 1);

        // ---- 按钮
        boolean hasSave = game.save.maxStage > 1 || game.save.clears > 0;
        UI.button(c, bStart, "出 击 作 战", true, pressed == 1, 0);
        UI.button(c, bHangar, "机 库 改 造", true, pressed == 2, 2);
        UI.button(c, bShop, "补 给 商 店", true, pressed == 3, 2);

        // 进度信息
        float py = bStart.top - 34f;
        String info = "最高进度  第 " + game.save.maxStage + " 关  ·  " + Data.stageTitle(game.save.maxStage);
        UI.text(c, info, cx, py, 20f, 0xFF9FBAD2, 1);
        if (!hasSave) {
            UI.text(c, "点击「出击作战」开始你的第一次任务", cx, py + 30f, 17f, 0xFF6E8AA2, 1);
        }

        UI.text(c, "v" + game.versionName, 26f, H - 26f, 17f, 0xFF4F6A80, 0);
        UI.text(c, "全部美术与音乐由程序实时生成", W - 26f, H - 26f, 15f, 0xFF3F5568, 2);
    }

    private void drawToggle(Canvas c, RectF r, String label, boolean on) {
        UI.fillRound(c, r.left, r.top, r.right, r.bottom, 12, on ? 0xCC12405A : 0xCC1A2130);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(1.4f);
        P.setColor(on ? 0xAA4FE0FF : 0x55FFFFFF);
        c.drawRoundRect(r, 12, 12, P);
        P.setStyle(Paint.Style.FILL);
        UI.bold(c, label, r.centerX(), r.centerY() + 10f, 26f, on ? UI.CY : 0xFF5A6A7A, 1);
        if (!on) {
            float m = r.centerX(), n = r.centerY();
            P.setColor(0x88FF5A4A);
            P.setStrokeWidth(2f);
            c.drawLine(r.left + 8, r.bottom - 8, r.right - 8, r.top + 8, P);
        }
    }

    @Override
    public void onTouch(int action, float x, float y) {
        if (action == 0) {
            pressed = hit(x, y);
            if (pressed == 4) {
                game.save.sfxOn = game.save.sfxOn == 1 ? 0 : 1;
                game.save.save();
                game.applyPrefs();
                Sfx.play(Sfx.UI);
            } else if (pressed == 5) {
                game.save.musicOn = game.save.musicOn == 1 ? 0 : 1;
                game.save.save();
                game.applyPrefs();
                Sfx.play(Sfx.UI);
            } else if (pressed > 0) {
                Sfx.play(Sfx.UI);
            }
        } else if (action == 2 || action == 3) {
            int id = hit(x, y);
            if (id == pressed) {
                if (id == 1) {
                    game.setScene(new StageScene());
                } else if (id == 2) {
                    game.setScene(new HangarScene());
                } else if (id == 3) {
                    game.setScene(new ShopScene());
                }
            }
            pressed = -1;
        }
    }

    private int hit(float x, float y) {
        if (bStart.contains(x, y)) return 1;
        if (bHangar.contains(x, y)) return 2;
        if (bShop.contains(x, y)) return 3;
        if (bSfx.contains(x, y)) return 4;
        if (bMusic.contains(x, y)) return 5;
        return -1;
    }

    @Override
    public boolean onBack() {
        return false;
    }
}
