package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

public class ShopScene extends Scene {

    // 商品：0,1 主武器解锁；2,3 超级武器解锁；4,5,6 消耗品
    private static final int N = 7;

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
        float top = 206f;
        float bottom = H - 92f;
        int rows = 4;
        float gap = 12f;
        float ch = (bottom - top - gap * (rows - 1)) / rows;
        float cw = (W - 44f - gap) / 2f;
        for (int i = 0; i < N; i++) {
            int r = i / 2, col = i % 2;
            float l = 22f + col * (cw + gap);
            float tp = top + r * (ch + gap);
            cards[i].set(l, tp, l + cw, tp + ch);
            bBuy[i].set(l + 16f, tp + ch - 58f, l + cw - 16f, tp + ch - 12f);
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

        UI.text(c, "解锁新武器与超级武器，采购作战补给", W / 2f, 158f, 18f, 0xFF8FA6BD, 1);

        for (int i = 0; i < N; i++) drawCard(c, i);

        UI.text(c, "击败敌机与通关任务可获得金币", W / 2f, H - 40f, 17f, 0xFF5C7A92, 1);

        if (toastT > 0) {
            float a = Math.min(1f, toastT * 2f);
            UI.fillRound(c, W / 2f - 200f, H * 0.44f, W / 2f + 200f, H * 0.44f + 62f, 14,
                    ((int) (210 * a) << 24) | 0x0A1A2A);
            UI.bold(c, toast, W / 2f, H * 0.44f + 40f, 22f, ((int) (255 * a) << 24) | 0xFFD8A0, 1);
        }
    }

    private String pName(int i) {
        if (i < 2) return Data.W_NAME[i + 1];
        if (i < 4) return Data.S_NAME[i - 1];
        return Data.C_NAME[i - 4];
    }

    private String pDesc(int i) {
        if (i < 2) return Data.W_TAG[i + 1];
        if (i < 4) return i == 2 ? "清屏 + 巨额伤害" : "减速 + 持续灼烧";
        if (i == 4) return "开局装甲 +40";
        if (i == 5) return "开局护盾 +40";
        return "开局能量 +50%";
    }

    private String pGlyph(int i) {
        if (i == 0) return "激";
        if (i == 1) return "散";
        if (i == 2) return "核";
        if (i == 3) return "僚";
        if (i == 4) return "修";
        if (i == 5) return "盾";
        return "充";
    }

    private int pPrice(int i) {
        if (i < 2) return Data.W_UNLOCK[i + 1];
        if (i < 4) return Data.S_UNLOCK[i - 1];
        return Data.C_PRICE[i - 4];
    }

    private boolean owned(int i) {
        if (i < 2) return game.save.weaponLv[i + 1] > 0;
        if (i < 4) return game.save.supLv[i - 1] > 0;
        return false;
    }

    private void drawCard(Canvas c, int i) {
        RectF r = cards[i];
        boolean own = owned(i);
        UI.panelTint(c, r.left, r.top, r.right, r.bottom, own ? 0x2233E0A0 : 0x224FE0FF);

        // 图标
        float icx = r.left + 54f, icy = r.top + 62f;
        int c1 = i < 2 ? 0xFF5BE6FF : i < 4 ? 0xFFFFA84A : 0xFF55E08C;
        int c2 = i < 2 ? 0xFF157BC4 : i < 4 ? 0xFFB8480C : 0xFF1E7A4A;
        P.setShader(new LinearGradient(icx - 30, icy - 30, icx + 30, icy + 30, new int[]{c1, c2}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(icx - 30, icy - 30, icx + 30, icy + 30), 14, 14, P);
        P.setShader(null);
        UI.bold(c, pGlyph(i), icx, icy + 12f, 32f, 0xFF04141C, 1);

        UI.bold(c, pName(i), r.left + 100f, r.top + 50f, 25f, UI.TXT, 0);
        UI.text(c, pDesc(i), r.left + 100f, r.top + 78f, 16f, UI.TXT_DIM, 0);

        if (i >= 4) {
            UI.text(c, "已拥有 x" + game.save.cons[i - 4], r.left + 100f, r.top + 104f, 16f, 0xFF9FF0C8, 0);
        } else {
            UI.text(c, own ? "已解锁 · 可在机库改造" : "永久解锁", r.left + 100f, r.top + 104f, 16f,
                    own ? 0xFF55E08C : 0xFF8FA6BD, 0);
        }

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
        if (i >= 4) {
            if (game.save.cons[i - 4] >= 9) {
                showToast("携带上限 9 个");
                return;
            }
            game.save.spend(price);
            game.save.cons[i - 4]++;
            game.save.save();
        } else {
            game.save.spend(price);
            if (i < 2) {
                game.save.weaponLv[i + 1] = 1;
            } else {
                game.save.supLv[i - 1] = 1;
            }
            game.save.save();
        }
        Sfx.play(Sfx.POWER);
        showToast("购买成功：" + pName(i));
    }

    private void showToast(String s) {
        toast = s;
        toastT = 1.5f;
    }

    @Override
    public boolean onBack() {
        game.setScene(new MenuScene());
        return true;
    }
}
