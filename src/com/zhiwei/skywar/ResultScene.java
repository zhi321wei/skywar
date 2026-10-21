package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public class ResultScene extends Scene {

    private final int stage;
    private final int score;
    private final int earned;
    private final int reward;
    private final boolean cleared;
    private final boolean first;
    private final int kills;

    private static final float PANEL_H = 820f;

    private float scroll;
    private final RectF bMain = new RectF();
    private final RectF bSecond = new RectF();
    private final RectF bHome = new RectF();
    private int pressed = -1;
    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float pt, pl, pw;

    public ResultScene(int stage, int score, int earned, int reward, boolean cleared, boolean first, int kills) {
        this.stage = stage;
        this.score = score;
        this.earned = earned;
        this.reward = reward;
        this.cleared = cleared;
        this.first = first;
        this.kills = kills;
    }

    private void layout() {
        pw = Math.min(608f, W - 56f);
        pl = (W - pw) / 2f;
        pt = Math.max(30f, (H - PANEL_H) / 2f);
        bMain.set(pl + 26f, pt + 610f, pl + pw / 2f - 10f, pt + 686f);
        bSecond.set(pl + pw / 2f + 10f, pt + 610f, pl + pw - 26f, pt + 686f);
        bHome.set(W / 2f - 140f, pt + 706f, W / 2f + 140f, pt + 776f);
    }

    @Override
    public void onEnter() {
        super.onEnter();
        layout();
        Sfx.resumeMusic();
    }

    @Override
    public void onResize() {
        super.onResize();
        layout();
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        scroll += dt * 16f;
    }

    @Override
    public void draw(Canvas c) {
        UI.resetPaint(P);   // 每帧先复位自己的画笔，避免上一帧的 alpha/style 串到这一帧
        Backdrop.draw(c, W, H, scroll);
        UI.starfield(c, W, H, t);
        UI.panel(c, pl, pt, pl + pw, pt + PANEL_H);

        int glowCol = cleared ? 0xFF33FFA8 : 0xFFFF5A4A;
        P.setShader(new RadialGradient(W / 2f, pt + 70f, 300f,
                new int[]{(glowCol & 0xFFFFFF) | 0x40000000, 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(pl + 2, pt + 2, pl + pw - 2, pt + 240f, P);
        P.setShader(null);

        UI.glowText(c, cleared ? "任 务 完 成" : "任 务 失 败", W / 2f, pt + 90f, 46f,
                cleared ? 0xFFB6FFD8 : 0xFFFFB0A8, glowCol);
        UI.text(c, "第 " + stage + " 关 · " + Data.stageTitle(stage), W / 2f, pt + 128f, 19f, 0xFF8FA6BD, 1);

        float lx = pl + 46f;
        float rx = pl + pw - 46f;
        row(c, lx, rx, pt + 196f, "击坠敌机", kills + " 架", UI.TXT);
        row(c, lx, rx, pt + 238f, "战斗得分", UI.num(score), UI.TXT);

        // 金币结算明细
        float by = pt + 282f;
        UI.fillRound(c, pl + 26f, by, pl + pw - 26f, by + 128f, 14, 0x18FFCC44);
        UI.text(c, "击杀收获", lx, by + 36f, 18f, 0xFFCEA96A, 0);
        UI.bold(c, "+" + UI.num(earned), rx, by + 36f, 20f, 0xFFFFE0A0, 2);
        if (cleared) {
            UI.text(c, "通关奖励", lx, by + 70f, 18f, 0xFFCEA96A, 0);
            UI.bold(c, "+" + UI.num(Data.stageReward(stage)), rx, by + 70f, 20f, 0xFFFFE0A0, 2);
            if (first) {
                UI.text(c, "首次通关赏金", lx, by + 104f, 18f, 0xFF55E08C, 0);
                UI.bold(c, "+" + UI.num(Data.stageFirstBonus(stage)), rx, by + 104f, 20f, 0xFF55E08C, 2);
            } else {
                UI.text(c, "重复通关（无首通赏金）", lx, by + 104f, 16f, 0xFF7C93A8, 0);
            }
        } else {
            UI.text(c, "任务失败仅保留击杀收获", lx, by + 74f, 17f, 0xFF7C93A8, 0);
        }

        // 合计
        UI.text(c, "本局合计获得", W / 2f, pt + 456f, 20f, 0xFFCEA96A, 1);
        UI.coin(c, W / 2f - 56f, pt + 512f, 15f);
        UI.bold(c, UI.num(earned + reward), W / 2f + 6f, pt + 524f, 38f, UI.GOLD, 1);
        UI.text(c, "当前金币 " + UI.num(game.save.gold), W / 2f, pt + 556f, 17f, 0xFF8FA6BD, 1);

        if (cleared && stage < Data.STAGES) {
            UI.text(c, "已解锁 第 " + (stage + 1) + " 关 · " + Data.stageTitle(stage + 1),
                    W / 2f, pt + 584f, 16f, 0xFF55E08C, 1);
        } else if (cleared) {
            UI.text(c, "全部关卡已通关，感谢你的战斗！", W / 2f, pt + 584f, 16f, 0xFFFFD166, 1);
        }

        UI.button(c, bMain, cleared && stage < Data.STAGES ? "下 一 关" : "再 战 一 次", true, pressed == 1, 0);
        UI.button(c, bSecond, "机 库 改 造", true, pressed == 2, 1);
        UI.button(c, bHome, "返 回 基 地", true, pressed == 3, 2);
    }

    private void row(Canvas c, float lx, float rx, float y, String k, String v, int vc) {
        UI.text(c, k, lx, y, 19f, UI.TXT_DIM, 0);
        UI.bold(c, v, rx, y, 21f, vc, 2);
        P.setColor(0x22FFFFFF);
        c.drawRect(lx, y + 14f, rx, y + 15f, P);
    }

    @Override
    public void onTouch(int action, float x, float y) {
        if (action == 0) {
            pressed = bMain.contains(x, y) ? 1 : bSecond.contains(x, y) ? 2 : bHome.contains(x, y) ? 3 : -1;
            if (pressed > 0) Sfx.play(Sfx.UI);
        } else if (action == 2) {
            int id = pressed;
            pressed = -1;
            if (id == 1 && bMain.contains(x, y)) {
                if (cleared && stage < Data.STAGES) game.setScene(new BattleScene(stage + 1));
                else game.setScene(new BattleScene(stage));
            } else if (id == 2 && bSecond.contains(x, y)) {
                game.setScene(new HangarScene());
            } else if (id == 3 && bHome.contains(x, y)) {
                game.setScene(new MenuScene());
            }
        } else if (action == 3) {
            pressed = -1;
        }
    }

    @Override
    public boolean onBack() {
        game.setScene(new MenuScene());
        return true;
    }
}
