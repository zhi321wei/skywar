package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public class HangarScene extends Scene {

    private float scroll;
    private final RectF bBack = new RectF();
    private final RectF[] bTabs = new RectF[3];
    private final RectF[] cards = new RectF[5];
    private final RectF[] bMain = new RectF[5];
    private final RectF[] bSub = new RectF[5];
    private int pressed = -1;
    private int tab = 0;
    private String toast = "";
    private float toastT;

    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HangarScene() {
        for (int i = 0; i < 3; i++) bTabs[i] = new RectF();
        for (int i = 0; i < 5; i++) {
            cards[i] = new RectF();
            bMain[i] = new RectF();
            bSub[i] = new RectF();
        }
        tab = 0;
    }

    private int count() {
        return tab == 2 ? 5 : 3;
    }

    private void layout() {
        bBack.set(24f, 30f, 108f, 100f);
        float gap = 12f;
        float tw = (W - 44f - gap * 2) / 3f;
        for (int i = 0; i < 3; i++) {
            float l = 22f + i * (tw + gap);
            bTabs[i].set(l, 132f, l + tw, 196f);
        }
        float top = 212f;
        float bottom = H - 152f;
        int n = count();
        float cg = 12f;
        float ch = (bottom - top - cg * (n - 1)) / n;
        for (int i = 0; i < 5; i++) {
            if (i < n) {
                float tp = top + i * (ch + cg);
                cards[i].set(22f, tp, W - 22f, tp + ch);
                bMain[i].set(cards[i].right - 200f, cards[i].bottom - 62f, cards[i].right - 112f, cards[i].bottom - 12f);
                bSub[i].set(cards[i].right - 104f, cards[i].bottom - 62f, cards[i].right - 16f, cards[i].bottom - 12f);
            } else {
                cards[i].set(0, 0, 0, 0);
                bMain[i].set(0, 0, 0, 0);
                bSub[i].set(0, 0, 0, 0);
            }
        }
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
        scroll += dt * 18f;
        if (toastT > 0) toastT -= dt;
    }

    @Override
    public void draw(Canvas c) {
        Backdrop.draw(c, W, H, scroll);
        UI.starfield(c, W, H, t);

        UI.fillRound(c, 0, 0, W, 120f, 0, 0xB3050C18);
        UI.fillRound(c, bBack.left, bBack.top, bBack.right, bBack.bottom, 14,
                pressed == 100 ? 0xEE1E3752 : 0xCC142437);
        UI.arrowLeft(c, bBack.centerX(), bBack.centerY(), 16f, UI.CY);
        UI.bold(c, "机 库 改 造", W / 2f, 84f, 34f, UI.TXT, 1);
        UI.panel(c, W - 178f, 34f, W - 22f, 96f);
        UI.goldLabel(c, W - 160f, 82f, game.save.gold, 26f);

        String[] names = {"主 武 器", "超 级 武 器", "飞 机 配 件"};
        for (int i = 0; i < 3; i++) {
            boolean on = tab == i;
            UI.fillRound(c, bTabs[i].left, bTabs[i].top, bTabs[i].right, bTabs[i].bottom, 12,
                    on ? 0xEE14648C : 0xCC101B2A);
            if (on) {
                P.setStyle(Paint.Style.STROKE);
                P.setStrokeWidth(2f);
                P.setColor(UI.CY);
                c.drawRoundRect(bTabs[i], 12, 12, P);
                P.setStyle(Paint.Style.FILL);
            }
            UI.bold(c, names[i], bTabs[i].centerX(), bTabs[i].centerY() + 9f, 23f, on ? 0xFFFFFFFF : 0xFF7C93A8, 1);
        }

        int n = count();
        for (int i = 0; i < n; i++) {
            if (tab == 2) drawPartCard(c, i);
            else drawGearCard(c, i, tab == 1);
        }

        drawSummary(c);

        if (toastT > 0) {
            float a = Math.min(1f, toastT * 2f);
            P.setColor((int) (0xCC * a) << 24 | 0x0A1A2A);
            UI.fillRound(c, W / 2f - 190f, H - 210f, W / 2f + 190f, H - 158f, 14, ((int) (200 * a) << 24) | 0x0A1A2A);
            UI.bold(c, toast, W / 2f, H - 178f, 21f, ((int) (255 * a) << 24) | 0xFFD8A0, 1);
        }
    }

    private void drawGearCard(Canvas c, int i, boolean isSuper) {
        String name = isSuper ? Data.S_NAME[i] : Data.W_NAME[i];
        int[] lvArr = isSuper ? game.save.supLv : game.save.weaponLv;
        int lv = lvArr[i];
        boolean locked = lv <= 0;
        boolean equipped = (isSuper ? game.save.sup : game.save.weapon) == i;
        RectF r = cards[i];

        UI.panelTint(c, r.left, r.top, r.right, r.bottom,
                equipped ? 0x332FE0C0 : (locked ? 0x14000000 : 0x224FE0FF));

        // 图标
        float icx = r.left + 62f, icy = r.top + 62f;
        drawGearIcon(c, isSuper, i, icx, icy, 40f, locked);

        UI.bold(c, name, r.left + 122f, r.top + 46f, 29f, locked ? 0xFF5C6C7C : UI.TXT, 0);
        UI.text(c, isSuper ? "超级武器 · 能量充满后手动释放"
                        : Data.W_TAG[i],
                r.left + 122f, r.top + 76f, 17f, locked ? 0xFF4C5B6B : UI.TXT_DIM, 0);

        // 等级
        float px = r.left + 122f, py = r.top + 108f;
        for (int k = 0; k < Data.MAX_LV; k++) {
            boolean filled = k < lv;
            UI.diamond(c, px + k * 26f, py, 9f, filled ? UI.CY : 0xFF2A3542);
        }
        UI.bold(c, locked ? "未解锁" : "Lv." + lv + "/" + Data.MAX_LV,
                px + Data.MAX_LV * 26f + 14f, py + 7f, 19f, locked ? 0xFF5C6C7C : UI.CY, 0);

        if (locked) {
            UI.text(c, isSuper ? "在补给商店中解锁后可改造" : "在补给商店中解锁后可改造",
                    r.left + 24f, r.bottom - 34f, 17f, 0xFF7C93A8, 0);
            UI.button(c, bSub[i], "去商店", true, pressed == 900 + i, 1);
            return;
        }

        // 关键数值
        String stat;
        if (isSuper) {
            stat = supEffect(i, lv);
        } else {
            Data.Stats st = Data.stats(game.save);
            stat = "伤害 " + Math.round(st.damage) + "  ·  射速 " + String.format("%.1f", 1f / st.fireInterval) + "/s";
            if (i == 0) stat += "  ·  " + st.streams + " 束";
            if (i == 1) stat += "  ·  穿透 " + st.pierce;
            if (i == 2) stat += "  ·  " + st.spread + " 发";
        }
        UI.text(c, stat, r.left + 124f, r.top + 146f, 17f, 0xFF9FBAD2, 0);

        // 装备按钮
        if (equipped) {
            UI.fillRound(c, bMain[i].left, bMain[i].top, bMain[i].right, bMain[i].bottom, 12, 0xCC10424A);
            UI.bold(c, "已装备", bMain[i].centerX(), bMain[i].centerY() + 7f, 20f, 0xFF55E08C, 1);
        } else {
            UI.button(c, bMain[i], "装备", true, pressed == 800 + i, 2);
        }

        // 升级按钮
        if (lv >= Data.MAX_LV) {
            UI.fillRound(c, bSub[i].left, bSub[i].top, bSub[i].right, bSub[i].bottom, 12, 0xCC1A2430);
            UI.bold(c, "满级", bSub[i].centerX(), bSub[i].centerY() + 7f, 19f, 0xFF7C93A8, 1);
        } else {
            int cost = isSuper ? Data.superUpCost(i, lv) : Data.weaponUpCost(i, lv);
            boolean can = game.save.gold >= cost;
            UI.button(c, bSub[i], "", can, pressed == 700 + i, 1);
            UI.bold(c, "升级", bSub[i].centerX(), bSub[i].centerY() - 2f, 17f, can ? 0xFF2A1800 : 0xFF66707C, 1);
            UI.bold(c, UI.num(cost), bSub[i].centerX(), bSub[i].centerY() + 18f, 15f, can ? 0xFF5A3200 : 0xFF66707C, 1);
        }
    }

    private String supEffect(int i, int lv) {
        if (i == 0) return "全屏伤害 " + (220 + 90 * lv) + "  ·  清空敌弹";
        if (i == 1) return "持续减速 55%  ·  灼烧 " + (14 + 6 * lv) + "/秒";
        return "僚机 " + (lv >= 4 ? 4 : 2) + " 架  ·  持续 14 秒";
    }

    private void drawPartCard(Canvas c, int i) {
        RectF r = cards[i];
        int lv = game.save.partLv[i];
        UI.panelTint(c, r.left, r.top, r.right, r.bottom, 0x224FE0FF);

        float icx = r.left + 56f, icy = r.centerY();
        drawPartIcon(c, i, icx, icy, 30f);

        float ch = r.height();
        UI.bold(c, Data.P_NAME[i], r.left + 106f, r.top + 40f, 25f, UI.TXT, 0);
        UI.text(c, Data.P_DESC[i], r.left + 106f, r.top + 68f, 16f, UI.TXT_DIM, 0);

        float px = r.left + 106f, py = r.top + 96f;
        for (int k = 0; k < Data.MAX_LV; k++) {
            UI.diamond(c, px + k * 24f, py, 8.5f, k < lv ? 0xFF55E08C : 0xFF2A3542);
        }
        UI.bold(c, "Lv." + lv, px + Data.MAX_LV * 24f + 10f, py + 7f, 18f, 0xFF55E08C, 0);
        if (ch >= 150f) {
            UI.text(c, "当前 " + partValue(i, lv), r.left + 106f, r.top + 126f, 15f, 0xFF8FA6BD, 0);
        }

        if (lv >= Data.MAX_LV) {
            UI.fillRound(c, bSub[i].left, bSub[i].top, bSub[i].right, bSub[i].bottom, 12, 0xCC1A2430);
            UI.bold(c, "满级", bSub[i].centerX(), bSub[i].centerY() + 7f, 19f, 0xFF7C93A8, 1);
        } else {
            int cost = Data.partUpCost(i, lv);
            boolean can = game.save.gold >= cost;
            UI.button(c, bSub[i], "", can, pressed == 700 + i, 1);
            UI.bold(c, "改造", bSub[i].centerX(), bSub[i].centerY() - 2f, 17f, can ? 0xFF2A1800 : 0xFF66707C, 1);
            UI.bold(c, UI.num(cost), bSub[i].centerX(), bSub[i].centerY() + 18f, 15f, can ? 0xFF5A3200 : 0xFF66707C, 1);
        }
    }

    private String partValue(int i, int lv) {
        if (i == 0) return (100 + 22 * lv) + " 装甲";
        if (i == 1) return (560 + 42 * lv) + " 速度";
        if (i == 2) return (18 + 20 * lv) + " 护盾";
        if (i == 3) return "拾取 " + (62 + 26 * lv) + " / 金币 +" + (6 * lv) + "%";
        return lv == 0 ? "无副炮" : lv + " 对副炮";
    }

    private void drawSummary(Canvas c) {
        Data.Stats s = Data.stats(game.save);
        UI.panel(c, 22f, H - 142f, W - 22f, H - 18f);
        float y = H - 112f;
        float lx = 44f;
        UI.text(c, "装甲", lx, y, 16f, UI.TXT_DIM, 0);
        UI.bold(c, (int) s.maxHp + "", lx, y + 26f, 22f, 0xFF6FE0FF, 0);
        UI.text(c, "护盾", lx + 108f, y, 16f, UI.TXT_DIM, 0);
        UI.bold(c, (int) s.maxShield + "", lx + 108f, y + 26f, 22f, 0xFF9BD0FF, 0);
        UI.text(c, "DPS", lx + 216f, y, 16f, UI.TXT_DIM, 0);
        UI.bold(c, UI.num(Data.dps(s)), lx + 216f, y + 26f, 22f, 0xFF55E08C, 0);
        UI.text(c, "副炮", lx + 330f, y, 16f, UI.TXT_DIM, 0);
        UI.bold(c, s.sideGuns + " 对", lx + 330f, y + 26f, 22f, UI.TXT, 0);
        // 携带的补给
        UI.text(c, "携带补给", W - 288f, y - 14f, 15f, UI.TXT_DIM, 0);
        float cx = W - 288f;
        for (int i = 0; i < 3; i++) {
            int cnt = game.save.cons[i];
            float bx = cx + i * 76f;
            UI.fillRound(c, bx, y + 2f, bx + 68f, y + 28f, 8, cnt > 0 ? 0x332FE0C0 : 0x1AFFFFFF);
            UI.text(c, Data.C_NAME[i].substring(0, 2) + " x" + cnt, bx + 8f, y + 22f, 14f,
                    cnt > 0 ? 0xFF9FF0C8 : 0xFF55677A, 0);
        }
    }

    private void drawGearIcon(Canvas c, boolean isSuper, int type, float cx, float cy, float s, boolean locked) {
        int col = locked ? 0xFF3A4756 : (isSuper ? 0xFFFF9A3C : UI.CY);
        if (isSuper) {
            if (type == 0) {
                P.setShader(new RadialGradient(cx, cy, s, new int[]{0xFFFFFFFF, 0xFFFFB03C, 0x00FF5A00},
                        new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, s, P);
                P.setShader(null);
                P.setStyle(Paint.Style.STROKE);
                P.setStrokeWidth(2.5f);
                P.setColor(0xAAFFD9A0);
                c.drawCircle(cx, cy, s * 1.15f, P);
                P.setStyle(Paint.Style.FILL);
            } else if (type == 1) {
                Path p = new Path();
                p.moveTo(cx + s * 0.25f, cy - s);
                p.lineTo(cx - s * 0.55f, cy + s * 0.12f);
                p.lineTo(cx - s * 0.02f, cy + s * 0.12f);
                p.lineTo(cx - s * 0.22f, cy + s);
                p.lineTo(cx + s * 0.6f, cy - s * 0.15f);
                p.lineTo(cx + s * 0.05f, cy - s * 0.15f);
                p.close();
                P.setColor(col);
                c.drawPath(p, P);
            } else {
                for (int k = -1; k <= 1; k += 2) {
                    Path p = new Path();
                    p.moveTo(cx + k * s * 0.5f, cy - s * 0.8f);
                    p.lineTo(cx + k * s * 0.5f, cy + s * 0.8f);
                    p.lineTo(cx + k * s * 0.06f, cy + s * 0.1f);
                    p.close();
                    P.setColor(col);
                    c.drawPath(p, P);
                }
            }
            return;
        }
        P.setColor(col);
        if (type == 0) {
            for (int k = -1; k <= 1; k++) {
                c.drawRoundRect(new RectF(cx - s * 0.45f + k * s * 0.42f, cy - s, cx - s * 0.52f + k * s * 0.42f + s * 0.16f, cy + s), 3, 3, P);
            }
        } else if (type == 1) {
            c.drawRoundRect(new RectF(cx - s * 0.16f, cy - s, cx + s * 0.16f, cy + s), 4, 4, P);
            P.setColor(0x55FFFFFF);
            c.drawRoundRect(new RectF(cx - s * 0.22f, cy - s * 0.4f, cx + s * 0.22f, cy + s * 0.4f), 3, 3, P);
        } else {
            for (int k = -1; k <= 1; k++) {
                P.setColor(k == 0 ? col : 0xAA4FE0FF);
                c.drawCircle(cx + k * s * 0.55f, cy + Math.abs(k) * s * 0.3f, s * 0.24f, P);
            }
        }
    }

    private void drawPartIcon(Canvas c, int type, float cx, float cy, float s) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(3f);
        P.setColor(0xFF55E08C);
        if (type == 0) {
            Path p = new Path();
            p.moveTo(cx, cy - s);
            p.lineTo(cx + s * 0.8f, cy - s * 0.5f);
            p.lineTo(cx + s * 0.8f, cy + s * 0.6f);
            p.lineTo(cx, cy + s);
            p.lineTo(cx - s * 0.8f, cy + s * 0.6f);
            p.lineTo(cx - s * 0.8f, cy - s * 0.5f);
            p.close();
            c.drawPath(p, P);
        } else if (type == 1) {
            c.drawCircle(cx, cy, s * 0.55f, P);
            for (int k = 0; k < 4; k++) {
                float a = (float) (k * Math.PI / 2 + Math.PI / 4);
                c.drawLine(cx + (float) Math.cos(a) * s * 0.65f, cy + (float) Math.sin(a) * s * 0.65f,
                        cx + (float) Math.cos(a) * s, cy + (float) Math.sin(a) * s, P);
            }
        } else if (type == 2) {
            c.drawCircle(cx, cy, s * 0.75f, P);
            c.drawCircle(cx, cy, s * 0.4f, P);
        } else if (type == 3) {
            c.drawArc(new RectF(cx - s * 0.8f, cy - s * 0.5f, cx + s * 0.8f, cy + s * 0.9f), 200, 140, false, P);
            c.drawLine(cx, cy - s * 0.9f, cx, cy + s * 0.2f, P);
        } else {
            c.drawRect(cx - s * 0.3f, cy - s * 0.9f, cx + s * 0.3f, cy + s * 0.5f, P);
            c.drawLine(cx - s * 0.3f, cy + s * 0.5f, cx - s * 0.7f, cy + s * 0.9f, P);
            c.drawLine(cx + s * 0.3f, cy + s * 0.5f, cx + s * 0.7f, cy + s * 0.9f, P);
        }
        P.setStyle(Paint.Style.FILL);
    }

    @Override
    public void onTouch(int action, float x, float y) {
        if (action == 0) {
            pressed = hit(x, y);
            if (pressed > 0 && pressed < 700) Sfx.play(Sfx.UI);
            if (pressed >= 700) Sfx.play(Sfx.UI);
            if (pressed == 100) Sfx.play(Sfx.UI);
        } else if (action == 2) {
            int id = hit(x, y);
            if (id != pressed) {
                pressed = -1;
                return;
            }
            pressed = -1;
            if (id == 100) {
                game.setScene(new MenuScene());
            } else if (id >= 1 && id <= 3) {
                tab = id - 1;
                layout();
            } else if (id >= 800 && id < 805) {
                int i = id - 800;
                if (tab == 0) game.save.weapon = i;
                else if (tab == 1) game.save.sup = i;
                game.save.save();
                showToast("已装备 " + (tab == 1 ? Data.S_NAME[i] : Data.W_NAME[i]));
            } else if (id >= 900 && id < 905) {
                int i = id - 900;
                game.setScene(new ShopScene());
            } else if (id >= 700 && id < 705) {
                upgrade(id - 700);
            }
        } else if (action == 3) {
            pressed = -1;
        }
    }

    private void upgrade(int i) {
        if (tab == 2) {
            int lv = game.save.partLv[i];
            if (lv >= Data.MAX_LV) return;
            int cost = Data.partUpCost(i, lv);
            if (!game.save.spend(cost)) {
                showToast("金币不足");
                Sfx.play(Sfx.HIT, 0.5f);
                return;
            }
            game.save.partLv[i] = lv + 1;
            game.save.save();
            Sfx.play(Sfx.POWER);
            showToast(Data.P_NAME[i] + " 改造完成 Lv." + (lv + 1));
            return;
        }
        int[] lvArr = tab == 1 ? game.save.supLv : game.save.weaponLv;
        int lv = lvArr[i];
        if (lv <= 0) {
            game.setScene(new ShopScene());
            return;
        }
        if (lv >= Data.MAX_LV) return;
        int cost = tab == 1 ? Data.superUpCost(i, lv) : Data.weaponUpCost(i, lv);
        if (!game.save.spend(cost)) {
            showToast("金币不足，去通关赚取吧");
            Sfx.play(Sfx.HIT, 0.5f);
            return;
        }
        lvArr[i] = lv + 1;
        game.save.save();
        Sfx.play(Sfx.POWER);
        showToast((tab == 1 ? Data.S_NAME[i] : Data.W_NAME[i]) + " 升至 Lv." + (lv + 1));
    }

    private void showToast(String s) {
        toast = s;
        toastT = 1.6f;
    }

    private int hit(float x, float y) {
        if (bBack.contains(x, y)) return 100;
        for (int i = 0; i < 3; i++) if (bTabs[i].contains(x, y)) return i + 1;
        int n = count();
        for (int i = 0; i < n; i++) {
            if (bSub[i].contains(x, y)) {
                boolean locked = tab != 2 && (tab == 0 ? game.save.weaponLv[i] : game.save.supLv[i]) <= 0;
                return locked ? 900 + i : 700 + i;
            }
            if (tab != 2 && bMain[i].contains(x, y)) return 800 + i;
        }
        return -1;
    }

    @Override
    public boolean onBack() {
        game.setScene(new MenuScene());
        return true;
    }
}
