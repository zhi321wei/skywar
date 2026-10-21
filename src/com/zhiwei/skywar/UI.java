package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** UI 绘制工具集（全部使用虚拟坐标） */
public class UI {

    public static final int CY = 0xFF4FE0FF;     // 主青色
    public static final int CY_D = 0xFF1B7FA8;
    public static final int GOLD = 0xFFFFCC44;
    public static final int TXT = 0xFFE8F4FF;
    public static final int TXT_DIM = 0xFF8FA6BD;
    public static final int RED = 0xFFFF5A4A;
    public static final int GREEN = 0xFF55E08C;
    public static final int PINK = 0xFFFF6BD6;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint T = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint TP = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        T.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        TP.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    /**
     * 每帧开头复位共享画笔。
     * 上一帧 panel/button 留下的 STROKE、渐变 shader、半透明 alpha 会污染下一帧，
     * 轻则面板发白、文字带描边，重则整屏像蒙了一层雾（用户说的「画面变透明」）。
     */
    static void resetPaints() {
        resetPaint(P);
        resetPaint(T);
        resetPaint(TP);
    }

    /**
     * 复位任意共享画笔。场景里各自持有一支复用的 Paint，同样会在帧之间串状态：
     * 上一帧最后一次 setAlpha(60) / setStyle(STROKE) / setShader(渐变) 会原封不动留给下一帧，
     * 于是背景图被画成半透明、面板只剩描边——这就是「画面慢慢变透明」的来源。
     */
    public static void resetPaint(Paint p) {
        if (p == null) return;
        p.setShader(null);
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(1f);
        p.setColor(0xFFFFFFFF);
        p.setXfermode(null);
        p.setShadowLayer(0f, 0f, 0f, 0);
        p.setMaskFilter(null);
        p.setColorFilter(null);
    }

    public static String num(int v) {
        String s = Integer.toString(Math.max(0, v));
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) sb.append(',');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    public static void text(Canvas c, String s, float x, float y, float size, int color, int align) {
        T.setTextSize(size);
        T.setColor(color);
        T.setTextAlign(align == 0 ? Paint.Align.LEFT : align == 1 ? Paint.Align.CENTER : Paint.Align.RIGHT);
        c.drawText(s, x, y, T);
    }

    public static void bold(Canvas c, String s, float x, float y, float size, int color, int align) {
        TP.setTextSize(size);
        TP.setColor(color);
        TP.setTextAlign(align == 0 ? Paint.Align.LEFT : align == 1 ? Paint.Align.CENTER : Paint.Align.RIGHT);
        c.drawText(s, x, y, TP);
    }

    public static void glowText(Canvas c, String s, float x, float y, float size, int color, int glow) {
        TP.setTextSize(size);
        TP.setTextAlign(Paint.Align.CENTER);
        TP.setColor(glow);
        TP.setStyle(Paint.Style.STROKE);
        TP.setStrokeWidth(size * 0.10f);
        c.drawText(s, x, y, TP);
        TP.setStyle(Paint.Style.FILL);
        TP.setColor(color);
        c.drawText(s, x, y, TP);
    }

    public static void fillRound(Canvas c, float l, float t, float r, float b, float rad, int color) {
        P.setShader(null);
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        c.drawRoundRect(new RectF(l, t, r, b), rad, rad, P);
    }

    /** 玻璃质感面板 */
    public static void panel(Canvas c, float l, float t, float r, float b) {
        fillRound(c, l, t, r, b, 18, 0xCC0B1524);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(1.6f);
        P.setColor(0x664FE0FF);
        c.drawRoundRect(new RectF(l, t, r, b), 18, 18, P);
        P.setStyle(Paint.Style.FILL);
        // 顶部高光
        P.setShader(new LinearGradient(0, t, 0, t + 42, new int[]{0x1AFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(l + 1, t + 1, r - 1, t + 42), 17, 17, P);
        P.setShader(null);
    }

    public static void panelTint(Canvas c, float l, float t, float r, float b, int tint) {
        fillRound(c, l, t, r, b, 18, 0xCC0B1524);
        P.setShader(new LinearGradient(l, t, r, b, new int[]{tint, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(l, t, r, b), 18, 18, P);
        P.setShader(null);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(1.6f);
        P.setColor(0x664FE0FF);
        c.drawRoundRect(new RectF(l, t, r, b), 18, 18, P);
        P.setStyle(Paint.Style.FILL);
    }

    /** 按钮：style 0=主色 1=金币 2=暗色 3=危险 */
    public static void button(Canvas c, RectF r, String label, boolean enabled, boolean pressed, int style) {
        int c1, c2, tc;
        switch (style) {
            case 1:
                c1 = 0xFFFFD65C;
                c2 = 0xFFE08A16;
                tc = 0xFF2A1800;
                break;
            case 2:
                c1 = 0xFF243244;
                c2 = 0xFF16202E;
                tc = TXT;
                break;
            case 3:
                c1 = 0xFFFF7A66;
                c2 = 0xFFC0241C;
                tc = 0xFFFFFFFF;
                break;
            default:
                c1 = 0xFF5BE6FF;
                c2 = 0xFF1587C4;
                tc = 0xFF03202C;
                break;
        }
        if (!enabled) {
            c1 = 0xFF2A3340;
            c2 = 0xFF1A212B;
            tc = 0xFF66707C;
        }
        float off = pressed ? 2.5f : 0f;
        // 底部投影
        fillRound(c, r.left, r.top + 4, r.right, r.bottom + 4, 14, 0x66000000);
        P.setShader(new LinearGradient(0, r.top, 0, r.bottom, new int[]{c1, c2}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(r.left, r.top + off, r.right, r.bottom + off), 14, 14, P);
        P.setShader(null);
        // 内高光
        P.setShader(new LinearGradient(0, r.top, 0, r.top + (r.bottom - r.top) * 0.6f,
                new int[]{pressed ? 0x10FFFFFF : 0x44FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(r.left + 2, r.top + 2 + off, r.right - 2, r.bottom - 2 + off), 13, 13, P);
        P.setShader(null);
        if (enabled) {
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeWidth(1.4f);
            P.setColor(0x88FFFFFF);
            c.drawRoundRect(new RectF(r.left, r.top + off, r.right, r.bottom + off), 14, 14, P);
            P.setStyle(Paint.Style.FILL);
        }
        float size = Math.min(32f, (r.bottom - r.top) * 0.42f);
        bold(c, label, (r.left + r.right) / 2f, (r.top + r.bottom) / 2f + size * 0.36f + off, size, tc, 1);
    }

    public static void coin(Canvas c, float cx, float cy, float r) {
        P.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.5f,
                new int[]{0xFFFFF3B0, 0xFFFFC42E, 0xFFD98A08}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, P);
        P.setShader(null);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(r * 0.18f);
        P.setColor(0x99FFF6C8);
        c.drawCircle(cx, cy, r * 0.68f, P);
        P.setStyle(Paint.Style.FILL);
    }

    /** 金币 + 数量，返回绘制宽度 */
    public static float goldLabel(Canvas c, float x, float y, int amount, float size) {
        coin(c, x + size * 0.42f, y - size * 0.32f, size * 0.42f);
        bold(c, num(amount), x + size * 1.05f, y, size, GOLD, 0);
        TP.setTextSize(size);
        return TP.measureText(num(amount)) + size * 1.1f;
    }

    public static void bar(Canvas c, float l, float t, float w, float h, float pct, int c1, int c2) {
        if (pct < 0) pct = 0;
        if (pct > 1) pct = 1;
        fillRound(c, l, t, l + w, t + h, h / 2, 0xAA000000);
        if (pct > 0.001f) {
            P.setShader(new LinearGradient(l, t, l + w, t, new int[]{c1, c2}, null, Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(l + 1, t + 1, l + Math.max(3, w * pct), t + h - 1), (h - 2) / 2, (h - 2) / 2, P);
            P.setShader(null);
        }
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(1f);
        P.setColor(0x55FFFFFF);
        c.drawRoundRect(new RectF(l, t, l + w, t + h), h / 2, h / 2, P);
        P.setStyle(Paint.Style.FILL);
    }

    public static void diamond(Canvas c, float cx, float cy, float r, int color) {
        Path p = new Path();
        p.moveTo(cx, cy - r);
        p.lineTo(cx + r, cy);
        p.lineTo(cx, cy + r);
        p.lineTo(cx - r, cy);
        p.close();
        P.setColor(color);
        c.drawPath(p, P);
    }

    public static void arrowLeft(Canvas c, float cx, float cy, float r, int color) {
        Path p = new Path();
        p.moveTo(cx + r * 0.5f, cy - r);
        p.lineTo(cx - r * 0.5f, cy);
        p.lineTo(cx + r * 0.5f, cy + r);
        p.close();
        P.setColor(color);
        c.drawPath(p, P);
    }

    /** 全屏渐变背景（用于非战斗界面） */
    public static void bg(Canvas c, float W, float H) {
        P.setShader(new LinearGradient(0, 0, W * 0.4f, H,
                new int[]{0xFF06101F, 0xFF0A1B31, 0xFF050A14},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, P);
        P.setShader(null);
        // 网格地平线
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(1f);
        P.setColor(0x143FD2FF);
        for (int i = 0; i < 9; i++) {
            float y = H * 0.55f + i * i * 9f;
            if (y < H) c.drawLine(0, y, W, y, P);
        }
        P.setStyle(Paint.Style.FILL);
    }

    public static void starfield(Canvas c, float W, float H, float time) {
        for (int i = 0; i < 70; i++) {
            float seed = i * 37.13f;
            float x = (float) ((seed * 137.5) % W);
            float spd = 12f + (i % 5) * 9f;
            float y = ((seed * 71.3f) + time * spd) % (H + 40) - 20;
            int a = 30 + (i % 4) * 40;
            P.setColor((a << 24) | 0xBFD8FF);
            c.drawCircle(x, y, 0.8f + (i % 3) * 0.5f, P);
        }
    }
}
