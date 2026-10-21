package com.zhiwei.skywar;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** 所有美术资源在启动时用代码矢量绘制并烘焙成 Bitmap。 */
public class Art {

    public static Bitmap player;
    public static Bitmap eScout, eZig, eTank, eKam, boss;
    public static Bitmap bVulcan, bLaser, bPlasma, bEnemy, bEnemyBig;
    public static Bitmap nebula;
    public static Bitmap dot;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint S = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void build() {
        if (player != null) return;
        player = makePlayer();
        eScout = makeScout();
        eZig = makeZig();
        eTank = makeTank();
        eKam = makeKam();
        boss = makeBoss();
        bVulcan = makeVulcanBullet();
        bLaser = makeLaserBullet();
        bPlasma = makePlasmaBullet();
        bEnemy = makeEnemyBullet(15, 0xFFF0503C, 0xFFFFD9A0);
        bEnemyBig = makeEnemyBullet(27, 0xFFB44BFF, 0xFFF0C8FF);
        nebula = makeNebula();
        dot = makeDot();
    }

    private static Bitmap newBmp(int w, int h) {
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    private static void glowRect(Canvas c, Path p, int color, float blur, int alpha) {
        Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
        g.setColor(color);
        g.setAlpha(alpha);
        g.setStyle(Paint.Style.FILL);
        g.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
        c.drawPath(p, g);
    }

    private static void outline(Canvas c, Path p, int color, float w) {
        S.setStyle(Paint.Style.STROKE);
        S.setStrokeWidth(w);
        S.setColor(color);
        S.setMaskFilter(null);
        c.drawPath(p, S);
    }

    private static Path poly(float[] xy) {
        Path p = new Path();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length - 1; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.close();
        return p;
    }

    // ======================= 玩家战机 =======================
    private static Bitmap makePlayer() {
        int w = 108, h = 132;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);

        // 引擎焰
        Path flameL = poly(new float[]{40, 104, 46, 106, 43, 128, 37, 122});
        Path flameR = poly(new float[]{62, 104, 68, 106, 71, 122, 65, 128});
        glowRect(c, flameL, 0xFF4FD8FF, 6, 210);
        glowRect(c, flameR, 0xFF4FD8FF, 6, 210);

        // 尾翼
        Path tailL = poly(new float[]{44, 84, 30, 108, 34, 112, 48, 94});
        Path tailR = poly(new float[]{64, 84, 78, 108, 74, 112, 60, 94});
        P.setShader(null);
        P.setStyle(Paint.Style.FILL);
        P.setColor(0xFF5E7C93);
        c.drawPath(tailL, P);
        c.drawPath(tailR, P);

        // 主翼
        Path wingL = poly(new float[]{52, 52, 10, 92, 14, 104, 50, 90, 52, 52});
        Path wingR = poly(new float[]{56, 52, 98, 92, 94, 104, 58, 90, 58, 52});
        P.setShader(new LinearGradient(10, 52, 98, 104, new int[]{0xFF9EC4DC, 0xFF5B7E97}, null, Shader.TileMode.CLAMP));
        c.drawPath(wingL, P);
        c.drawPath(wingR, P);
        P.setShader(null);

        // 翼尖光带
        outline(c, wingL, 0xFF57E4FF, 2.2f);
        outline(c, wingR, 0xFF57E4FF, 2.2f);

        // 机身
        Path body = poly(new float[]{54, 3, 66, 38, 69, 74, 62, 108, 46, 108, 39, 74, 42, 38});
        P.setShader(new LinearGradient(39, 0, 69, 0, new int[]{0xFFA8C6DC, 0xFFF2FAFF, 0xFF7EA0B8}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, P);
        P.setShader(null);
        outline(c, body, 0xFF2E4B60, 1.6f);

        // 机身高光条
        Path hl = poly(new float[]{54, 10, 58, 44, 57, 96, 51, 96, 50, 44});
        P.setColor(0x99FFFFFF);
        c.drawPath(hl, P);

        // 座舱
        Path cock = poly(new float[]{54, 30, 62, 52, 61, 66, 47, 66, 46, 52});
        P.setShader(new LinearGradient(46, 30, 62, 66, new int[]{0xFF9BF0FF, 0xFF1B7FD8}, null, Shader.TileMode.CLAMP));
        c.drawPath(cock, P);
        P.setShader(null);
        outline(c, cock, 0xFF0C3F6B, 1.4f);
        // 座舱反光
        P.setColor(0xCCE9FFFF);
        c.drawPath(poly(new float[]{50, 36, 53, 46, 52, 52, 49, 50}), P);

        // 侧炮
        P.setColor(0xFF39485A);
        c.drawRect(41, 74, 45, 96, P);
        c.drawRect(63, 74, 67, 96, P);
        P.setColor(0xFF7FE8FF);
        c.drawRect(42, 92, 44, 98, P);
        c.drawRect(64, 92, 66, 98, P);

        // 整体外发光
        Path all = poly(new float[]{54, 3, 66, 38, 69, 74, 62, 108, 46, 108, 39, 74, 42, 38});
        glowRect(c, all, 0xFF3FD2FF, 7, 60);
        return b;
    }

    // ======================= 小型侦察机 =======================
    private static Bitmap makeScout() {
        int w = 60, h = 60;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        Path body = poly(new float[]{30, 56, 22, 30, 6, 20, 12, 8, 26, 16, 30, 4, 34, 16, 48, 8, 54, 20, 38, 30});
        glowRect(c, body, 0xFFFF3B2F, 7, 90);
        P.setShader(new LinearGradient(6, 4, 54, 56, new int[]{0xFFFF8A6A, 0xFFD3331F, 0xFF6E1B14}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, P);
        P.setShader(null);
        outline(c, body, 0xFF3B120E, 1.5f);
        P.setColor(0xFFFFE08A);
        c.drawCircle(30, 28, 4.2f, P);
        P.setColor(0xFFFFFFDD);
        c.drawCircle(30, 28, 2f, P);
        return b;
    }

    // ======================= 迂回机 =======================
    private static Bitmap makeZig() {
        int w = 68, h = 64;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        Path body = poly(new float[]{34, 60, 24, 34, 4, 30, 8, 14, 28, 20, 34, 6, 40, 20, 60, 14, 64, 30, 44, 34});
        glowRect(c, body, 0xFFFF8A22, 7, 90);
        P.setShader(new LinearGradient(4, 6, 64, 60, new int[]{0xFFFFC46A, 0xFFEF7A18, 0xFF7A3406}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, P);
        P.setShader(null);
        outline(c, body, 0xFF3A1B03, 1.5f);
        P.setColor(0xFF2A1206);
        c.drawCircle(34, 30, 7, P);
        P.setColor(0xFFFFF0B8);
        c.drawCircle(34, 30, 3, P);
        return b;
    }

    // ======================= 重装炮台机 =======================
    private static Bitmap makeTank() {
        int w = 96, h = 88;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        Path body = poly(new float[]{48, 84, 20, 66, 6, 40, 22, 16, 48, 4, 74, 16, 90, 40, 76, 66});
        glowRect(c, body, 0xFF9C5BFF, 8, 80);
        P.setShader(new LinearGradient(6, 4, 90, 84, new int[]{0xFFB79BFF, 0xFF7A46D8, 0xFF3A2170}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, P);
        P.setShader(null);
        outline(c, body, 0xFF241245, 2f);
        // 装甲板
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(2f);
        P.setColor(0x554F2E96);
        c.drawPath(poly(new float[]{28, 30, 68, 30, 74, 48, 48, 62, 22, 48}), P);
        P.setStyle(Paint.Style.FILL);
        // 炮口
        P.setColor(0xFF2A1A44);
        c.drawRect(14, 44, 24, 68, P);
        c.drawRect(72, 44, 82, 68, P);
        P.setColor(0xFFE4A8FF);
        c.drawCircle(19, 50, 3.4f, P);
        c.drawCircle(77, 50, 3.4f, P);
        // 核心
        P.setShader(new RadialGradient(48, 34, 16, new int[]{0xFFFFF2FF, 0xFFD77BFF, 0x00000000}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(48, 34, 16, P);
        P.setShader(null);
        P.setColor(0xFFFFFFFF);
        c.drawCircle(48, 34, 4.5f, P);
        return b;
    }

    // ======================= 自杀式冲撞机 =======================
    private static Bitmap makeKam() {
        int w = 52, h = 62;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        Path body = poly(new float[]{26, 58, 18, 30, 4, 16, 20, 20, 26, 2, 32, 20, 48, 16, 34, 30});
        glowRect(c, body, 0xFFFF2E2E, 8, 110);
        P.setShader(new LinearGradient(4, 2, 48, 58, new int[]{0xFFFF7B5C, 0xFFC41E1E, 0xFF5C0A0A}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(body, P);
        P.setShader(null);
        outline(c, body, 0xFF2C0505, 1.4f);
        P.setColor(0xFFFFD25C);
        c.drawCircle(26, 26, 3.6f, P);
        return b;
    }

    // ======================= BOSS =======================
    private static Bitmap makeBoss() {
        int w = 420, h = 240;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        float cx = 210;

        // 侧翼挂架
        Path wingL = poly(new float[]{116, 88, 10, 106, 4, 164, 116, 156});
        Path wingR = poly(new float[]{cx + 94, 88, cx + 200, 106, cx + 206, 164, cx + 94, 156});
        P.setShader(new LinearGradient(cx - 204, 0, cx - 96, 0, new int[]{0xFF7C889C, 0xFF323B49}, null, Shader.TileMode.CLAMP));
        c.drawPath(wingL, P);
        P.setShader(new LinearGradient(cx + 96, 0, cx + 204, 0, new int[]{0xFF323B49, 0xFF7C889C}, null, Shader.TileMode.CLAMP));
        c.drawPath(wingR, P);
        P.setShader(null);
        outline(c, wingL, 0xFF14181F, 2f);
        outline(c, wingR, 0xFF14181F, 2f);

        // 主舰体
        Path hull = poly(new float[]{
                cx, 14, cx + 30, 38, cx + 50, 68, cx + 100, 74, cx + 124, 106, cx + 124, 140,
                cx + 86, 158, cx + 48, 168, cx + 30, 206, cx, 232, cx - 30, 206, cx - 48, 168,
                cx - 86, 158, cx - 124, 140, cx - 124, 106, cx - 100, 74, cx - 50, 68, cx - 30, 38});
        glowRect(c, hull, 0xFFFF4A22, 26, 60);
        P.setShader(new LinearGradient(0, 8, 0, 232,
                new int[]{0xFF9AA7BA, 0xFF525E70, 0xFF232B37},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(hull, P);
        P.setShader(null);
        outline(c, hull, 0xFF14181F, 2.5f);

        // 装甲分段线
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(2f);
        P.setColor(0x44FFFFFF);
        c.drawLine(cx - 94, 80, cx + 94, 80, P);
        c.drawLine(cx - 104, 150, cx + 104, 150, P);
        P.setColor(0x33000000);
        c.drawLine(cx - 32, 44, cx - 32, 160, P);
        c.drawLine(cx + 32, 44, cx + 32, 160, P);
        P.setStyle(Paint.Style.FILL);

        // 前向主炮
        for (int i = -1; i <= 1; i += 2) {
            float x = cx + i * 104;
            P.setColor(0xFF39434F);
            c.drawRect(x - 16, 112, x + 16, 186, P);
            P.setColor(0xFF5A6675);
            c.drawRect(x - 16, 112, x + 16, 126, P);
            P.setColor(0xFF232A34);
            c.drawRect(x - 9, 172, x + 9, 192, P);
            P.setColor(0xFFFF6A3C);
            c.drawCircle(x, 190, 8f, P);
            P.setColor(0xFFFFE1B0);
            c.drawCircle(x, 190, 3.8f, P);
        }

        // 中央反应堆
        P.setShader(new RadialGradient(cx, 108, 70, new int[]{0xFFFFFFFF, 0xFFFFB347, 0xFFFF3A17, 0x00000000},
                new float[]{0f, 0.22f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, 108, 70, P);
        P.setShader(null);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(3f);
        P.setColor(0xCCFFD9A0);
        c.drawCircle(cx, 108, 46, P);
        P.setColor(0x88FF8A40);
        c.drawCircle(cx, 108, 31, P);
        P.setStyle(Paint.Style.FILL);
        P.setColor(0xFFFFFFFF);
        c.drawCircle(cx, 108, 14, P);

        // 顶部感应器
        for (int i = -1; i <= 1; i += 2) {
            P.setColor(0xFF0B2A38);
            c.drawCircle(cx + i * 42, 40, 10f, P);
            P.setColor(0xFF9BF6FF);
            c.drawCircle(cx + i * 42, 40, 5f, P);
            P.setColor(0xFFFFFFFF);
            c.drawCircle(cx + i * 42, 40, 2f, P);
        }
        return b;
    }

    // ======================= 子弹 =======================
    private static Bitmap makeVulcanBullet() {
        int w = 14, h = 30;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        P.setShader(new LinearGradient(0, 0, 0, h, new int[]{0x00FFFFFF, 0xFF8FF7FF, 0xFFFFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(4, 0, 10, 30), 3, 3, P);
        P.setShader(null);
        glowRect(c, rectPath(4, 0, 10, 30), 0xFF39D8FF, 4, 160);
        P.setColor(0xFFFFFFFF);
        c.drawRoundRect(new RectF(6, 8, 8, 26), 1, 1, P);
        return b;
    }

    private static Bitmap makeLaserBullet() {
        int w = 22, h = 64;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        P.setShader(new LinearGradient(0, 0, 0, h, new int[]{0x00EAFBFF, 0xFFB6F4FF, 0xFFFFFFFF, 0xFFB6F4FF, 0x00EAFBFF}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(6, 0, 16, 64), 5, 5, P);
        P.setShader(null);
        glowRect(c, rectPath(6, 0, 16, 64), 0xFF2ED0FF, 7, 190);
        P.setColor(0xFFFFFFFF);
        c.drawRoundRect(new RectF(9, 6, 13, 58), 2, 2, P);
        return b;
    }

    private static Bitmap makePlasmaBullet() {
        int w = 28, h = 28;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);
        P.setShader(new RadialGradient(14, 14, 14, new int[]{0xFFFFFFFF, 0xFFB26BFF, 0x00A64BFF}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(14, 14, 14, P);
        P.setShader(null);
        return b;
    }

    private static Bitmap makeEnemyBullet(int size, int outer, int inner) {
        Bitmap b = newBmp(size, size);
        Canvas c = new Canvas(b);
        float r = size / 2f;
        P.setShader(new RadialGradient(r, r, r, new int[]{inner, outer, 0x00000000}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(r, r, r, P);
        P.setShader(null);
        return b;
    }

    private static Path rectPath(float l, float t, float r, float bo) {
        Path p = new Path();
        p.addRoundRect(new RectF(l, t, r, bo), 4, 4, Path.Direction.CW);
        return p;
    }

    // ======================= 背景星云 =======================
    private static Bitmap makeNebula() {
        int w = 720, h = 1240;
        Bitmap b = newBmp(w, h);
        Canvas c = new Canvas(b);

        // 底色
        P.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF05070F, 0xFF0A1224, 0xFF081020, 0xFF04060E},
                new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, P);
        P.setShader(null);

        // 星云团
        int[][] clouds = {
                {120, 180, 320, 0x334A2A8C},
                {600, 420, 380, 0x2A1E5FA8},
                {200, 700, 420, 0x28306E9E},
                {560, 980, 330, 0x332C1A66},
                {360, 1180, 400, 0x221A4E7A},
        };
        for (int[] cl : clouds) {
            P.setShader(new RadialGradient(cl[0], cl[1], cl[2],
                    new int[]{cl[3], 0x00000000}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cl[0], cl[1], cl[2], P);
            P.setShader(null);
        }

        // 远景星点
        java.util.Random rnd = new java.util.Random(20261021L);
        for (int i = 0; i < 420; i++) {
            float x = rnd.nextFloat() * w;
            float y = rnd.nextFloat() * h;
            float r = 0.5f + rnd.nextFloat() * 1.3f;
            int a = 40 + rnd.nextInt(120);
            P.setColor(Color.argb(a, 200, 220, 255));
            c.drawCircle(x, y, r, P);
        }
        // 少量亮星十字闪光
        for (int i = 0; i < 14; i++) {
            float x = rnd.nextFloat() * w;
            float y = rnd.nextFloat() * h;
            P.setColor(0x99DCEBFF);
            c.drawCircle(x, y, 1.8f, P);
        }
        return b;
    }

    private static Bitmap makeDot() {
        Bitmap b = newBmp(64, 64);
        Canvas c = new Canvas(b);
        P.setShader(new RadialGradient(32, 32, 32,
                new int[]{0xFFFFFFFF, 0x80FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.35f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(32, 32, 32, P);
        P.setShader(null);
        return b;
    }
}
