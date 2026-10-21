package com.zhiwei.skywar;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

/** 共享的深空滚动背景 */
public class Backdrop {

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** scroll 单位为像素（虚拟坐标） */
    public static void draw(Canvas c, float W, float H, float scroll) {
        float tile = Art.nebula.getHeight();
        float y = scroll % tile;
        c.save();
        c.drawBitmap(Art.nebula, 0, y - tile, P);
        c.drawBitmap(Art.nebula, 0, y, P);
        c.drawBitmap(Art.nebula, 0, y + tile, P);
        c.restore();
        // 顶部/底部压暗，突出 UI
        P.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xB3050812, 0x33050812, 0xB3050812},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, P);
        P.setShader(null);
    }

    /** 星点（视差） */
    public static void stars(Canvas c, float W, float H, float time) {
        for (int i = 0; i < 56; i++) {
            float seed = i * 37.13f;
            float x = (float) ((seed * 137.5) % W);
            float spd = 14f + (i % 5) * 10f;
            float y = ((seed * 71.3f) + time * spd) % (H + 40) - 20;
            int a = 30 + (i % 4) * 38;
            P.setColor((a << 24) | 0xBFD8FF);
            c.drawCircle(x, y, 0.9f + (i % 3) * 0.6f, P);
        }
    }
}
