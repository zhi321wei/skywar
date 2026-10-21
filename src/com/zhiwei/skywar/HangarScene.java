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

    // 主 / 副武器选择弹窗
    private int pick = -1;
    private final RectF bPickMain = new RectF();
    private final RectF bPickSub = new RectF();
    private final RectF bPickCancel = new RectF();

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
        if (tab == 2) return 5;
        if (tab == 0) return Data.WN;
        return 3;
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
        float bottom = H - 200f;
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

        float pw = Math.min(440f, W - 80f);
        float pl = (W - pw) / 2f;
        float pt = H * 0.5f - 150f;
        bPickMain.set(pl + 20f, pt + 100f, pl + pw - 20f, pt + 168f);
        bPickSub.set(pl + 20f, pt + 182f, pl + pw - 20f, pt + 250f);
        bPickCancel.set(pl + 20f, pt + 264f, pl + pw - 20f, pt + 320f);
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
        UI.resetPaint(P);   // 每帧先复位自己的画笔，避免上一帧的 alpha/style 串到这一帧
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
            UI.fillRound(c, W / 2f - 200f, H - 262f, W / 2f + 200f, H - 208f, 14,
                    ((int) (200 * a) << 24) | 0x0A1A2A);
            UI.bold(c, toast, W / 2f, H - 228f, 20f, ((int) (255 * a) << 24) | 0xFFD8A0, 1);
        }

        if (pick >= 0) drawPick(c);
    }

    private void drawPick(Canvas c) {
        P.setColor(0xB3000000);
        c.drawRect(0, 0, W, H, P);
        float pw = Math.min(440f, W - 80f);
        float pl = (W - pw) / 2f;
        float pt = H * 0.5f - 150f;
        UI.panel(c, pl, pt, pl + pw, pt + 340f);
        UI.bold(c, "配 置 武 器", W / 2f, pt + 52f, 27f, UI.CY, 1);
        UI.text(c, Data.W_NAME[pick], W / 2f, pt + 84f, 19f, UI.TXT, 1);

        boolean isMain = game.save.weapon == pick;
        boolean isSub = game.save.weapon2 == pick;

        if (isMain) {
            UI.fillRound(c, bPickMain.left, bPickMain.top, bPickMain.right, bPickMain.bottom, 12, 0xCC10424A);
            UI.bold(c, "当前主武器", bPickMain.centerX(), bPickMain.centerY() + 8f, 21f, 0xFF55E08C, 1);
        } else {
            UI.button(c, bPickMain, "设为主武器", true, pressed == 500, 0);
        }

        if (isSub) {
            UI.fillRound(c, bPickSub.left, bPickSub.top, bPickSub.right, bPickSub.bottom, 12, 0xCC3A2360);
            UI.bold(c, "当前副武器", bPickSub.centerX(), bPickSub.centerY() + 8f, 21f, 0xFFC79BFF, 1);
        } else {
            UI.button(c, bPickSub, "设为副武器", true, pressed == 501, 2);
            UI.text(c, "融合 · " + Data.fusionName(game.save.weapon, pick),
                    bPickSub.centerX(), bPickSub.bottom + 4f, 14f, 0xFF8FA6BD, 1);
        }

        UI.button(c, bPickCancel, "取 消", true, pressed == 502, 3);
    }

    private void drawGearCard(Canvas c, int i, boolean isSuper) {
        String name = isSuper ? Data.S_NAME[i] : Data.W_NAME[i];
        int[] lvArr = isSuper ? game.save.supLv : game.save.weaponLv;
        int lv = lvArr[i];
        boolean locked = lv <= 0;
        boolean isMain = (isSuper ? game.save.sup : game.save.weapon) == i;
        boolean isSub = !isSuper && game.save.dual > 0 && game.save.weapon2 == i;
        boolean equipped = isMain || isSub;
        RectF r = cards[i];

        int tint = 0x224FE0FF;
        if (locked) tint = 0x14000000;
        else if (isMain) tint = 0x332FE0C0;
        else if (isSub) tint = 0x33C79BFF;
        UI.panelTint(c, r.left, r.top, r.right, r.bottom, tint);

        float icx = r.left + 62f, icy = r.top + 62f;
        drawGearIcon(c, isSuper, i, icx, icy, 40f, locked);

        UI.bold(c, name, r.left + 122f, r.top + 46f, 29f, locked ? 0xFF5C6C7C : UI.TXT, 0);
        if (isSub) {
            float nx = r.left + 122f + name.length() * 29f + 8f;
            UI.fillRound(c, nx, r.top + 24f, nx + 36f, r.top + 52f, 8, 0xCC5B2FB0);
            UI.bold(c, "副", nx + 18f, r.top + 46f, 18f, 0xFFE6D8FF, 1);
        }

        UI.text(c, isSuper ? "超级武器 · 能量充满后手动释放"
                        : Data.W_TAG[i],
                r.left + 122f, r.top + 76f, 17f, locked ? 0xFF4C5B6B : UI.TXT_DIM, 0);

        // 等级
        float px = r.left + 122f, py = r.top + 108f;
        for (int k = 0; k < Data.MAX_LV; k++) {
            boolean filled = k < lv;
            UI.diamond(c, px + k * 26f, py, 9f, filled ? (isSub ? 0xFFC79BFF : UI.CY) : 0xFF2A3542);
        }
        UI.bold(c, locked ? "未解锁" : "Lv." + lv + "/" + Data.MAX_LV,
                px + Data.MAX_LV * 26f + 14f, py + 7f, 19f,
                locked ? 0xFF5C6C7C : (isSub ? 0xFFC79BFF : UI.CY), 0);

        if (locked) {
            UI.text(c, "在补给商店中解锁后可改造", r.left + 24f, r.bottom - 34f, 17f, 0xFF7C93A8, 0);
            UI.button(c, bSub[i], "去商店", true, pressed == 900 + i, 1);
            return;
        }

        // 关键数值
        String stat;
        if (isSuper) {
            stat = supEffect(i, lv);
        } else {
            Data.Wpn w = Data.wpn(i, lv);
            stat = Data.wpnStat(w);
            if ((isMain || isSub) && game.save.dual > 0 && game.save.weapon2 >= 0) {
                stat += "  ·  融合 " + Data.fusionName(game.save.weapon, game.save.weapon2);
            }
        }
        UI.text(c, stat, r.left + 124f, r.top + 146f, 16f, 0xFF9FBAD2, 0);

        // 装备按钮
        if (equipped) {
            UI.fillRound(c, bMain[i].left, bMain[i].top, bMain[i].right, bMain[i].bottom, 12,
                    isSub ? 0xCC3A2360 : 0xCC10424A);
            UI.bold(c, isSub ? "副武器" : "主武器", bMain[i].centerX(), bMain[i].centerY() + 7f, 19f,
                    isSub ? 0xFFC79BFF : 0xFF55E08C, 1);
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

        // 双武器解锁提示
        if (!isSuper && game.save.dual == 0 && i == 0) {
            UI.text(c, "商店可加装双武器挂架", r.left + 24f, r.top + 22f, 14f, 0xFF7C93A8, 0);
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
        UI.panel(c, 22f, H - 190f, W - 22f, H - 18f);

        // 第一行：四项核心数值
        float y = H - 154f;
        float lx = 44f;
        UI.text(c, "装甲", lx, y, 15f, UI.TXT_DIM, 0);
        UI.bold(c, (int) s.maxHp + "", lx, y + 26f, 21f, 0xFF6FE0FF, 0);
        UI.text(c, "护盾", lx + 104f, y, 15f, UI.TXT_DIM, 0);
        UI.bold(c, (int) s.maxShield + "", lx + 104f, y + 26f, 21f, 0xFF9BD0FF, 0);
        UI.text(c, "DPS", lx + 208f, y, 15f, UI.TXT_DIM, 0);
        UI.bold(c, UI.num(Data.dps(s)), lx + 208f, y + 26f, 21f, 0xFF55E08C, 0);
        UI.text(c, "副炮", lx + 324f, y, 15f, UI.TXT_DIM, 0);
        UI.bold(c, s.sideGuns + " 对", lx + 324f, y + 26f, 21f, UI.TXT, 0);

        // 第二行：携带补给
        float y2 = H - 92f;
        UI.text(c, "携带补给", lx, y2, 14f, UI.TXT_DIM, 0);
        float cx = lx + 84f;
        for (int i = 0; i < 3; i++) {
            int cnt = game.save.cons[i];
            float bx = cx + i * 84f;
            UI.fillRound(c, bx, y2 - 16f, bx + 76f, y2 + 8f, 8, cnt > 0 ? 0x332FE0C0 : 0x1AFFFFFF);
            UI.text(c, Data.C_NAME[i].substring(0, 2) + " x" + cnt, bx + 8f, y2 + 3f, 14f,
                    cnt > 0 ? 0xFF9FF0C8 : 0xFF55677A, 0);
        }

        // 第三行：融合状态
        float y3 = H - 44f;
        if (s.fused) {
            UI.bold(c, "融合 · " + s.fusionName, lx, y3, 17f, 0xFFB6F4FF, 0);
            UI.text(c, s.fusionDesc, lx + 160f, y3, 15f, 0xFF7C93A8, 0);
        } else if (game.save.dual > 0) {
            UI.text(c, "已加装双武器挂架 · 点击主武器卡片配置副武器", lx, y3, 15f, 0xFFC79BFF, 0);
        } else {
            UI.text(c, "未加装双武器挂架 · 补给商店可购买", lx, y3, 15f, 0xFF5C7A92, 0);
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
        } else if (type == 2) {
            for (int k = -1; k <= 1; k++) {
                P.setColor(k == 0 ? col : 0xAA4FE0FF);
                c.drawCircle(cx + k * s * 0.55f, cy + Math.abs(k) * s * 0.3f, s * 0.24f, P);
            }
        } else {
            // 追猎导弹
            P.setColor(col);
            for (int k = -1; k <= 1; k += 2) {
                Path p = new Path();
                p.moveTo(cx + k * s * 0.42f, cy - s * 0.9f);
                p.lineTo(cx + k * s * 0.62f, cy + s * 0.2f);
                p.lineTo(cx + k * s * 0.62f, cy + s * 0.9f);
                p.lineTo(cx + k * s * 0.22f, cy + s * 0.5f);
                p.lineTo(cx + k * s * 0.22f, cy - s * 0.5f);
                p.close();
                c.drawPath(p, P);
            }
            P.setColor(0xFFFF8A3C);
            c.drawCircle(cx, cy + s * 0.15f, s * 0.16f, P);
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
        if (pick >= 0) {
            if (action == 0) {
                pressed = -1;
                if (bPickMain.contains(x, y)) pressed = 500 + 0;
                else if (bPickSub.contains(x, y)) pressed = 501;
                else if (bPickCancel.contains(x, y)) pressed = 502;
                if (pressed > 0) Sfx.play(Sfx.UI);
            } else if (action == 2) {
                int id = pressed;
                pressed = -1;
                if (id == 500) {
                    equip(pick, true);
                    pick = -1;
                } else if (id == 501) {
                    equip(pick, false);
                    pick = -1;
                } else if (id == 502) {
                    pick = -1;
                }
            } else if (action == 3) {
                pressed = -1;
            }
            return;
        }

        if (action == 0) {
            pressed = hit(x, y);
            if (pressed > 0) Sfx.play(Sfx.UI);
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
            } else if (id >= 600 && id < 605) {
                if (tab == 0 && game.save.dual > 0) pick = id - 600;
            } else if (id >= 800 && id < 805) {
                int i = id - 800;
                if (tab == 0) {
                    if (game.save.dual > 0 && game.save.weaponLv[i] > 0) {
                        pick = i;
                    } else {
                        equip(i, true);
                    }
                } else if (tab == 1) {
                    game.save.sup = i;
                    game.save.save();
                    showToast("已装备 " + Data.S_NAME[i]);
                }
            } else if (id >= 900 && id < 905) {
                game.setScene(new ShopScene());
            } else if (id >= 700 && id < 705) {
                upgrade(id - 700);
            }
        } else if (action == 3) {
            pressed = -1;
        }
    }

    private void equip(int i, boolean asMain) {
        if (i < 0 || i >= Data.WN || game.save.weaponLv[i] <= 0) return;
        if (asMain) {
            if (game.save.weapon2 == i) game.save.weapon2 = game.save.weapon;
            game.save.weapon = i;
            game.save.save();
            showToast("主武器 · " + Data.W_NAME[i]);
        } else {
            if (game.save.dual == 0) {
                showToast("需要先购买双联火控挂架");
                return;
            }
            game.save.weapon2 = i;
            game.save.save();
            showToast("副武器 · " + Data.W_NAME[i] + "（" + Data.fusionName(game.save.weapon, i) + "）");
        }
        Sfx.play(Sfx.POWER);
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
        toastT = 1.8f;
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
            if (tab == 0 && cards[i].contains(x, y)) return 600 + i;
        }
        return -1;
    }

    @Override
    public boolean onBack() {
        if (pick >= 0) {
            pick = -1;
            return true;
        }
        game.setScene(new MenuScene());
        return true;
    }
}
