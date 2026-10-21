package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class StageScene extends Scene {

    private float scroll;
    private final RectF[] cards = new RectF[Data.STAGES];
    private final RectF bBack = new RectF();
    private final RectF bGo = new RectF();
    private final RectF bCancel = new RectF();
    private int pressed = -1;
    private int selected = -1;

    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StageScene() {
        for (int i = 0; i < cards.length; i++) cards[i] = new RectF();
    }

    private void layout() {
        bBack.set(24f, 30f, 108f, 100f);
        float top = 138f;
        float bottom = H - 96f;
        int rows = 5;
        int gap = 14;
        float avail = bottom - top;
        float cellH = (avail - gap * (rows - 1)) / rows;
        float sideGap = 22f;
        float cellW = (W - sideGap * 2 - gap) / 2f;
        for (int i = 0; i < cards.length; i++) {
            int r = i / 2, col = i % 2;
            float l = sideGap + col * (cellW + gap);
            float tp = top + r * (cellH + gap);
            cards[i].set(l, tp, l + cellW, tp + cellH);
        }
        float pw = Math.min(620f, W - 60f);
        float pl = (W - pw) / 2f;
        float pt = Math.max(40f, (H - 560f) / 2f);
        bCancel.set(pl + 24f, pt + 432f, pl + pw / 2f - 12f, pt + 502f);
        bGo.set(pl + pw / 2f + 12f, pt + 432f, pl + pw - 24f, pt + 502f);
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
        scroll += dt * 22f;
    }

    @Override
    public void draw(Canvas c) {
        UI.resetPaint(P);   // 每帧先复位自己的画笔，避免上一帧的 alpha/style 串到这一帧
        Backdrop.draw(c, W, H, scroll);
        UI.starfield(c, W, H, t);

        // 顶栏
        UI.fillRound(c, 0, 0, W, 120f, 0, 0xB3050C18);
        UI.fillRound(c, bBack.left, bBack.top, bBack.right, bBack.bottom, 14,
                pressed == 100 ? 0xEE1E3752 : 0xCC142437);
        UI.arrowLeft(c, bBack.centerX(), bBack.centerY(), 16f, UI.CY);
        UI.bold(c, "作战关卡", W / 2f, 84f, 34f, UI.TXT, 1);
        UI.panel(c, W - 178f, 34f, W - 22f, 96f);
        UI.goldLabel(c, W - 160f, 82f, game.save.gold, 26f);

        for (int i = 0; i < cards.length; i++) drawCard(c, i, cards[i]);

        UI.text(c, "通关可获得金币奖励，首次通关有额外赏金", W / 2f, H - 52f, 18f, 0xFF6E8AA2, 1);
        UI.text(c, "已解锁 " + game.save.maxStage + " / " + Data.STAGES, W / 2f, H - 26f, 16f, 0xFF4F6A80, 1);

        if (selected >= 0) drawBrief(c);
    }

    private void drawCard(Canvas c, int idx, RectF r) {
        boolean unlocked = idx + 1 <= game.save.maxStage;
        boolean cleared = game.save.bestScore[idx] > 0;
        int tint = unlocked ? (cleared ? 0x3333E0A0 : 0x334FE0FF) : 0x22000000;
        UI.panelTint(c, r.left, r.top, r.right, r.bottom, tint);

        float padL = r.left + 18f;
        UI.bold(c, "第 " + (idx + 1) + " 关", padL, r.top + 34f, 20f, unlocked ? UI.CY : 0xFF55677A, 0);
        UI.bold(c, Data.stageTitle(idx + 1), padL, r.top + 68f, 27f, unlocked ? UI.TXT : 0xFF4C5B6B, 0);

        if (unlocked) {
            // 奖励
            float by = r.bottom - 20f;
            UI.coin(c, padL + 10f, by - 9f, 10f);
            UI.bold(c, UI.num(Data.stageReward(idx + 1)), padL + 27f, by, 19f, UI.GOLD, 0);
            if (cleared) {
                UI.text(c, "最佳 " + UI.num(game.save.bestScore[idx]), r.right - 16f, by, 16f, 0xFF55E08C, 2);
            } else {
                UI.text(c, "未通关", r.right - 16f, by, 16f, 0xFF6E8AA2, 2);
            }
            // 关卡序号水印
            UI.bold(c, Integer.toString(idx + 1), r.right - 26f, r.top + 60f, 52f, 0x1A4FE0FF, 2);
        } else {
            // 锁
            float lx = r.right - 40f, ly = r.centerY() - 6f;
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeWidth(3f);
            P.setColor(0xFF44586C);
            c.drawArc(new RectF(lx - 12, ly - 20, lx + 12, ly + 4), 180, 180, false, P);
            P.setStyle(Paint.Style.FILL);
            UI.fillRound(c, lx - 14, ly - 4, lx + 14, ly + 18, 5, 0xFF44586C);
            UI.text(c, "通关第 " + idx + " 关解锁", padL, r.bottom - 22f, 16f, 0xFF4C5B6B, 0);
        }
    }

    private void drawBrief(Canvas c) {
        P.setColor(0xCC000000);
        c.drawRect(0, 0, W, H, P);
        float pw = Math.min(620f, W - 60f);
        float pl = (W - pw) / 2f;
        float pt = Math.max(40f, (H - 560f) / 2f);
        float pb = pt + 560f;
        UI.panel(c, pl, pt, pl + pw, pb);

        int st = selected + 1;
        UI.bold(c, "任 务 简 报", W / 2f, pt + 56f, 32f, UI.CY, 1);
        UI.text(c, "第 " + st + " 关 · " + Data.stageTitle(st), W / 2f, pt + 96f, 24f, UI.TXT, 1);

        Data.Stats s = Data.stats(game.save);
        float ly = pt + 152f;
        float lx = pl + 40f;
        float rx = pl + pw / 2f + 14f;
        UI.text(c, "装甲上限", lx, ly, 19f, UI.TXT_DIM, 0);
        UI.bold(c, (int) s.maxHp + "", rx, ly, 20f, UI.TXT, 0);
        UI.text(c, "主武器", lx, ly + 36f, 19f, UI.TXT_DIM, 0);
        String wn = Data.W_NAME[s.wType] + " Lv." + s.wLv;
        if (s.b != null) wn += " + " + Data.W_NAME[s.b.type] + " Lv." + s.b.lv;
        UI.bold(c, wn, rx, ly + 36f, 18f, UI.TXT, 0);
        UI.text(c, "超级武器", lx, ly + 72f, 19f, UI.TXT_DIM, 0);
        UI.bold(c, Data.S_NAME[s.sType] + " Lv." + s.sLv, rx, ly + 72f, 20f, UI.TXT, 0);
        UI.text(c, "理论 DPS", lx, ly + 108f, 19f, UI.TXT_DIM, 0);
        UI.bold(c, UI.num(Data.dps(s)), rx, ly + 108f, 20f, 0xFF55E08C, 0);
        UI.text(c, "护盾 / 速度", lx, ly + 144f, 19f, UI.TXT_DIM, 0);
        UI.bold(c, (int) s.maxShield + " / " + (int) s.speed, rx, ly + 144f, 20f, UI.TXT, 0);

        // 奖励
        UI.fillRound(c, pl + 24f, pt + 318f, pl + pw - 24f, pt + 368f, 12, 0x14FFCC44);
        UI.text(c, "通关奖励", pl + 42f, pt + 352f, 20f, 0xFFCEA96A, 0);
        UI.coin(c, pl + pw - 142f, pt + 344f, 11f);
        UI.bold(c, UI.num(Data.stageReward(st)), pl + pw - 124f, pt + 352f, 22f, UI.GOLD, 0);
        if (s.fused) {
            UI.text(c, "融合效果 · " + s.fusionName, pl + 42f, pt + 396f, 17f, 0xFF8FE8FF, 0);
            UI.text(c, s.fusionDesc, pl + 42f, pt + 422f, 15f, 0xFF7C93A8, 0);
        } else if (game.save.bestScore[selected] == 0) {
            UI.text(c, "首次通关额外 +" + UI.num(Data.stageFirstBonus(st)), pl + 42f, pt + 396f, 17f, 0xFF55E08C, 0);
        }

        UI.button(c, bCancel, "返 回", true, pressed == 101, 2);
        UI.button(c, bGo, "出 击", true, pressed == 102, 0);
    }

    @Override
    public void onTouch(int action, float x, float y) {
        if (selected >= 0) {
            if (action == 0) {
                if (bGo.contains(x, y)) pressed = 102;
                else if (bCancel.contains(x, y)) pressed = 101;
                if (pressed > 0) Sfx.play(Sfx.UI);
            } else if (action == 2 || action == 3) {
                int id = pressed;
                pressed = -1;
                if (action == 3) return;
                if (id == 102 && bGo.contains(x, y)) {
                    game.setScene(new BattleScene(selected + 1));
                } else if (id == 101 && bCancel.contains(x, y)) {
                    selected = -1;
                }
            }
            return;
        }
        if (action == 0) {
            pressed = bBack.contains(x, y) ? 100 : -1;
            if (pressed == 100) {
                Sfx.play(Sfx.UI);
                return;
            }
            for (int i = 0; i < cards.length; i++) {
                if (cards[i].contains(x, y)) {
                    if (i + 1 <= game.save.maxStage) {
                        selected = i;
                        Sfx.play(Sfx.UI);
                    } else {
                        Sfx.play(Sfx.HIT, 0.4f);
                    }
                    break;
                }
            }
        } else if (action == 2 || action == 3) {
            if (pressed == 100 && action == 2 && bBack.contains(x, y)) {
                game.setScene(new MenuScene());
            }
            pressed = -1;
        }
    }

    @Override
    public boolean onBack() {
        if (selected >= 0) {
            selected = -1;
            return true;
        }
        game.setScene(new MenuScene());
        return true;
    }
}
