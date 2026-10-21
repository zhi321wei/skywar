package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

public class ShopScene extends Scene {

    // 0..2 主武器解锁；3..4 超级武器解锁；5 双联挂架；6..8 消耗品
    private static final int GEAR = 6;
    private static final int N = 9;

    private float scroll;
    private final RectF bBack = new RectF();
    private final RectF[] cards = new RectF[N];
    private final RectF[] bBuy = new RectF[N];
    private int pressed = -1;
    private String toast = "";
    private float toastT;

    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ShopScene() {
        for (int i = 0; i < N; i++) {
            cards[i] = new RectF();
            bBuy[i] = new RectF();
        }
    }

    private void layout() {
        bBack.set(24f, 30f, 108f, 100f);
        float gap = 12f;
        float cw = (W - 44f - gap) / 2f;
        float top = 206f;
        float consH = 112f;
        float cardsBottom = H - 92f - consH - 16f;
        float ch = (cardsBottom - top - gap * 2f) / 3f;
        for (int i = 0; i < GEAR; i++) {
            int r = i / 2, col = i % 2;
            float l = 22f + col * (cw + gap);
            float tp = top + r * (ch + gap);
            cards[i].set(l, tp, l + cw, tp + ch);
            bBuy[i].set(l + 16f, tp + ch - 54f, l + cw - 16f, tp + ch - 10f);
        }
        float cw3 = (W - 44f - gap * 2f) / 3f;
        float ct = H - 92f - consH;
        for (int i = GEAR; i < N; i++) {
            int col = i - GEAR;
            float l = 22f + col * (cw3 + gap);
            cards[i].set(l, ct, l + cw3, ct + consH);
            bBuy[i].set(l + 12f, ct + consH - 46f, l + cw3 - 12f, ct + consH - 8f);
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
        scroll += dt * 20f;
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
        UI.bold(c, "补 给 商 店", W / 2f, 84f, 34f, UI.TXT, 1);
        UI.panel(c, W - 178f, 34f, W - 22f, 96f);
        UI.goldLabel(c, W - 160f, 82f, game.save.gold, 26f);

        UI.text(c, "解锁武器 · 加装双武器槽 · 采购作战补给", W / 2f, 168f, 17f, 0xFF8FA6BD, 1);

        for (int i = 0; i < N; i++) {
            if (i < GEAR) drawCard(c, i);
            else drawConsCard(c, i);
        }

        UI.text(c, "击败敌机与通关任务可获得金币", W / 2f, H - 46f, 15f, 0xFF5C7A92, 1);

        if (toastT > 0) {
            float a = Math.min(1f, toastT * 2f);
            UI.fillRound(c, W / 2f - 210f, H * 0.42f, W / 2f + 210f, H * 0.42f + 62f, 14,
                    ((int) (215 * a) << 24) | 0x0A1A2A);
            UI.bold(c, toast, W / 2f, H * 0.42f + 40f, 22f, ((int) (255 * a) << 24) | 0xFFD8A0, 1);
        }
    }

    private String pName(int i) {
        if (i < 3) return Data.W_NAME[i + 1];
        if (i < 5) return Data.S_NAME[i - 2];
        if (i == 5) return Data.DUAL_NAME;
        return Data.C_NAME[i - GEAR];
    }

    private String pDesc(int i) {
        if (i < 3) return Data.W_TAG[i + 1];
        if (i == 3) return "减速 + 持续灼烧";
        if (i == 4) return "召唤僚机协同作战";
        if (i == 5) return "解锁第二主武器槽";
        if (i == 6) return "开局装甲 +40";
        if (i == 7) return "开局护盾 +40";
        return "开局能量 +50%";
    }

    private String pGlyph(int i) {
        if (i < 3) return Data.W_GLYPH[i + 1];
        if (i == 3) return "电";
        if (i == 4) return "僚";
        if (i == 5) return "双";
        return i == 6 ? "修" : i == 7 ? "盾" : "充";
    }

    private int pPrice(int i) {
        if (i < 3) return Data.W_UNLOCK[i + 1];
        if (i < 5) return Data.S_UNLOCK[i - 2];
        if (i == 5) return Data.DUAL_PRICE;
        return Data.C_PRICE[i - GEAR];
    }

    private boolean owned(int i) {
        if (i < 3) return game.save.weaponLv[i + 1] > 0;
        if (i < 5) return game.save.supLv[i - 2] > 0;
        if (i == 5) return game.save.dual > 0;
        return false;
    }

    private void iconBox(Canvas c, int i, float icx, float icy, float half) {
        int c1, c2;
        if (i < 3) {
            c1 = 0xFF5BE6FF;
            c2 = 0xFF157BC4;
        } else if (i < 5) {
            c1 = 0xFFFFA84A;
            c2 = 0xFFB8480C;
        } else if (i == 5) {
            c1 = 0xFFC79BFF;
            c2 = 0xFF5B2FB0;
        } else {
            c1 = 0xFF55E08C;
            c2 = 0xFF1E7A4A;
        }
        P.setShader(new LinearGradient(icx - half, icy - half, icx + half, icy + half,
                new int[]{c1, c2}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(icx - half, icy - half, icx + half, icy + half), 14, 14, P);
        P.setShader(null);
        UI.bold(c, pGlyph(i), icx, icy + half * 0.38f, half * 1.06f, 0xFF04141C, 1);
    }

    private void drawCard(Canvas c, int i) {
        RectF r = cards[i];
        boolean own = owned(i);
        UI.panelTint(c, r.left, r.top, r.right, r.bottom, own ? 0x2233E0A0 : 0x224FE0FF);

        iconBox(c, i, r.left + 54f, r.top + 62f, 30f);

        UI.bold(c, pName(i), r.left + 100f, r.top + 46f, 24f, UI.TXT, 0);
        UI.text(c, pDesc(i), r.left + 100f, r.top + 74f, 16f, UI.TXT_DIM, 0);

        String st;
        int col;
        if (i == 5) {
            st = own ? "已加装 · 可携带双主武器" : "永久解锁 · 激活融合";
            col = own ? 0xFF55E08C : 0xFFC79BFF;
        } else if (own) {
            st = "已解锁 · 可在机库改造";
            col = 0xFF55E08C;
        } else {
            st = "永久解锁";
            col = 0xFF8FA6BD;
        }
        UI.text(c, st, r.left + 100f, r.top + 102f, 16f, col, 0);

        if (own) {
            UI.fillRound(c, bBuy[i].left, bBuy[i].top, bBuy[i].right, bBuy[i].bottom, 12, 0xCC123A2E);
            UI.bold(c, "已 拥 有", bBuy[i].centerX(), bBuy[i].centerY() + 7f, 20f, 0xFF55E08C, 1);
        } else {
            int price = pPrice(i);
            boolean can = game.save.gold >= price;
            UI.button(c, bBuy[i], "", can, pressed == i, 1);
            float tx = bBuy[i].centerX() - 26f;
            UI.coin(c, tx - 8f, bBuy[i].centerY() - 1f, 10f);
            UI.bold(c, UI.num(price), tx + 8f, bBuy[i].centerY() + 7f, 21f, can ? 0xFF3A2000 : 0xFF66707C, 0);
        }
    }

    private void drawConsCard(Canvas c, int i) {
        RectF r = cards[i];
        int cnt = game.save.cons[i - GEAR];
        UI.panelTint(c, r.left, r.top, r.right, r.bottom, cnt > 0 ? 0x2233E0A0 : 0x224FE0FF);

        iconBox(c, i, r.left + 38f, r.top + 40f, 22f);
        UI.bold(c, pName(i), r.left + 70f, r.top + 38f, 18f, UI.TXT, 0);
        UI.text(c, pDesc(i), r.left + 70f, r.top + 62f, 14f, UI.TXT_DIM, 0);
        UI.text(c, "持有 x" + cnt, r.left + 70f, r.top + 84f, 14f, cnt > 0 ? 0xFF9FF0C8 : 0xFF55677A, 0);

        int price = pPrice(i);
        boolean can = game.save.gold >= price && cnt < 9;
        UI.button(c, bBuy[i], "", can, pressed == i, 1);
        float tx = bBuy[i].centerX() - 24f;
        UI.coin(c, tx - 7f, bBuy[i].centerY() - 1f, 8f);
        UI.bold(c, UI.num(price), tx + 9f, bBuy[i].centerY() + 6f, 18f, can ? 0xFF3A2000 : 0xFF66707C, 0);
    }

    @Override
    public void onTouch(int action, float x, float y) {
        if (action == 0) {
            pressed = -1;
            if (bBack.contains(x, y)) {
                pressed = 100;
            } else {
                for (int i = 0; i < N; i++) if (bBuy[i].contains(x, y)) pressed = i;
            }
            if (pressed >= 0) Sfx.play(Sfx.UI);
        } else if (action == 2) {
            int id = pressed;
            pressed = -1;
            if (id == 100 && bBack.contains(x, y)) {
                game.setScene(new MenuScene());
                return;
            }
            if (id >= 0 && id < N && bBuy[id].contains(x, y)) buy(id);
        } else if (action == 3) {
            pressed = -1;
        }
    }

    private void buy(int i) {
        if (owned(i)) return;
        int price = pPrice(i);
        if (game.save.gold < price) {
            showToast("金币不足");
            Sfx.play(Sfx.HIT, 0.5f);
            return;
        }
        if (i >= GEAR) {
            if (game.save.cons[i - GEAR] >= 9) {
                showToast("携带上限 9 个");
                return;
            }
            game.save.spend(price);
            game.save.cons[i - GEAR]++;
            game.save.save();
        } else if (i < 3) {
            game.save.spend(price);
            game.save.weaponLv[i + 1] = 1;
            game.save.save();
        } else if (i < 5) {
            game.save.spend(price);
            game.save.supLv[i - 2] = 1;
            game.save.save();
        } else {
            game.save.spend(price);
            game.save.dual = 1;
            // 默认给副武器槽装一把已解锁的武器
            for (int k = 0; k < Data.WN; k++) {
                if (game.save.weaponLv[k] > 0 && k != game.save.weapon) {
                    game.save.weapon2 = k;
                    break;
                }
            }
            if (game.save.weapon2 < 0) game.save.weapon2 = game.save.weapon;
            game.save.save();
        }
        Sfx.play(Sfx.POWER);
        if (i == 5) {
            showToast("已加装双武器挂架，去机库配置副武器");
        } else {
            showToast("购买成功：" + pName(i));
        }
    }

    private void showToast(String s) {
        toast = s;
        toastT = 2.0f;
    }

    @Override
    public boolean onBack() {
        game.setScene(new MenuScene());
        return true;
    }
}
